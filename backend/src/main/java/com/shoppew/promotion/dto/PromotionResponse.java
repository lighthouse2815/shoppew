package com.shoppew.promotion.dto;

import com.shoppew.promotion.entity.PromotionOwnerType;
import com.shoppew.promotion.entity.PromotionStatus;
import com.shoppew.promotion.entity.PromotionType;
import com.shoppew.voucher.entity.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PromotionResponse(
        UUID id, PromotionOwnerType ownerType, UUID shopId, String name, PromotionType promotionType,
        DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscount,
        Instant startsAt, Instant endsAt, PromotionStatus status, List<Target> targets,
        Instant createdAt, Instant updatedAt) {
    public record Target(UUID id, UUID productId, UUID variantId, BigDecimal promotionalPrice,
            Long quantityLimit, long soldQuantity) {}
}
