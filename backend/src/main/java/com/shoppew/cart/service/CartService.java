package com.shoppew.cart.service;

import com.shoppew.cart.dto.CartBulkSelectionRequest;
import com.shoppew.cart.dto.CartItemRequest;
import com.shoppew.cart.dto.CartItemResponse;
import com.shoppew.cart.dto.CartQuantityRequest;
import com.shoppew.cart.dto.CartResponse;
import com.shoppew.cart.dto.CartSelectionRequest;
import com.shoppew.cart.dto.CartShopGroupResponse;
import com.shoppew.cart.entity.CartEntity;
import com.shoppew.cart.entity.CartItemEntity;
import com.shoppew.cart.repository.CartItemRepository;
import com.shoppew.cart.repository.CartRepository;
import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import com.shoppew.inventory.entity.InventoryEntity;
import com.shoppew.inventory.repository.InventoryRepository;
import com.shoppew.product.dto.VariantSelectionResponse;
import com.shoppew.product.entity.ProductImageEntity;
import com.shoppew.product.entity.ProductOptionValueEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductImageRepository;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.promotion.service.PromotionPricingService;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final InventoryRepository inventoryRepository;
    private final PromotionPricingService promotionPricing;
    private final AppProperties properties;
    private final Clock clock;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository itemRepository,
            UserRepository userRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            InventoryRepository inventoryRepository,
            PromotionPricingService promotionPricing,
            AppProperties properties,
            Clock clock) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.inventoryRepository = inventoryRepository;
        this.promotionPricing = promotionPricing;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CartResponse get(UUID userId) {
        return response(requireCart(userId));
    }

    @Transactional
    public CartResponse add(UUID userId, CartItemRequest request) {
        CartEntity cart = requireCart(userId);
        ProductVariantEntity variant = requireVariant(request.variantId());
        CartItemEntity existing = itemRepository.findByCart_IdAndVariant_Id(cart.getId(), variant.getId())
                .orElse(null);
        long desiredQuantity;
        try {
            desiredQuantity = existing == null
                    ? request.quantity()
                    : Math.addExact(existing.getQuantity(), request.quantity());
        } catch (ArithmeticException exception) {
            throw invalidQuantity();
        }
        validateQuantity(desiredQuantity);
        requirePurchasable(variant, desiredQuantity);
        Instant now = Instant.now(clock);
        if (existing == null) {
            itemRepository.save(CartItemEntity.create(cart, variant, desiredQuantity, now));
        } else {
            existing.updateQuantity(desiredQuantity, now);
        }
        cart.touch(now);
        return response(cart);
    }

    @Transactional
    public CartResponse updateQuantity(UUID userId, UUID itemId, CartQuantityRequest request) {
        CartEntity cart = requireCart(userId);
        CartItemEntity item = requireItem(cart, itemId);
        validateQuantity(request.quantity());
        requirePurchasable(item.getVariant(), request.quantity());
        Instant now = Instant.now(clock);
        item.updateQuantity(request.quantity(), now);
        cart.touch(now);
        return response(cart);
    }

    @Transactional
    public CartResponse select(UUID userId, UUID itemId, CartSelectionRequest request) {
        CartEntity cart = requireCart(userId);
        CartItemEntity item = requireItem(cart, itemId);
        Instant now = Instant.now(clock);
        item.select(request.selected(), now);
        cart.touch(now);
        return response(cart);
    }

    @Transactional
    public CartResponse selectMany(UUID userId, CartBulkSelectionRequest request) {
        CartEntity cart = requireCart(userId);
        List<CartItemEntity> items = itemRepository.findAllDetailedByCartId(cart.getId());
        Set<UUID> requestedIds = request.itemIds() == null ? Set.of() : Set.copyOf(request.itemIds());
        if (!requestedIds.isEmpty()) {
            Set<UUID> ownedIds = items.stream().map(CartItemEntity::getId).collect(Collectors.toSet());
            if (!ownedIds.containsAll(requestedIds)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                        "Không tìm thấy một hoặc nhiều sản phẩm trong giỏ hàng");
            }
        }
        Instant now = Instant.now(clock);
        items.stream()
                .filter(item -> requestedIds.isEmpty() || requestedIds.contains(item.getId()))
                .forEach(item -> item.select(request.selected(), now));
        cart.touch(now);
        return response(cart);
    }

    @Transactional
    public CartResponse remove(UUID userId, UUID itemId) {
        CartEntity cart = requireCart(userId);
        itemRepository.delete(requireItem(cart, itemId));
        cart.touch(Instant.now(clock));
        return response(cart);
    }

    @Transactional
    public CartResponse clear(UUID userId) {
        CartEntity cart = requireCart(userId);
        itemRepository.deleteAllByCart_Id(cart.getId());
        cart.touch(Instant.now(clock));
        return response(cart);
    }

    private CartEntity requireCart(UUID userId) {
        return cartRepository.findByUser_Id(userId).orElseGet(() -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                            "Không tìm thấy người dùng"));
            return cartRepository.save(CartEntity.create(user, Instant.now(clock)));
        });
    }

    private CartItemEntity requireItem(CartEntity cart, UUID itemId) {
        return itemRepository.findByIdAndCart_Id(itemId, cart.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND",
                        "Không tìm thấy sản phẩm trong giỏ hàng"));
    }

    private ProductVariantEntity requireVariant(UUID variantId) {
        return variantRepository.findForInventory(variantId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND",
                        "Không tìm thấy phân loại sản phẩm"));
    }

    private void requirePurchasable(ProductVariantEntity variant, long desiredQuantity) {
        if (variant.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "SHOP_NOT_ACTIVE", "Cửa hàng hiện không hoạt động");
        }
        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "PRODUCT_NOT_AVAILABLE", "Sản phẩm hiện không thể mua");
        }
        if (variant.getStatus() != VariantStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "VARIANT_NOT_ELIGIBLE", "Phân loại hiện không thể mua");
        }
        long available = inventoryRepository.findById(variant.getId())
                .map(InventoryEntity::getAvailableQuantity)
                .orElse(0L);
        if (available < desiredQuantity) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
                    "Sản phẩm không còn đủ tồn kho khả dụng");
        }
    }

    private void validateQuantity(long quantity) {
        if (quantity < 1 || quantity > 999) throw invalidQuantity();
    }

    private ApiException invalidQuantity() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CART_QUANTITY",
                "Số lượng trong giỏ hàng phải từ 1 đến 999");
    }

    private CartResponse response(CartEntity cart) {
        List<CartItemEntity> items = itemRepository.findAllDetailedByCartId(cart.getId());
        if (items.isEmpty()) {
            return new CartResponse(cart.getId(), List.of(), 0, 0, moneyZero(),
                    properties.currency(), Instant.now(clock));
        }
        List<UUID> variantIds = items.stream().map(item -> item.getVariant().getId()).toList();
        Map<UUID, InventoryEntity> inventories = inventoryRepository.findAllDetailedByVariantIdIn(variantIds)
                .stream().collect(Collectors.toMap(InventoryEntity::getVariantId, inventory -> inventory));
        List<UUID> productIds = items.stream().map(item -> item.getProduct().getId()).distinct().toList();
        Map<UUID, List<ProductImageEntity>> images = imageRepository.findAllByProduct_IdInOrderBySortOrderAsc(productIds)
                .stream().collect(Collectors.groupingBy(ProductImageEntity::getProductId));
        Map<UUID, PromotionPricingService.PriceDecision> promotionPrices = promotionPricing.prices(items);

        Map<UUID, List<CartItemResponse>> groupedItems = new LinkedHashMap<>();
        Map<UUID, CartItemEntity> groupSources = new HashMap<>();
        long itemCount = 0;
        long selectedItemCount = 0;
        BigDecimal selectedSubtotal = moneyZero();
        for (CartItemEntity item : items) {
            InventoryEntity inventory = inventories.get(item.getVariant().getId());
            CartItemResponse itemResponse = itemResponse(item, inventory,
                    images.getOrDefault(item.getProduct().getId(), List.of()), promotionPrices.get(item.getId()));
            groupedItems.computeIfAbsent(item.getShop().getId(), ignored -> new ArrayList<>()).add(itemResponse);
            groupSources.putIfAbsent(item.getShop().getId(), item);
            itemCount += item.getQuantity();
            if (item.isSelected()) {
                selectedItemCount += item.getQuantity();
                if (itemResponse.eligible()) selectedSubtotal = selectedSubtotal.add(itemResponse.lineTotal());
            }
        }

        List<CartShopGroupResponse> groups = groupedItems.entrySet().stream()
                .map(entry -> {
                    CartItemEntity source = groupSources.get(entry.getKey());
                    BigDecimal subtotal = entry.getValue().stream()
                            .filter(item -> item.selected() && item.eligible())
                            .map(CartItemResponse::lineTotal)
                            .reduce(moneyZero(), BigDecimal::add);
                    boolean eligible = entry.getValue().stream()
                            .filter(CartItemResponse::selected)
                            .allMatch(CartItemResponse::eligible);
                    return new CartShopGroupResponse(
                            source.getShop().getId(), source.getShop().getName(), source.getShop().getSlug(),
                            source.getShop().getLogoUrl(), List.copyOf(entry.getValue()), subtotal, eligible);
                })
                .sorted(Comparator.comparing(CartShopGroupResponse::shopName))
                .toList();
        return new CartResponse(cart.getId(), groups, itemCount, selectedItemCount, selectedSubtotal,
                properties.currency(), Instant.now(clock));
    }

    private CartItemResponse itemResponse(
            CartItemEntity item,
            InventoryEntity inventory,
            List<ProductImageEntity> images,
            PromotionPricingService.PriceDecision price) {
        long available = inventory == null ? 0 : inventory.getAvailableQuantity();
        long threshold = inventory == null ? 0 : inventory.getLowStockThreshold();
        List<String> issues = new ArrayList<>();
        if (item.getShop().getStatus() != ShopStatus.ACTIVE) issues.add("SHOP_INACTIVE");
        if (item.getProduct().getStatus() != ProductStatus.ACTIVE) issues.add("PRODUCT_UNAVAILABLE");
        if (item.getVariant().getStatus() != VariantStatus.ACTIVE) issues.add("VARIANT_UNAVAILABLE");
        if (available == 0) issues.add("OUT_OF_STOCK");
        else if (available < item.getQuantity()) issues.add("INSUFFICIENT_STOCK");
        String stockStatus = available == 0 ? "OUT_OF_STOCK"
                : available < item.getQuantity() ? "INSUFFICIENT"
                : available <= threshold ? "LOW_STOCK" : "AVAILABLE";
        BigDecimal lineTotal = price.unitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        ProductImageEntity primary = images.stream().filter(ProductImageEntity::isPrimary).findFirst()
                .orElse(images.isEmpty() ? null : images.getFirst());
        String imageUrl = item.getVariant().getImageUrl() == null
                ? primary == null ? null : primary.getUrl()
                : item.getVariant().getImageUrl();
        List<VariantSelectionResponse> selections = item.getVariant().getOptionValues().stream()
                .sorted(Comparator.comparingInt(value -> value.getOption().getSortOrder()))
                .map(this::selection)
                .toList();
        return new CartItemResponse(
                item.getId(), item.getShop().getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getSlug(), imageUrl, item.getVariant().getId(), item.getVariant().getSku(),
                item.getVariant().getName(), selections, item.getQuantity(), item.isSelected(),
                price.unitPrice(), item.getVariant().getPrice(), price.promotionId(), price.promotionName(),
                lineTotal, item.getVariant().getCurrency(), available, stockStatus,
                issues.isEmpty(), List.copyOf(issues), item.getUpdatedAt());
    }

    private VariantSelectionResponse selection(ProductOptionValueEntity value) {
        return new VariantSelectionResponse(
                value.getOptionId(), value.getOption().getName(), value.getId(), value.getValue());
    }

    private BigDecimal moneyZero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
