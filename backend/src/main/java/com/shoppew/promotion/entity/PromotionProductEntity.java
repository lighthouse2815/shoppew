package com.shoppew.promotion.entity;

import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promotion_products")
public class PromotionProductEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_id")
    private PromotionEntity promotion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id")
    private ProductEntity product;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id")
    private ProductVariantEntity variant;
    @Column(name = "promotional_price", precision = 19, scale = 2)
    private BigDecimal promotionalPrice;
    @Column(name = "quantity_limit") private Long quantityLimit;
    @Column(name = "sold_quantity", nullable = false) private long soldQuantity;

    protected PromotionProductEntity() {}

    public static PromotionProductEntity create(PromotionEntity promotion, ProductEntity product,
            ProductVariantEntity variant, BigDecimal promotionalPrice, Long quantityLimit) {
        PromotionProductEntity target = new PromotionProductEntity();
        target.promotion = promotion; target.product = product; target.variant = variant;
        target.promotionalPrice = promotionalPrice; target.quantityLimit = quantityLimit; target.soldQuantity = 0;
        return target;
    }

    public UUID getId() { return id; }
    public PromotionEntity getPromotion() { return promotion; }
    public ProductEntity getProduct() { return product; }
    public UUID getProductId() { return product.getId(); }
    public ProductVariantEntity getVariant() { return variant; }
    public UUID getVariantId() { return variant == null ? null : variant.getId(); }
    public BigDecimal getPromotionalPrice() { return promotionalPrice; }
    public Long getQuantityLimit() { return quantityLimit; }
    public long getSoldQuantity() { return soldQuantity; }
    public boolean hasCapacity(long quantity) { return quantityLimit == null || soldQuantity + quantity <= quantityLimit; }
}
