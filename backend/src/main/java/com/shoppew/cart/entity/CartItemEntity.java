package com.shoppew.cart.entity;

import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import com.shoppew.shop.entity.ShopEntity;
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
@Table(name = "cart_items")
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariantEntity variant;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private boolean selected;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CartItemEntity() {}

    public static CartItemEntity create(
            CartEntity cart,
            ProductVariantEntity variant,
            long quantity,
            Instant now) {
        CartItemEntity item = new CartItemEntity();
        item.cart = cart;
        item.shop = variant.getShop();
        item.product = variant.getProduct();
        item.variant = variant;
        item.quantity = quantity;
        item.selected = true;
        item.createdAt = now;
        item.updatedAt = now;
        return item;
    }

    public UUID getId() { return id; }
    public UUID getCartId() { return cart.getId(); }
    public ShopEntity getShop() { return shop; }
    public ProductEntity getProduct() { return product; }
    public ProductVariantEntity getVariant() { return variant; }
    public long getQuantity() { return quantity; }
    public boolean isSelected() { return selected; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateQuantity(long quantity, Instant now) {
        this.quantity = quantity;
        this.updatedAt = now;
    }

    public void select(boolean selected, Instant now) {
        this.selected = selected;
        this.updatedAt = now;
    }
}
