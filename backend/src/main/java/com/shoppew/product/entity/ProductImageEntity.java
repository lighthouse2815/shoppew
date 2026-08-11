package com.shoppew.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "object_key", nullable = false, length = 700)
    private String objectKey;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ProductImageEntity() {}

    public static ProductImageEntity create(
            ProductEntity product,
            String objectKey,
            String url,
            String altText,
            int sortOrder,
            boolean primary,
            Instant now) {
        ProductImageEntity image = new ProductImageEntity();
        image.product = product;
        image.objectKey = objectKey;
        image.url = url;
        image.altText = altText;
        image.sortOrder = sortOrder;
        image.primary = primary;
        image.createdAt = now;
        return image;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return product.getId(); }
    public String getObjectKey() { return objectKey; }
    public String getUrl() { return url; }
    public String getAltText() { return altText; }
    public int getSortOrder() { return sortOrder; }
    public boolean isPrimary() { return primary; }
    public Instant getCreatedAt() { return createdAt; }

    public void makePrimary() {
        primary = true;
    }
}
