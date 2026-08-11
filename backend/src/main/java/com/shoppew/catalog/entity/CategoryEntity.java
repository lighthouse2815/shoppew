package com.shoppew.catalog.entity;

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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CatalogStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CategoryEntity() {}

    public static CategoryEntity create(
            CategoryEntity parent,
            String name,
            String slug,
            String description,
            String imageUrl,
            int sortOrder,
            Instant now) {
        CategoryEntity category = new CategoryEntity();
        category.parent = parent;
        category.name = name;
        category.slug = slug;
        category.description = description;
        category.imageUrl = imageUrl;
        category.sortOrder = sortOrder;
        category.status = CatalogStatus.ACTIVE;
        category.createdAt = now;
        category.updatedAt = now;
        return category;
    }

    public UUID getId() { return id; }
    public CategoryEntity getParent() { return parent; }
    public UUID getParentId() { return parent == null ? null : parent.getId(); }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public CatalogStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(
            CategoryEntity parent,
            String name,
            String slug,
            String description,
            String imageUrl,
            int sortOrder,
            Instant now) {
        this.parent = parent;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.updatedAt = now;
    }

    public void changeStatus(CatalogStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
