package com.shoppew.product.entity;

import com.shoppew.catalog.entity.BrandEntity;
import com.shoppew.catalog.entity.CategoryEntity;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private BrandEntity brand;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 280)
    private String slug;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Column(name = "sold_count", nullable = false)
    private long soldCount;

    @Column(name = "moderation_note", length = 1000)
    private String moderationNote;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ProductEntity() {}

    public static ProductEntity create(
            ShopEntity shop,
            CategoryEntity category,
            BrandEntity brand,
            String name,
            String slug,
            String shortDescription,
            String description,
            Instant now) {
        ProductEntity product = new ProductEntity();
        product.shop = shop;
        product.category = category;
        product.brand = brand;
        product.name = name;
        product.slug = slug;
        product.shortDescription = shortDescription;
        product.description = description;
        product.status = ProductStatus.DRAFT;
        product.ratingAverage = BigDecimal.ZERO.setScale(2);
        product.reviewCount = 0;
        product.soldCount = 0;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }

    public UUID getId() { return id; }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop.getId(); }
    public CategoryEntity getCategory() { return category; }
    public BrandEntity getBrand() { return brand; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getShortDescription() { return shortDescription; }
    public String getDescription() { return description; }
    public ProductStatus getStatus() { return status; }
    public BigDecimal getRatingAverage() { return ratingAverage; }
    public long getReviewCount() { return reviewCount; }
    public long getSoldCount() { return soldCount; }
    public String getModerationNote() { return moderationNote; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            CategoryEntity category,
            BrandEntity brand,
            String name,
            String slug,
            String shortDescription,
            String description,
            Instant now) {
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.slug = slug;
        this.shortDescription = shortDescription;
        this.description = description;
        if (status == ProductStatus.REJECTED || status == ProductStatus.HIDDEN) {
            status = ProductStatus.DRAFT;
            moderationNote = null;
        }
        updatedAt = now;
    }

    public boolean isSellerEditable() {
        return status == ProductStatus.DRAFT
                || status == ProductStatus.REJECTED
                || status == ProductStatus.HIDDEN;
    }

    public void submit(Instant now) {
        status = ProductStatus.PENDING_REVIEW;
        moderationNote = null;
        updatedAt = now;
    }

    public void approve(Instant now) {
        status = ProductStatus.ACTIVE;
        moderationNote = null;
        if (publishedAt == null) {
            publishedAt = now;
        }
        updatedAt = now;
    }

    public void reject(String reason, Instant now) {
        status = ProductStatus.REJECTED;
        moderationNote = reason;
        updatedAt = now;
    }

    public void hide(String reason, Instant now) {
        status = ProductStatus.HIDDEN;
        moderationNote = reason;
        updatedAt = now;
    }

    public void archive(Instant now) {
        status = ProductStatus.ARCHIVED;
        updatedAt = now;
    }
}
