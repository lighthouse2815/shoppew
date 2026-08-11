package com.shoppew.promotion.entity;

import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.voucher.entity.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotions")
public class PromotionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false, length = 24)
    private PromotionOwnerType ownerType;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "shop_id")
    private ShopEntity shop;
    @Column(nullable = false, length = 180) private String name;
    @Enumerated(EnumType.STRING) @Column(name = "promotion_type", nullable = false, length = 32)
    private PromotionType promotionType;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false, length = 24)
    private DiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "max_discount", precision = 19, scale = 2)
    private BigDecimal maxDiscount;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private PromotionStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PromotionEntity() {}

    public static PromotionEntity create(
            PromotionOwnerType ownerType, ShopEntity shop, String name, PromotionType promotionType,
            DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscount,
            Instant startsAt, Instant endsAt, Instant now) {
        PromotionEntity promotion = new PromotionEntity();
        promotion.ownerType = ownerType; promotion.shop = shop;
        promotion.apply(name, promotionType, discountType, discountValue, maxDiscount, startsAt, endsAt, now);
        promotion.status = PromotionStatus.DRAFT; promotion.createdAt = now;
        return promotion;
    }

    public void update(String name, PromotionType type, DiscountType discountType,
            BigDecimal discountValue, BigDecimal maxDiscount, Instant startsAt, Instant endsAt, Instant now) {
        apply(name, type, discountType, discountValue, maxDiscount, startsAt, endsAt, now);
    }

    private void apply(String name, PromotionType type, DiscountType discountType,
            BigDecimal discountValue, BigDecimal maxDiscount, Instant startsAt, Instant endsAt, Instant now) {
        this.name = name; this.promotionType = type; this.discountType = discountType;
        this.discountValue = discountValue; this.maxDiscount = maxDiscount;
        this.startsAt = startsAt; this.endsAt = endsAt; this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public PromotionOwnerType getOwnerType() { return ownerType; }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop == null ? null : shop.getId(); }
    public String getName() { return name; }
    public PromotionType getPromotionType() { return promotionType; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public PromotionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean activeAt(Instant now) {
        return (status == PromotionStatus.ACTIVE || status == PromotionStatus.SCHEDULED)
                && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }
    public void activate(Instant now) {
        status = startsAt.isAfter(now) ? PromotionStatus.SCHEDULED : PromotionStatus.ACTIVE;
        updatedAt = now;
    }
    public void pause(Instant now) { status = PromotionStatus.PAUSED; updatedAt = now; }
    public void archive(Instant now) { status = PromotionStatus.ARCHIVED; updatedAt = now; }
}
