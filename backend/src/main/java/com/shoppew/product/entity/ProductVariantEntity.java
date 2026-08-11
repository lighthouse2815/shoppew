package com.shoppew.product.entity;

import com.shoppew.shop.entity.ShopEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_variants")
public class ProductVariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 19, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "length_mm")
    private Integer lengthMm;

    @Column(name = "width_mm")
    private Integer widthMm;

    @Column(name = "height_mm")
    private Integer heightMm;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private VariantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_variant_option_values",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "option_value_id"))
    private Set<ProductOptionValueEntity> optionValues = new LinkedHashSet<>();

    protected ProductVariantEntity() {}

    public static ProductVariantEntity create(
            ProductEntity product,
            String sku,
            String name,
            BigDecimal price,
            BigDecimal compareAtPrice,
            String currency,
            Integer weightGrams,
            Integer lengthMm,
            Integer widthMm,
            Integer heightMm,
            String imageUrl,
            Set<ProductOptionValueEntity> optionValues,
            Instant now) {
        ProductVariantEntity variant = new ProductVariantEntity();
        variant.product = product;
        variant.shop = product.getShop();
        variant.sku = sku;
        variant.name = name;
        variant.price = price;
        variant.compareAtPrice = compareAtPrice;
        variant.currency = currency;
        variant.weightGrams = weightGrams;
        variant.lengthMm = lengthMm;
        variant.widthMm = widthMm;
        variant.heightMm = heightMm;
        variant.imageUrl = imageUrl;
        variant.status = VariantStatus.ACTIVE;
        variant.optionValues.addAll(optionValues);
        variant.createdAt = now;
        variant.updatedAt = now;
        return variant;
    }

    public UUID getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public UUID getProductId() { return product.getId(); }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop.getId(); }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public String getCurrency() { return currency; }
    public Integer getWeightGrams() { return weightGrams; }
    public Integer getLengthMm() { return lengthMm; }
    public Integer getWidthMm() { return widthMm; }
    public Integer getHeightMm() { return heightMm; }
    public String getImageUrl() { return imageUrl; }
    public VariantStatus getStatus() { return status; }
    public Set<ProductOptionValueEntity> getOptionValues() { return Set.copyOf(optionValues); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            String sku,
            String name,
            BigDecimal price,
            BigDecimal compareAtPrice,
            Integer weightGrams,
            Integer lengthMm,
            Integer widthMm,
            Integer heightMm,
            String imageUrl,
            Set<ProductOptionValueEntity> values,
            VariantStatus status,
            Instant now) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.compareAtPrice = compareAtPrice;
        this.weightGrams = weightGrams;
        this.lengthMm = lengthMm;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.imageUrl = imageUrl;
        this.optionValues.clear();
        this.optionValues.addAll(values);
        this.status = status;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        status = VariantStatus.ARCHIVED;
        updatedAt = now;
    }
}
