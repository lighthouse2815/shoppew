package com.shoppew.order.service;

import com.shoppew.checkout.entity.CheckoutStatus;
import com.shoppew.checkout.repository.CheckoutGroupRepository;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.service.InventoryReservationService;
import com.shoppew.order.dto.OrderActionRequest;
import com.shoppew.order.dto.AdminOrderDetailResponse;
import com.shoppew.order.dto.AdminOrderSummaryResponse;
import com.shoppew.order.dto.OrderDetailResponse;
import com.shoppew.order.dto.OrderSummaryResponse;
import com.shoppew.order.entity.OrderActorType;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.entity.OrderStatusHistoryEntity;
import com.shoppew.order.event.OrderStatusChangedEvent;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.order.repository.OrderStatusHistoryRepository;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.payment.entity.PaymentStatus;
import com.shoppew.payment.dto.PaymentResponse;
import com.shoppew.payment.repository.PaymentRepository;
import com.shoppew.promotion.service.PromotionPricingService;
import com.shoppew.shipping.entity.ShipmentEntity;
import com.shoppew.shipping.entity.ShipmentStatus;
import com.shoppew.shipping.entity.ShipmentTrackingEntity;
import com.shoppew.shipping.repository.ShipmentRepository;
import com.shoppew.shipping.repository.ShipmentTrackingRepository;
import com.shoppew.shop.service.ShopAccessService;
import com.shoppew.voucher.service.VoucherEngine;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final CheckoutGroupRepository checkoutRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryReservationService reservationService;
    private final ShopAccessService shopAccessService;
    private final PromotionPricingService promotionPricing;
    private final VoucherEngine voucherEngine;
    private final OrderResponseAssembler assembler;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderService(
            OrderRepository orderRepository,
            OrderStatusHistoryRepository historyRepository,
            ShipmentRepository shipmentRepository,
            ShipmentTrackingRepository trackingRepository,
            CheckoutGroupRepository checkoutRepository,
            PaymentRepository paymentRepository,
            InventoryReservationService reservationService,
            ShopAccessService shopAccessService,
            PromotionPricingService promotionPricing,
            VoucherEngine voucherEngine,
            OrderResponseAssembler assembler,
            ApplicationEventPublisher events,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
        this.checkoutRepository = checkoutRepository;
        this.paymentRepository = paymentRepository;
        this.reservationService = reservationService;
        this.shopAccessService = shopAccessService;
        this.promotionPricing = promotionPricing;
        this.voucherEngine = voucherEngine;
        this.assembler = assembler;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> customerOrders(UUID userId, int page, int size) {
        return PageResponse.from(orderRepository.findAllByUser_Id(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), assembler::summary);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse customerDetail(UUID userId, UUID orderId) {
        return assembler.detail(orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(this::orderNotFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> sellerOrders(
            UUID userId, UUID shopId, OrderStatus status, int page, int size) {
        shopAccessService.requireActiveMember(userId, shopId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(status == null
                ? orderRepository.findAllByShop_Id(shopId, pageable)
                : orderRepository.findAllByShop_IdAndStatus(shopId, status, pageable), assembler::summary);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse sellerDetail(UUID userId, UUID shopId, UUID orderId) {
        shopAccessService.requireActiveMember(userId, shopId);
        return assembler.detail(orderRepository.findByIdAndShop_Id(orderId, shopId)
                .orElseThrow(this::orderNotFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummaryResponse> adminOrders(
            String query, OrderStatus status, UUID shopId, UUID userId, int page, int size) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        Specification<OrderEntity> specification = (root, criteria, builder) -> {
            var predicate = builder.conjunction();
            if (!normalized.isEmpty()) {
                String pattern = "%" + escapeLike(normalized) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("orderNumber")), pattern, '\\'),
                        builder.like(builder.lower(root.get("shop").get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("user").get("email")), pattern, '\\')));
            }
            if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            if (shopId != null) predicate = builder.and(predicate, builder.equal(root.get("shop").get("id"), shopId));
            if (userId != null) predicate = builder.and(predicate, builder.equal(root.get("user").get("id"), userId));
            return predicate;
        };
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(orderRepository.findAll(specification, pageable), assembler::adminSummary);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse adminDetail(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(this::orderNotFound);
        PaymentResponse payment = paymentRepository.findByCheckoutGroup_Id(order.getCheckoutGroupId())
                .map(this::paymentResponse)
                .orElse(null);
        return new AdminOrderDetailResponse(
                order.getUserId(), order.getUserEmail(), assembler.detail(order), payment);
    }

    @Transactional
    public OrderDetailResponse sellerConfirm(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        transition(order, Set.of(OrderStatus.PAID), OrderStatus.CONFIRMED, userId,
                OrderActorType.SELLER, reason(request));
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse sellerProcess(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        transition(order, Set.of(OrderStatus.CONFIRMED), OrderStatus.PROCESSING, userId,
                OrderActorType.SELLER, reason(request));
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse sellerReady(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        transition(order, Set.of(OrderStatus.PROCESSING), OrderStatus.READY_TO_SHIP, userId,
                OrderActorType.SELLER, reason(request));
        updateShipment(order, ShipmentStatus.READY, request, "Đơn hàng đã sẵn sàng bàn giao");
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse sellerShip(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        transition(order, Set.of(OrderStatus.READY_TO_SHIP), OrderStatus.SHIPPED, userId,
                OrderActorType.SELLER, reason(request));
        updateShipment(order, ShipmentStatus.IN_TRANSIT, request, "Đơn hàng đang được vận chuyển");
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse sellerDeliver(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        transition(order, Set.of(OrderStatus.SHIPPED), OrderStatus.DELIVERED, userId,
                OrderActorType.SELLER, reason(request));
        updateShipment(order, ShipmentStatus.DELIVERED, request, "Đơn hàng đã được giao");
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse customerComplete(UUID userId, UUID orderId) {
        OrderEntity order = orderRepository.findLocked(orderId).orElseThrow(this::orderNotFound);
        if (!order.getUserId().equals(userId)) throw orderNotFound();
        transition(order, Set.of(OrderStatus.DELIVERED), OrderStatus.COMPLETED, userId,
                OrderActorType.CUSTOMER, "CUSTOMER_CONFIRMED_RECEIPT");
        return assembler.detail(order);
    }

    @Transactional
    public OrderDetailResponse customerCancel(UUID userId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = orderRepository.findLocked(orderId).orElseThrow(this::orderNotFound);
        if (!order.getUserId().equals(userId)) throw orderNotFound();
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            cancelPendingCheckout(order, userId, OrderActorType.CUSTOMER, reason(request));
            return assembler.detail(order);
        }
        if (order.getStatus() == OrderStatus.CONFIRMED && isCod(order)) {
            transition(order, Set.of(OrderStatus.CONFIRMED), OrderStatus.CANCELLED, userId,
                    OrderActorType.CUSTOMER, reason(request));
            reservationService.returnOrderSold(order.getId(), userId);
            promotionPricing.releaseOrder(order.getId());
            voucherEngine.releaseOrder(order.getId());
            return assembler.detail(order);
        }
        throw invalidTransition(order, OrderStatus.CANCELLED);
    }

    @Transactional
    public OrderDetailResponse sellerCancel(UUID userId, UUID shopId, UUID orderId, OrderActionRequest request) {
        OrderEntity order = requireSellerOrder(userId, shopId, orderId);
        if (!isCod(order) || !EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING).contains(order.getStatus())) {
            throw invalidTransition(order, OrderStatus.CANCELLED);
        }
        transition(order, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING), OrderStatus.CANCELLED,
                userId, OrderActorType.SELLER, reason(request));
        reservationService.returnOrderSold(order.getId(), userId);
        promotionPricing.releaseOrder(order.getId());
        voucherEngine.releaseOrder(order.getId());
        return assembler.detail(order);
    }

    @Transactional
    public void paymentSucceeded(UUID checkoutId) {
        for (OrderEntity order : orderRepository.findAllByCheckoutGroup_IdOrderByCreatedAtAsc(checkoutId)) {
            OrderEntity locked = orderRepository.findLocked(order.getId()).orElseThrow(this::orderNotFound);
            if (locked.getStatus() == OrderStatus.PAID) continue;
            transition(locked, Set.of(OrderStatus.PENDING_PAYMENT), OrderStatus.PAID, null,
                    OrderActorType.PAYMENT_PROVIDER, "PAYMENT_SUCCEEDED");
            reservationService.consumeOrder(locked.getId());
        }
    }

    @Transactional
    public void paymentFailed(UUID checkoutId) {
        for (OrderEntity order : orderRepository.findAllByCheckoutGroup_IdOrderByCreatedAtAsc(checkoutId)) {
            OrderEntity locked = orderRepository.findLocked(order.getId()).orElseThrow(this::orderNotFound);
            if (locked.getStatus() != OrderStatus.PENDING_PAYMENT) continue;
            transition(locked, Set.of(OrderStatus.PENDING_PAYMENT), OrderStatus.CANCELLED, null,
                    OrderActorType.PAYMENT_PROVIDER, "PAYMENT_FAILED");
            reservationService.releaseOrder(locked.getId());
        }
    }

    private void cancelPendingCheckout(
            OrderEntity source, UUID actorId, OrderActorType actorType, String reason) {
        var checkout = source.getCheckoutGroup();
        for (OrderEntity current : orderRepository.findAllByCheckoutGroup_IdOrderByCreatedAtAsc(checkout.getId())) {
            OrderEntity locked = current.getId().equals(source.getId())
                    ? source : orderRepository.findLocked(current.getId()).orElseThrow(this::orderNotFound);
            if (locked.getStatus() != OrderStatus.PENDING_PAYMENT) continue;
            transition(locked, Set.of(OrderStatus.PENDING_PAYMENT), OrderStatus.CANCELLED,
                    actorId, actorType, reason);
            reservationService.releaseOrder(locked.getId());
        }
        checkout.changeStatus(CheckoutStatus.CANCELLED, Instant.now(clock));
        paymentRepository.findByCheckoutGroup_Id(checkout.getId()).ifPresent(payment -> payment.cancel(Instant.now(clock)));
        promotionPricing.releaseCheckout(checkout.getId());
        voucherEngine.releaseCheckout(checkout.getId());
    }

    private OrderEntity requireSellerOrder(UUID userId, UUID shopId, UUID orderId) {
        shopAccessService.requireActiveMember(userId, shopId);
        OrderEntity order = orderRepository.findLocked(orderId).orElseThrow(this::orderNotFound);
        if (!order.getShopId().equals(shopId)) throw orderNotFound();
        return order;
    }

    private boolean isCod(OrderEntity order) {
        return paymentRepository.findByCheckoutGroup_Id(order.getCheckoutGroupId())
                .map(payment -> payment.getProvider() == PaymentProviderType.COD).orElse(false);
    }

    private PaymentResponse paymentResponse(com.shoppew.payment.entity.PaymentEntity payment) {
        String action = payment.getProvider() == PaymentProviderType.COD ? "PAY_ON_DELIVERY"
                : payment.getStatus() == PaymentStatus.PENDING ? "MOCK_WEBHOOK_REQUIRED" : null;
        return new PaymentResponse(
                payment.getId(), payment.getCheckoutGroupId(), payment.getProvider().name(),
                payment.getProviderReference(), payment.getStatus().name(), payment.getAmount(),
                payment.getCurrency(), action, payment.getFailureCode(), payment.getFailureMessage(),
                payment.getPaidAt(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private void transition(
            OrderEntity order,
            Set<OrderStatus> allowed,
            OrderStatus next,
            UUID actorId,
            OrderActorType actorType,
            String reason) {
        OrderStatus from = order.getStatus();
        if (!allowed.contains(from)) throw invalidTransition(order, next);
        Instant now = Instant.now(clock);
        order.transition(next, now);
        historyRepository.save(OrderStatusHistoryEntity.create(
                order, from, next, actorId, actorType, reason, now));
        events.publishEvent(new OrderStatusChangedEvent(order.getId(), from, next, actorId, actorType, now));
    }

    private void updateShipment(
            OrderEntity order, ShipmentStatus next, OrderActionRequest request, String defaultDescription) {
        ShipmentEntity shipment = shipmentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new IllegalStateException("Order shipment is missing"));
        Instant now = Instant.now(clock);
        shipment.transition(next, request == null ? null : request.trackingNumber(), now);
        trackingRepository.save(ShipmentTrackingEntity.create(
                shipment, next, reason(request) == null ? defaultDescription : reason(request),
                request == null ? null : request.location(), now));
    }

    private String reason(OrderActionRequest request) {
        return request == null || request.reason() == null || request.reason().isBlank()
                ? null : request.reason().strip();
    }

    private ApiException invalidTransition(OrderEntity order, OrderStatus target) {
        return new ApiException(HttpStatus.CONFLICT, "ORDER_INVALID_STATE",
                "Không thể chuyển đơn hàng từ " + order.getStatus() + " sang " + target);
    }

    private ApiException orderNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng");
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
