package com.shoppew.refund.service;

import com.shoppew.audit.service.AdminAuditService;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.finance.service.SellerFinanceService;
import com.shoppew.order.entity.OrderActorType;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.entity.OrderStatusHistoryEntity;
import com.shoppew.order.event.OrderStatusChangedEvent;
import com.shoppew.order.repository.OrderItemRepository;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.order.repository.OrderStatusHistoryRepository;
import com.shoppew.payment.entity.PaymentEntity;
import com.shoppew.payment.repository.PaymentRepository;
import com.shoppew.refund.dto.RefundCreateRequest;
import com.shoppew.refund.dto.RefundDecisionRequest;
import com.shoppew.refund.dto.RefundResponse;
import com.shoppew.refund.entity.RefundEntity;
import com.shoppew.refund.entity.RefundRequestEntity;
import com.shoppew.refund.entity.RefundRequestItemEntity;
import com.shoppew.refund.entity.RefundRequestStatus;
import com.shoppew.refund.entity.RefundStatus;
import com.shoppew.refund.repository.RefundRepository;
import com.shoppew.refund.repository.RefundRequestItemRepository;
import com.shoppew.refund.repository.RefundRequestRepository;
import com.shoppew.shop.repository.ShopSettingsRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {
    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private final RefundRequestRepository requests; private final RefundRequestItemRepository requestItems;
    private final RefundRepository refunds; private final OrderRepository orders; private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository histories; private final PaymentRepository payments;
    private final UserRepository users; private final ShopSettingsRepository settings; private final ShopAccessService access;
    private final SellerFinanceService finance; private final AdminAuditService audit;
    private final ApplicationEventPublisher events; private final Clock clock;

    public RefundService(RefundRequestRepository requests, RefundRequestItemRepository requestItems,
            RefundRepository refunds, OrderRepository orders, OrderItemRepository orderItems,
            OrderStatusHistoryRepository histories, PaymentRepository payments, UserRepository users,
            ShopSettingsRepository settings, ShopAccessService access, SellerFinanceService finance,
            AdminAuditService audit, ApplicationEventPublisher events, Clock clock) {
        this.requests = requests; this.requestItems = requestItems; this.refunds = refunds; this.orders = orders;
        this.orderItems = orderItems; this.histories = histories; this.payments = payments; this.users = users;
        this.settings = settings; this.access = access; this.finance = finance; this.audit = audit;
        this.events = events; this.clock = clock;
    }

    @Transactional
    public RefundResponse create(UUID userId, RefundCreateRequest input) {
        Instant now = Instant.now(clock);
        OrderEntity order = orders.findLocked(input.orderId()).orElseThrow(this::notFound);
        if (!order.getUserId().equals(userId)) throw notFound();
        if (!Set.of(OrderStatus.COMPLETED, OrderStatus.PARTIALLY_REFUNDED).contains(order.getStatus())) {
            throw conflict("REFUND_ORDER_NOT_ELIGIBLE", "Order must be completed before requesting a refund");
        }
        int returnDays = settings.findById(order.getShopId()).map(value -> value.getReturnWindowDays()).orElse(7);
        if (order.getCompletedAt() == null || now.isAfter(order.getCompletedAt().plusSeconds(returnDays * 86400L))) {
            throw conflict("REFUND_WINDOW_EXPIRED", "The shop return window has expired");
        }
        if (input.items().stream().map(RefundCreateRequest.Item::orderItemId).distinct().count() != input.items().size()) {
            throw conflict("REFUND_DUPLICATE_ITEM", "Each order item may appear only once in a refund request");
        }

        BigDecimal customerMerchandise = order.getItemsSubtotal().subtract(order.getShopDiscountTotal())
                .subtract(order.getPlatformDiscountTotal());
        BigDecimal sellerMerchandise = order.getItemsSubtotal().subtract(order.getShopDiscountTotal());
        List<ItemQuote> quotes = input.items().stream().map(item -> quote(order, item, customerMerchandise, sellerMerchandise)).toList();
        BigDecimal requested = quotes.stream().map(ItemQuote::customerAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        RefundRequestEntity request = requests.saveAndFlush(RefundRequestEntity.create(number("RFR"), order,
                users.findById(userId).orElseThrow(this::notFound), input.reason(), normalize(input.description()), requested, now));
        requestItems.saveAll(quotes.stream().map(quote -> RefundRequestItemEntity.create(request, quote.item(),
                quote.quantity(), quote.customerAmount(), quote.sellerAmount())).toList());
        changeOrder(order, OrderStatus.REFUND_REQUESTED, userId, OrderActorType.CUSTOMER, "REFUND_REQUESTED", now);
        return response(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> customerList(UUID userId, int page, int size) {
        return PageResponse.from(requests.findAllByUser_Id(userId, page(page, size)), this::response);
    }
    @Transactional(readOnly = true)
    public RefundResponse customerDetail(UUID userId, UUID requestId) {
        return response(requests.findByIdAndUser_Id(requestId, userId).orElseThrow(this::notFound));
    }
    @Transactional
    public RefundResponse cancel(UUID userId, UUID requestId) {
        RefundRequestEntity request = requests.findLocked(requestId).orElseThrow(this::notFound);
        if (!request.getUserId().equals(userId)) throw notFound();
        if (request.getStatus() != RefundRequestStatus.REQUESTED) throw invalidState(request);
        request.cancel(Instant.now(clock));
        restoreOrder(request, userId, OrderActorType.CUSTOMER, "REFUND_CANCELLED");
        return response(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> sellerList(UUID userId, UUID shopId, RefundRequestStatus status, int page, int size) {
        access.requireActiveMember(userId, shopId);
        return PageResponse.from(status == null ? requests.findAllByShop_Id(shopId, page(page, size))
                : requests.findAllByShop_IdAndStatus(shopId, status, page(page, size)), this::response);
    }
    @Transactional
    public RefundResponse sellerReview(UUID userId, UUID shopId, UUID requestId, String note) {
        access.requireActiveMember(userId, shopId);
        RefundRequestEntity request = requests.findLocked(requestId).orElseThrow(this::notFound);
        if (!request.getShopId().equals(shopId)) throw notFound();
        if (request.getStatus() != RefundRequestStatus.REQUESTED) throw invalidState(request);
        request.review(users.findById(userId).orElseThrow(this::notFound), normalize(note), Instant.now(clock));
        return response(request);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> adminList(RefundRequestStatus status, int page, int size) {
        return PageResponse.from(status == null ? requests.findAll(page(page, size))
                : requests.findAllByStatus(status, page(page, size)), this::response);
    }
    @Transactional
    public RefundResponse approve(UUID actorId, UUID requestId, RefundDecisionRequest input) {
        RefundRequestEntity request = requests.findLocked(requestId).orElseThrow(this::notFound);
        if (!Set.of(RefundRequestStatus.REQUESTED, RefundRequestStatus.UNDER_REVIEW).contains(request.getStatus())) throw invalidState(request);
        if (input.approvedAmount().compareTo(request.getRequestedAmount()) > 0) {
            throw conflict("REFUND_AMOUNT_EXCEEDS_REQUEST", "Approved amount cannot exceed the server-calculated requested amount");
        }
        RefundRequestStatus before = request.getStatus();
        request.approve(users.findById(actorId).orElseThrow(this::notFound), input.approvedAmount(), normalize(input.note()), Instant.now(clock));
        audit.record(actorId, "REFUND_APPROVED", "REFUND_REQUEST", requestId,
                Map.of("status", before), Map.of("status", request.getStatus(), "approvedAmount", request.getApprovedAmount()));
        return response(request);
    }
    @Transactional
    public RefundResponse reject(UUID actorId, UUID requestId, String note) {
        RefundRequestEntity request = requests.findLocked(requestId).orElseThrow(this::notFound);
        if (!Set.of(RefundRequestStatus.REQUESTED, RefundRequestStatus.UNDER_REVIEW).contains(request.getStatus())) throw invalidState(request);
        RefundRequestStatus before = request.getStatus();
        request.reject(users.findById(actorId).orElseThrow(this::notFound), normalize(note), Instant.now(clock));
        restoreOrder(request, actorId, OrderActorType.ADMIN, "REFUND_REJECTED");
        audit.record(actorId, "REFUND_REJECTED", "REFUND_REQUEST", requestId,
                Map.of("status", before), Map.of("status", request.getStatus()));
        return response(request);
    }

    @Transactional
    public RefundResponse process(UUID actorId, UUID requestId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "A valid Idempotency-Key header is required");
        }
        var replay = refunds.findByIdempotencyKey(idempotencyKey);
        if (replay.isPresent()) {
            if (!replay.get().getRefundRequestId().equals(requestId)) throw conflict("IDEMPOTENCY_KEY_REUSED", "Idempotency key belongs to another refund");
            return response(requests.findById(requestId).orElseThrow(this::notFound));
        }
        RefundRequestEntity request = requests.findLocked(requestId).orElseThrow(this::notFound);
        if (request.getStatus() == RefundRequestStatus.REFUNDED) return response(request);
        if (request.getStatus() != RefundRequestStatus.APPROVED) throw invalidState(request);
        Instant now = Instant.now(clock);
        PaymentEntity payment = payments.findByCheckoutGroup_Id(request.getOrder().getCheckoutGroupId()).orElseThrow();
        BigDecimal sellerTotal = requestItems.sellerChargeTotal(requestId);
        BigDecimal sellerCharge = sellerTotal.multiply(request.getApprovedAmount())
                .divide(request.getRequestedAmount(), 2, RoundingMode.HALF_UP);
        request.refunding(now);
        RefundEntity refund = refunds.saveAndFlush(RefundEntity.processing(request, payment, request.getApprovedAmount(),
                sellerCharge, idempotencyKey, now));
        refund.succeed("LOCAL-REFUND-" + refund.getId(), now);
        request.refunded(now);
        refunds.flush();
        BigDecimal refundedTotal = refunds.sumForOrder(request.getOrderId(), RefundStatus.SUCCEEDED);
        BigDecimal merchandisePaid = request.getOrder().getItemsSubtotal().subtract(request.getOrder().getShopDiscountTotal())
                .subtract(request.getOrder().getPlatformDiscountTotal());
        OrderStatus target = refundedTotal.compareTo(merchandisePaid) >= 0 ? OrderStatus.REFUNDED : OrderStatus.PARTIALLY_REFUNDED;
        changeOrder(request.getOrder(), target, actorId, OrderActorType.ADMIN, "REFUND_SUCCEEDED", now);
        payment.recordRefund(refundedTotal, now);
        finance.recordRefund(request.getOrder(), refund);
        audit.record(actorId, "REFUND_PROCESSED", "REFUND", refund.getId(), null,
                Map.of("requestId", requestId, "amount", refund.getAmount(), "sellerCharge", refund.getSellerChargeAmount(), "status", refund.getStatus()));
        return response(request);
    }

    private ItemQuote quote(OrderEntity order, RefundCreateRequest.Item input,
            BigDecimal customerMerchandise, BigDecimal sellerMerchandise) {
        OrderItemEntity item = orderItems.findById(input.orderItemId()).orElseThrow(this::notFound);
        if (!item.getOrderId().equals(order.getId())) throw notFound();
        long available = item.getQuantity() - requestItems.allocatedQuantity(item.getId());
        if (input.quantity() > available) throw conflict("REFUND_QUANTITY_EXCEEDED", "Refund quantity exceeds the remaining purchased quantity");
        BigDecimal base = item.getUnitPrice().multiply(BigDecimal.valueOf(input.quantity()));
        BigDecimal customer = prorate(base, customerMerchandise, order.getItemsSubtotal());
        BigDecimal seller = prorate(base, sellerMerchandise, order.getItemsSubtotal());
        return new ItemQuote(item, input.quantity(), customer, seller);
    }
    private BigDecimal prorate(BigDecimal base, BigDecimal total, BigDecimal denominator) {
        return denominator.signum() == 0 ? BigDecimal.ZERO.setScale(2) : base.multiply(total).divide(denominator, 2, RoundingMode.HALF_UP);
    }
    private void restoreOrder(RefundRequestEntity request, UUID actorId, OrderActorType actorType, String reason) {
        changeOrder(request.getOrder(), request.getPreviousOrderStatus(), actorId, actorType, reason, Instant.now(clock));
    }
    private void changeOrder(OrderEntity order, OrderStatus target, UUID actorId, OrderActorType actorType, String reason, Instant now) {
        OrderStatus from = order.getStatus(); order.transition(target, now);
        histories.save(OrderStatusHistoryEntity.create(order, from, target, actorId, actorType, reason, now));
        events.publishEvent(new OrderStatusChangedEvent(order.getId(), from, target, actorId, actorType, now));
    }
    private RefundResponse response(RefundRequestEntity request) {
        List<RefundResponse.Item> items = requestItems.findAllByRefundRequest_IdOrderByOrderItem_Id(request.getId()).stream()
                .map(item -> new RefundResponse.Item(item.getOrderItemId(), item.getProductName(), item.getVariantName(), item.getQuantity(), item.getRequestedAmount())).toList();
        RefundResponse.Refund refund = refunds.findByRefundRequest_Id(request.getId()).map(value -> new RefundResponse.Refund(
                value.getId(), value.getPaymentId(), value.getProviderReference(), value.getAmount(), value.getStatus(), value.getCompletedAt())).orElse(null);
        return new RefundResponse(request.getId(), request.getRequestNumber(), request.getOrderId(), request.getOrder().getOrderNumber(),
                request.getUserId(), request.getShopId(), request.getReason(), request.getDescription(), request.getStatus(),
                request.getRequestedAmount(), request.getApprovedAmount(), request.getCurrency(), request.getReviewedById(),
                request.getReviewNote(), request.getPreviousOrderStatus(), items, refund, request.getCreatedAt(), request.getUpdatedAt());
    }
    private PageRequest page(int page, int size) { return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")); }
    private String number(String prefix) { return prefix + "-" + NUMBER_TIME.format(Instant.now(clock)) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    private ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "REFUND_NOT_FOUND", "Refund request was not found"); }
    private ApiException invalidState(RefundRequestEntity request) { return conflict("REFUND_INVALID_STATE", "Refund request cannot transition from " + request.getStatus()); }
    private ApiException conflict(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }
    private record ItemQuote(OrderItemEntity item, long quantity, BigDecimal customerAmount, BigDecimal sellerAmount) {}
}
