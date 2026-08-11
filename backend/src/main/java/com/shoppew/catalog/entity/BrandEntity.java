package com.shoppew.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands")
public class BrandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 180)
    private String slug;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CatalogStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BrandEntity() {}

    public static BrandEntity create(String name, String slug, String logoUrl, Instant now) {
        BrandEntity brand = new BrandEntity();
        brand.name = name;
        brand.slug = slug;
        brand.logoUrl = logoUrl;
        brand.status = CatalogStatus.ACTIVE;
        brand.createdAt = now;
        brand.updatedAt = now;
        return brand;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getLogoUrl() { return logoUrl; }
    public CatalogStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String name, String slug, String logoUrl, Instant now) {
        this.name = name;
        this.slug = slug;
        this.logoUrl = logoUrl;
        this.updatedAt = now;
    }

    public void changeStatus(CatalogStatus status, Instant now) {
        this.status = status;
        this.updatedAt = now;
    }
}
