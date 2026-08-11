package com.shoppew.payment.service;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.checkout.entity.CheckoutStatus;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.order.service.OrderService;
import com.shoppew.promotion.service.PromotionPricingService;
import com.shoppew.voucher.service.VoucherEngine;
import com.shoppew.payment.dto.MockPaymentWebhookRequest;
import com.shoppew.payment.dto.AdminPaymentResponse;
import com.shoppew.payment.dto.PaymentResponse;
import com.shoppew.payment.entity.PaymentEntity;
import com.shoppew.payment.entity.PaymentEventEntity;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.payment.entity.PaymentStatus;
import com.shoppew.payment.repository.PaymentEventRepository;
import com.shoppew.payment.repository.PaymentRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository eventRepository;
    private final OrderService orderService;
    private final PromotionPricingService promotionPricing;
    private final VoucherEngine voucherEngine;
    private final AppProperties properties;
    private final Clock clock;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventRepository eventRepository,
            OrderService orderService,
            PromotionPricingService promotionPricing,
            VoucherEngine voucherEngine,
            AppProperties properties,
            Clock clock) {
        this.paymentRepository = paymentRepository;
        this.eventRepository = eventRepository;
        this.orderService = orderService;
        this.promotionPricing = promotionPricing;
        this.voucherEngine = voucherEngine;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaymentResponse customerPayment(UUID userId, UUID paymentId) {
        return response(paymentRepository.findByIdAndCheckoutGroup_User_Id(paymentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",
                        "Không tìm thấy giao dịch thanh toán")));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminPaymentResponse> adminPayments(
            String query, PaymentStatus status, PaymentProviderType provider, int page, int size) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        Specification<PaymentEntity> specification = (root, criteria, builder) -> {
            var predicate = builder.conjunction();
            if (!normalized.isEmpty()) {
                String pattern = "%" + escapeLike(normalized) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("providerReference")), pattern, '\\'),
                        builder.like(builder.lower(root.get("checkoutGroup").get("checkoutNumber")), pattern, '\\'),
                        builder.like(builder.lower(root.get("checkoutGroup").get("user").get("email")), pattern, '\\')));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (provider != null) predicate = builder.and(predicate, builder.equal(root.get("provider"), provider));
            return predicate;
        };
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(paymentRepository.findAll(specification, pageable), this::adminResponse);
    }

    @Transactional(readOnly = true)
    public AdminPaymentResponse adminPayment(UUID paymentId) {
        return adminResponse(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",
                        "KhĂ´ng tĂ¬m tháº¥y giao dá»‹ch thanh toĂ¡n")));
    }

    @Transactional
    public PaymentResponse processMockWebhook(String signature, MockPaymentWebhookRequest request) {
        if (!properties.payment().mockEnabled()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_PROVIDER_NOT_AVAILABLE",
                    "PhÆ°Æ¡ng thá»©c thanh toĂ¡n khĂ´ng kháº£ dá»¥ng trong mĂ´i trÆ°á»ng nĂ y");
        }
        verifyMockSignature(signature);
        PaymentEntity payment = paymentRepository.findLockedByProviderReference(request.providerReference())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",
                        "Không tìm thấy giao dịch thanh toán"));
        if (payment.getProvider() != PaymentProviderType.MOCK_ONLINE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PAYMENT_PROVIDER_MISMATCH",
                    "Giao dịch không thuộc nhà cung cấp mô phỏng");
        }
        String payloadHash = sha256(request.providerEventId() + "|" + request.providerReference()
                + "|" + request.succeeded());
        PaymentEventEntity existing = eventRepository
                .findByProviderAndProviderEventId(PaymentProviderType.MOCK_ONLINE.name(), request.providerEventId())
                .orElse(null);
        if (existing != null) {
            if (!existing.getPayloadHash().equals(payloadHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_EVENT_PAYLOAD_MISMATCH",
                        "Mã sự kiện thanh toán đã được dùng với payload khác");
            }
            return response(existing.getPayment());
        }

        Instant now = Instant.now(clock);
        PaymentEventEntity event = eventRepository.save(PaymentEventEntity.receive(
                payment, PaymentProviderType.MOCK_ONLINE.name(), request.providerEventId(),
                request.succeeded() ? "PAYMENT_SUCCEEDED" : "PAYMENT_FAILED", payloadHash, now));
        CheckoutGroupEntity checkout = payment.getCheckoutGroup();
        if (request.succeeded()) {
            if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                payment.succeed(now);
                checkout.changeStatus(CheckoutStatus.CONFIRMED, now);
                orderService.paymentSucceeded(checkout.getId());
                promotionPricing.consumeCheckout(checkout.getId());
                voucherEngine.consumeCheckout(checkout.getId());
            }
        } else if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.fail("MOCK_PAYMENT_DECLINED", "Thanh toán mô phỏng đã bị từ chối", now);
            checkout.changeStatus(CheckoutStatus.FAILED, now);
            orderService.paymentFailed(checkout.getId());
            promotionPricing.releaseCheckout(checkout.getId());
            voucherEngine.releaseCheckout(checkout.getId());
        }
        event.processed(now);
        return response(payment);
    }

    public PaymentResponse response(PaymentEntity payment) {
        String action = payment.getProvider() == PaymentProviderType.COD ? "PAY_ON_DELIVERY"
                : payment.getStatus() == PaymentStatus.PENDING ? "MOCK_WEBHOOK_REQUIRED" : null;
        return new PaymentResponse(
                payment.getId(), payment.getCheckoutGroupId(), payment.getProvider().name(),
                payment.getProviderReference(), payment.getStatus().name(), payment.getAmount(),
                payment.getCurrency(), action, payment.getFailureCode(), payment.getFailureMessage(),
                payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private AdminPaymentResponse adminResponse(PaymentEntity payment) {
        CheckoutGroupEntity checkout = payment.getCheckoutGroup();
        return new AdminPaymentResponse(
                payment.getId(), payment.getCheckoutGroupId(), checkout.getCheckoutNumber(),
                checkout.getUserId(), checkout.getUserEmail(), payment.getProvider().name(),
                payment.getProviderReference(), payment.getStatus().name(), payment.getAmount(),
                payment.getCurrency(), payment.getFailureCode(), payment.getFailureMessage(),
                payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private void verifyMockSignature(String signature) {
        byte[] expected = properties.payment().mockWebhookSecret().getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature == null ? new byte[0] : signature.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_PAYMENT_SIGNATURE",
                    "Chữ ký callback thanh toán không hợp lệ");
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
