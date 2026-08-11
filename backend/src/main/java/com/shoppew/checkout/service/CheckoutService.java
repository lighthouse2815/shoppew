package com.shoppew.checkout.service;

import com.shoppew.address.entity.UserAddressEntity;
import com.shoppew.address.repository.UserAddressRepository;
import com.shoppew.cart.entity.CartEntity;
import com.shoppew.cart.entity.CartItemEntity;
import com.shoppew.cart.repository.CartItemRepository;
import com.shoppew.cart.repository.CartRepository;
import com.shoppew.checkout.dto.CheckoutPreviewResponse;
import com.shoppew.checkout.dto.CheckoutRequest;
import com.shoppew.checkout.dto.CheckoutResponse;
import com.shoppew.checkout.dto.CheckoutShopQuoteResponse;
import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.checkout.entity.CheckoutStatus;
import com.shoppew.checkout.event.CheckoutPlacedEvent;
import com.shoppew.checkout.repository.CheckoutGroupRepository;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.entity.InventoryEntity;
import com.shoppew.inventory.repository.InventoryRepository;
import com.shoppew.inventory.service.InventoryReservationService;
import com.shoppew.order.dto.OrderSummaryResponse;
import com.shoppew.order.entity.OrderActorType;
import com.shoppew.order.entity.OrderAddressEntity;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.entity.OrderStatusHistoryEntity;
import com.shoppew.order.repository.OrderAddressRepository;
import com.shoppew.order.repository.OrderItemRepository;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.order.repository.OrderStatusHistoryRepository;
import com.shoppew.order.service.OrderResponseAssembler;
import com.shoppew.payment.dto.PaymentResponse;
import com.shoppew.payment.entity.PaymentEntity;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.payment.repository.PaymentRepository;
import com.shoppew.payment.service.PaymentProvider;
import com.shoppew.payment.service.PaymentProviderRegistry;
import com.shoppew.product.entity.ProductImageEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductImageRepository;
import com.shoppew.promotion.service.PromotionPricingService;
import com.shoppew.shipping.entity.ShipmentEntity;
import com.shoppew.shipping.entity.ShipmentStatus;
import com.shoppew.shipping.entity.ShipmentTrackingEntity;
import com.shoppew.shipping.entity.ShippingMethodEntity;
import com.shoppew.shipping.repository.ShipmentRepository;
import com.shoppew.shipping.repository.ShipmentTrackingRepository;
import com.shoppew.shipping.repository.ShippingMethodRepository;
import com.shoppew.shipping.service.ShippingProvider;
import com.shoppew.shipping.service.ShippingProviderRegistry;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import com.shoppew.voucher.service.VoucherEngine;
import com.shoppew.voucher.service.VoucherEngine.VoucherQuote;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository imageRepository;
    private final CheckoutGroupRepository checkoutRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderAddressRepository orderAddressRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final ShippingMethodRepository shippingMethodRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingRepository trackingRepository;
    private final InventoryReservationService reservationService;
    private final PaymentProviderRegistry paymentProviders;
    private final ShippingProviderRegistry shippingProviders;
    private final OrderResponseAssembler orderAssembler;
    private final PromotionPricingService promotionPricing;
    private final VoucherEngine voucherEngine;
    private final AppProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CheckoutService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            UserAddressRepository addressRepository,
            InventoryRepository inventoryRepository,
            ProductImageRepository imageRepository,
            CheckoutGroupRepository checkoutRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderAddressRepository orderAddressRepository,
            OrderStatusHistoryRepository historyRepository,
            PaymentRepository paymentRepository,
            ShippingMethodRepository shippingMethodRepository,
            ShipmentRepository shipmentRepository,
            ShipmentTrackingRepository trackingRepository,
            InventoryReservationService reservationService,
            PaymentProviderRegistry paymentProviders,
            ShippingProviderRegistry shippingProviders,
            OrderResponseAssembler orderAssembler,
            PromotionPricingService promotionPricing,
            VoucherEngine voucherEngine,
            AppProperties properties,
            ApplicationEventPublisher events,
            Clock clock) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
        this.imageRepository = imageRepository;
        this.checkoutRepository = checkoutRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderAddressRepository = orderAddressRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.shippingMethodRepository = shippingMethodRepository;
        this.shipmentRepository = shipmentRepository;
        this.trackingRepository = trackingRepository;
        this.reservationService = reservationService;
        this.paymentProviders = paymentProviders;
        this.shippingProviders = shippingProviders;
        this.orderAssembler = orderAssembler;
        this.promotionPricing = promotionPricing;
        this.voucherEngine = voucherEngine;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(UUID userId, CheckoutRequest request) {
        CheckoutPlan plan = plan(userId, request);
        return new CheckoutPreviewResponse(
                plan.shops().stream().map(ShopPlan::response).toList(), plan.itemsSubtotal(),
                plan.shippingTotal(), plan.vouchers().totalDiscount(), plan.grandTotal(), properties.currency(),
                request.paymentProvider().name(), plan.shippingMethodCode(), plan.vouchers().responses(), Instant.now(clock));
    }

    @Transactional
    public CheckoutResponse place(UUID userId, String idempotencyKey, CheckoutRequest request) {
        String normalizedKey = validateIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(request);
        CheckoutGroupEntity replay = checkoutRepository
                .findByUser_IdAndIdempotencyKey(userId, normalizedKey).orElse(null);
        if (replay != null) {
            if (!replay.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key đã được dùng cho nội dung checkout khác");
            }
            return response(replay);
        }

        CheckoutPlan plan = plan(userId, request);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Không tìm thấy người dùng"));
        Instant now = Instant.now(clock);
        boolean cod = request.paymentProvider() == PaymentProviderType.COD;
        CheckoutGroupEntity checkout = checkoutRepository.save(CheckoutGroupEntity.create(
                nextNumber("CHK"), user, properties.currency(), plan.itemsSubtotal(), plan.shippingTotal(),
                plan.vouchers().totalDiscount(), normalizedKey, requestHash,
                cod ? CheckoutStatus.CONFIRMED : CheckoutStatus.PAYMENT_PENDING, now));
        ShippingMethodEntity shippingMethod = shippingMethodRepository
                .findByProviderAndCodeAndActiveTrue(plan.shippingProvider(), plan.shippingMethodCode())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "SHIPPING_METHOD_NOT_AVAILABLE",
                        "Phương thức vận chuyển không khả dụng"));

        List<OrderEntity> orders = new ArrayList<>();
        Map<UUID, OrderEntity> ordersByShop = new HashMap<>();
        for (ShopPlan shopPlan : plan.shops()) {
            OrderStatus initialStatus = cod ? OrderStatus.CONFIRMED : OrderStatus.PENDING_PAYMENT;
            OrderEntity order = orderRepository.save(OrderEntity.create(
                    nextNumber("ORD"), checkout, user, shopPlan.items().getFirst().getShop(), initialStatus,
                    properties.currency(), shopPlan.itemsSubtotal(), shopPlan.shippingFee(),
                    shopPlan.shopDiscount(), shopPlan.platformDiscount(),
                    normalizeNote(request.customerNote()), now));
            orders.add(order);
            ordersByShop.put(order.getShopId(), order);
            orderAddressRepository.save(OrderAddressEntity.snapshot(order, plan.address()));
            for (PricedItem pricedItem : shopPlan.items()) {
                CartItemEntity cartItem = pricedItem.item();
                OrderItemEntity orderItem = orderItemRepository.save(OrderItemEntity.snapshot(
                        order, cartItem.getProduct(), cartItem.getVariant(), cartItem.getProduct().getName(),
                        cartItem.getVariant().getName(), cartItem.getVariant().getSku(), imageUrl(cartItem, plan.images()),
                        pricedItem.unitPrice(), cartItem.getQuantity(), cartItem.getVariant().getCurrency(), now));
                reservationService.reserve(userId, cartItem.getVariant().getId(), cartItem.getQuantity(), order.getId());
                promotionPricing.reserve(pricedItem.promotionProductId(), checkout, order, orderItem, cartItem.getQuantity());
            }
            ShipmentEntity shipment = shipmentRepository.save(ShipmentEntity.create(
                    order, shippingMethod, shopPlan.shippingFee(), properties.currency(),
                    shopPlan.estimatedFrom(), shopPlan.estimatedTo(), now));
            trackingRepository.save(ShipmentTrackingEntity.create(
                    shipment, ShipmentStatus.PENDING, "Đơn vị vận chuyển đã nhận thông tin đơn hàng", null, now));
            historyRepository.save(OrderStatusHistoryEntity.create(
                    order, null, initialStatus, userId, OrderActorType.CUSTOMER, "ORDER_PLACED", now));
        }

        voucherEngine.reserve(plan.vouchers(), user, checkout, ordersByShop, properties.currency());

        PaymentProvider provider = paymentProviders.require(request.paymentProvider());
        PaymentProvider.PaymentInitiation initiation = provider.initiate(
                checkout.getCheckoutNumber(), checkout.getGrandTotal(), checkout.getCurrency());
        paymentRepository.save(PaymentEntity.create(
                checkout, request.paymentProvider(), initiation.providerReference(), checkout.getGrandTotal(),
                checkout.getCurrency(), "PAY-" + checkout.getId(), now));

        if (cod) {
            orders.forEach(order -> reservationService.consumeOrder(order.getId()));
            promotionPricing.consumeCheckout(checkout.getId());
            voucherEngine.consumeCheckout(checkout.getId());
        }
        cartItemRepository.deleteAll(plan.items());
        plan.cart().touch(now);
        events.publishEvent(new CheckoutPlacedEvent(checkout.getId()));
        return response(checkout);
    }

    private CheckoutPlan plan(UUID userId, CheckoutRequest request) {
        paymentProviders.require(request.paymentProvider());
        String shippingMethodCode = request.shippingMethodCode() == null || request.shippingMethodCode().isBlank()
                ? "MOCK_STANDARD" : request.shippingMethodCode().strip();
        ShippingProvider shippingProvider = shippingProviders.require(shippingMethodCode);
        CartEntity cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CART_EMPTY", "Giỏ hàng đang trống"));
        List<CartItemEntity> allItems = cartItemRepository.findAllDetailedByCartId(cart.getId());
        Set<UUID> requestedIds = Set.copyOf(request.cartItemIds());
        List<CartItemEntity> items = allItems.stream().filter(item -> requestedIds.contains(item.getId())).toList();
        if (items.size() != requestedIds.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                    "Không tìm thấy một hoặc nhiều sản phẩm checkout trong giỏ hàng của bạn");
        }
        UserAddressEntity address = addressRepository.findByIdAndUserId(request.addressId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND",
                        "Không tìm thấy địa chỉ giao hàng"));
        List<UUID> variantIds = items.stream().map(item -> item.getVariant().getId()).toList();
        Map<UUID, InventoryEntity> inventory = inventoryRepository.findAllDetailedByVariantIdIn(variantIds)
                .stream().collect(Collectors.toMap(InventoryEntity::getVariantId, value -> value));
        validateItems(items, inventory);
        List<UUID> productIds = items.stream().map(item -> item.getProduct().getId()).distinct().toList();
        Map<UUID, List<ProductImageEntity>> images = imageRepository
                .findAllByProduct_IdInOrderBySortOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(ProductImageEntity::getProductId));

        Map<UUID, List<CartItemEntity>> byShop = items.stream().collect(Collectors.groupingBy(
                item -> item.getShop().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<UUID, PromotionPricingService.PriceDecision> promotionPrices = promotionPricing.prices(items);
        List<ShopPlan> shops = new ArrayList<>();
        for (List<CartItemEntity> shopItems : byShop.values()) {
            List<PricedItem> pricedItems = shopItems.stream().map(item -> {
                PromotionPricingService.PriceDecision decision = promotionPrices.get(item.getId());
                return new PricedItem(item, decision.unitPrice(), decision.promotionProductId());
            }).toList();
            BigDecimal subtotal = pricedItems.stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(moneyZero(), BigDecimal::add);
            long weight = shopItems.stream().mapToLong(item ->
                    (long) (item.getVariant().getWeightGrams() == null ? 500 : item.getVariant().getWeightGrams())
                            * item.getQuantity()).sum();
            ShippingProvider.ShippingQuote quote = shippingProvider.quote(new ShippingProvider.ShippingQuoteRequest(
                    shopItems.getFirst().getShop().getId(), weight, subtotal, address.getProvince()));
            shops.add(new ShopPlan(
                    List.copyOf(pricedItems), subtotal, quote.fee(), moneyZero(), moneyZero(),
                    quote.estimatedFrom(), quote.estimatedTo()));
        }
        shops.sort(Comparator.comparing(plan -> plan.items().getFirst().getShop().getName()));
        BigDecimal itemsSubtotal = shops.stream().map(ShopPlan::itemsSubtotal)
                .reduce(moneyZero(), BigDecimal::add);
        BigDecimal shippingTotal = shops.stream().map(ShopPlan::shippingFee)
                .reduce(moneyZero(), BigDecimal::add);
        List<VoucherEngine.ShopInput> voucherShops = shops.stream().map(shop -> new VoucherEngine.ShopInput(
                shop.items().getFirst().getShop().getId(), shop.items().stream().map(item -> new VoucherEngine.LineInput(
                        item.getProduct().getId(), item.getProduct().getCategory().getId(),
                        item.unitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))).toList(),
                shop.itemsSubtotal(), shop.shippingFee())).toList();
        VoucherQuote vouchers = voucherEngine.quote(userId, request.voucherCodes(), request.paymentProvider(),
                properties.currency(), voucherShops);
        List<ShopPlan> discountedShops = shops.stream().map(shop -> {
            UUID shopId = shop.items().getFirst().getShop().getId();
            return new ShopPlan(shop.items(), shop.itemsSubtotal(), shop.shippingFee(),
                    vouchers.shopOwnerDiscounts().getOrDefault(shopId, moneyZero()),
                    vouchers.platformDiscounts().getOrDefault(shopId, moneyZero()),
                    shop.estimatedFrom(), shop.estimatedTo());
        }).toList();
        return new CheckoutPlan(cart, items, address, images, List.copyOf(discountedShops), itemsSubtotal, shippingTotal,
                itemsSubtotal.add(shippingTotal).subtract(vouchers.totalDiscount()),
                shippingProvider.provider(), shippingMethodCode, vouchers);
    }

    private void validateItems(List<CartItemEntity> items, Map<UUID, InventoryEntity> inventories) {
        for (CartItemEntity item : items) {
            if (item.getShop().getStatus() != ShopStatus.ACTIVE
                    || item.getProduct().getStatus() != ProductStatus.ACTIVE
                    || item.getVariant().getStatus() != VariantStatus.ACTIVE) {
                throw new ApiException(HttpStatus.CONFLICT, "CHECKOUT_ITEM_NOT_ELIGIBLE",
                        "Một sản phẩm đã không còn đủ điều kiện checkout");
            }
            if (!properties.currency().equals(item.getVariant().getCurrency())) {
                throw new ApiException(HttpStatus.CONFLICT, "CHECKOUT_CURRENCY_MISMATCH",
                        "Đơn hàng chứa tiền tệ không được hỗ trợ");
            }
            InventoryEntity inventory = inventories.get(item.getVariant().getId());
            if (inventory == null || inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                        "Một sản phẩm không còn đủ tồn kho để checkout");
            }
        }
    }

    private CheckoutResponse response(CheckoutGroupEntity checkout) {
        List<OrderSummaryResponse> orders = orderRepository
                .findAllByCheckoutGroup_IdOrderByCreatedAtAsc(checkout.getId()).stream()
                .map(orderAssembler::summary).toList();
        PaymentEntity payment = paymentRepository.findByCheckoutGroup_Id(checkout.getId())
                .orElseThrow(() -> new IllegalStateException("Checkout payment is missing"));
        return new CheckoutResponse(
                checkout.getId(), checkout.getCheckoutNumber(), checkout.getStatus().name(),
                checkout.getItemsSubtotal(), checkout.getShippingTotal(), checkout.getDiscountTotal(),
                checkout.getGrandTotal(), checkout.getCurrency(), orders, paymentResponse(payment),
                voucherEngine.responses(checkout.getId()), checkout.getCreatedAt());
    }

    private PaymentResponse paymentResponse(PaymentEntity payment) {
        String action = payment.getProvider() == PaymentProviderType.COD ? "PAY_ON_DELIVERY"
                : payment.getStatus().name().equals("PENDING") ? "MOCK_WEBHOOK_REQUIRED" : null;
        return new PaymentResponse(
                payment.getId(), payment.getCheckoutGroupId(), payment.getProvider().name(),
                payment.getProviderReference(), payment.getStatus().name(), payment.getAmount(), payment.getCurrency(),
                action, payment.getFailureCode(), payment.getFailureMessage(), payment.getPaidAt(),
                payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private String imageUrl(CartItemEntity item, Map<UUID, List<ProductImageEntity>> images) {
        if (item.getVariant().getImageUrl() != null) return item.getVariant().getImageUrl();
        List<ProductImageEntity> productImages = images.getOrDefault(item.getProduct().getId(), List.of());
        if (productImages.isEmpty()) return null;
        return productImages.stream().filter(ProductImageEntity::isPrimary).findFirst()
                .orElse(productImages.getFirst()).getUrl();
    }

    private String validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
                    "Checkout yêu cầu Idempotency-Key từ 1 đến 128 ký tự");
        }
        return key.strip();
    }

    private String requestHash(CheckoutRequest request) {
        String canonical = request.cartItemIds().stream().map(UUID::toString).sorted().collect(Collectors.joining(","))
                + "|" + request.addressId() + "|" + request.paymentProvider() + "|"
                + (request.shippingMethodCode() == null ? "MOCK_STANDARD" : request.shippingMethodCode().strip())
                + "|" + normalizeNote(request.customerNote()) + "|"
                + (request.voucherCodes() == null ? "" : request.voucherCodes().stream()
                        .map(value -> value.strip().toUpperCase(java.util.Locale.ROOT)).sorted()
                        .collect(Collectors.joining(",")));
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String normalizeNote(String note) { return note == null || note.isBlank() ? "" : note.strip(); }

    private String nextNumber(String prefix) {
        String time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now(clock));
        return prefix + "-" + time + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal moneyZero() { return BigDecimal.ZERO.setScale(2); }

    private record CheckoutPlan(
            CartEntity cart,
            List<CartItemEntity> items,
            UserAddressEntity address,
            Map<UUID, List<ProductImageEntity>> images,
            List<ShopPlan> shops,
            BigDecimal itemsSubtotal,
            BigDecimal shippingTotal,
            BigDecimal grandTotal,
            String shippingProvider,
            String shippingMethodCode,
            VoucherQuote vouchers) {}

    private record PricedItem(
            CartItemEntity item,
            BigDecimal unitPrice,
            UUID promotionProductId) {
        ProductEntity getProduct() { return item.getProduct(); }
        com.shoppew.product.entity.ProductVariantEntity getVariant() { return item.getVariant(); }
        com.shoppew.shop.entity.ShopEntity getShop() { return item.getShop(); }
        long getQuantity() { return item.getQuantity(); }
    }

    private record ShopPlan(
            List<PricedItem> items,
            BigDecimal itemsSubtotal,
            BigDecimal shippingFee,
            BigDecimal shopDiscount,
            BigDecimal platformDiscount,
            LocalDate estimatedFrom,
            LocalDate estimatedTo) {
        CheckoutShopQuoteResponse response() {
            return new CheckoutShopQuoteResponse(
                    items.getFirst().getShop().getId(), items.getFirst().getShop().getName(),
                    items.stream().map(item -> item.item().getId()).toList(), itemsSubtotal, shippingFee,
                    shopDiscount.add(platformDiscount),
                    itemsSubtotal.add(shippingFee).subtract(shopDiscount).subtract(platformDiscount),
                    estimatedFrom, estimatedTo);
        }
    }
}
