package com.shoppew.wishlist.entity;

import com.shoppew.product.entity.ProductEntity;
import com.shoppew.user.entity.UserEntity;
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
@Table(name = "wishlists")
public class WishlistEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private ProductEntity product;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected WishlistEntity() {}
    public static WishlistEntity create(UserEntity user, ProductEntity product, Instant now) {
        WishlistEntity item = new WishlistEntity(); item.user = user; item.product = product; item.createdAt = now; return item;
    }
    public UUID getId() { return id; }
    public ProductEntity getProduct() { return product; }
    public Instant getCreatedAt() { return createdAt; }
}
