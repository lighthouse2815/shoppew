package com.shoppew.promotion.service;

import com.shoppew.cart.entity.CartItemEntity;
import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.common.exception.ApiException;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.promotion.entity.PromotionProductEntity;
import com.shoppew.promotion.entity.PromotionUsageEntity;
import com.shoppew.promotion.entity.PromotionUsageStatus;
import com.shoppew.promotion.repository.PromotionProductRepository;
import com.shoppew.promotion.repository.PromotionUsageRepository;
import com.shoppew.voucher.entity.DiscountType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionPricingService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final PromotionProductRepository targetRepository;
    private final PromotionUsageRepository usageRepository;
    private final Clock clock;

    public PromotionPricingService(PromotionProductRepository targetRepository,
            PromotionUsageRepository usageRepository, Clock clock) {
        this.targetRepository = targetRepository; this.usageRepository = usageRepository; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<UUID, PriceDecision> prices(List<CartItemEntity> items) {
        if (items.isEmpty()) return Map.of();
        Instant now = Instant.now(clock);
        Collection<UUID> productIds = items.stream().map(item -> item.getProduct().getId()).distinct().toList();
        List<PromotionProductEntity> targets = targetRepository.findApplicable(productIds, now);
        Map<UUID, PriceDecision> result = new HashMap<>();
        for (CartItemEntity item : items) {
            result.put(item.getId(), decision(item.getVariant(), item.getQuantity(), targets));
        }
        return Map.copyOf(result);
    }

    @Transactional(readOnly = true)
    public Map<UUID, PriceDecision> variantPrices(List<ProductVariantEntity> variants) {
        if (variants.isEmpty()) return Map.of();
        Collection<UUID> productIds = variants.stream().map(ProductVariantEntity::getProductId).distinct().toList();
        List<PromotionProductEntity> targets = targetRepository.findApplicable(productIds, Instant.now(clock));
        Map<UUID, PriceDecision> result = new HashMap<>();
        variants.forEach(variant -> result.put(variant.getId(), decision(variant, 1, targets)));
        return Map.copyOf(result);
    }

    @Transactional
    public void reserve(UUID promotionProductId, CheckoutGroupEntity checkout, OrderEntity order,
            OrderItemEntity orderItem, long quantity) {
        if (promotionProductId == null) return;
        if (targetRepository.reserveCapacity(promotionProductId, quantity) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "PROMOTION_QUANTITY_EXHAUSTED",
                    "Số lượng promotion vừa hết, vui lòng xem lại checkout");
        }
        PromotionProductEntity target = targetRepository.getReferenceById(promotionProductId);
        usageRepository.save(PromotionUsageEntity.reserve(
                target, checkout, order, orderItem, quantity, Instant.now(clock)));
    }

    @Transactional
    public void consumeCheckout(UUID checkoutId) {
        Instant now = Instant.now(clock);
        usageRepository.findAllByCheckout(checkoutId).forEach(usage -> usage.consume(now));
    }

    @Transactional
    public void releaseCheckout(UUID checkoutId) {
        release(usageRepository.findAllByCheckout(checkoutId));
    }

    @Transactional
    public void releaseOrder(UUID orderId) {
        release(usageRepository.findAllByOrder(orderId));
    }

    private void release(List<PromotionUsageEntity> usages) {
        Instant now = Instant.now(clock);
        for (PromotionUsageEntity usage : usages) {
            if (usage.getStatus() == PromotionUsageStatus.RELEASED) continue;
            if (targetRepository.releaseCapacity(usage.getPromotionProductId(), usage.getQuantity()) != 1) {
                throw new IllegalStateException("Promotion capacity ledger is inconsistent");
            }
            usage.release(now);
        }
    }

    private BigDecimal discountPrice(BigDecimal base, PromotionProductEntity target) {
        BigDecimal discount = target.getPromotion().getDiscountType() == DiscountType.FIXED
                ? target.getPromotion().getDiscountValue()
                : base.multiply(target.getPromotion().getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        if (target.getPromotion().getMaxDiscount() != null) discount = discount.min(target.getPromotion().getMaxDiscount());
        return money(base.subtract(discount).max(BigDecimal.ZERO));
    }

    private PriceDecision decision(ProductVariantEntity variant, long quantity,
            List<PromotionProductEntity> targets) {
        BigDecimal base = money(variant.getPrice());
        PriceDecision best = new PriceDecision(base, null, null, null);
        for (PromotionProductEntity target : targets) {
            if (!target.getProductId().equals(variant.getProductId())) continue;
            if (target.getVariantId() != null && !target.getVariantId().equals(variant.getId())) continue;
            if (target.getPromotion().getShopId() != null
                    && !target.getPromotion().getShopId().equals(variant.getShopId())) continue;
            if (!target.hasCapacity(quantity)) continue;
            BigDecimal candidate = target.getPromotionalPrice() == null
                    ? discountPrice(base, target) : money(target.getPromotionalPrice());
            if (candidate.compareTo(best.unitPrice()) < 0
                    || (candidate.compareTo(best.unitPrice()) == 0 && best.promotionProductId() != null
                        && target.getId().toString().compareTo(best.promotionProductId().toString()) < 0)) {
                best = new PriceDecision(candidate, target.getId(), target.getPromotion().getId(),
                        target.getPromotion().getName());
            }
        }
        return best;
    }

    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    public record PriceDecision(
            BigDecimal unitPrice, UUID promotionProductId, UUID promotionId, String promotionName) {}
}
