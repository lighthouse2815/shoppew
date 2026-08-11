package com.shoppew.shop.entity;

import com.shoppew.user.entity.UserEntity;
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
@Table(name = "shops")
public class ShopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "banner_url", length = 1000)
    private String bannerUrl;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ShopStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ShopEntity() {}

    public static ShopEntity create(UserEntity owner, String name, String slug, String description, Instant now) {
        ShopEntity shop = new ShopEntity();
        shop.owner = owner;
        shop.name = name;
        shop.slug = slug;
        shop.description = description;
        shop.ratingAverage = BigDecimal.ZERO.setScale(2);
        shop.reviewCount = 0;
        shop.status = ShopStatus.PENDING;
        shop.createdAt = now;
        shop.updatedAt = now;
        return shop;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return owner.getId();
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public ShopStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String name,
            String slug,
            String description,
            String logoUrl,
            String bannerUrl,
            Instant now) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.updatedAt = now;
    }

    public void changeStatus(ShopStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
