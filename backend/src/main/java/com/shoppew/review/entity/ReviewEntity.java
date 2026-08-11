package com.shoppew.review.entity;

import com.shoppew.order.entity.OrderItemEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.shop.entity.ShopEntity;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class ReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private ShopEntity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private ProductEntity product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id") private OrderItemEntity orderItem;
    @Column(nullable = false) private short rating;
    @Column(columnDefinition = "text") private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private ReviewStatus status;
    @Column(name = "seller_reply", columnDefinition = "text") private String sellerReply;
    @Column(name = "seller_replied_at") private Instant sellerRepliedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;
    protected ReviewEntity() {}
    public static ReviewEntity create(UserEntity user, OrderItemEntity item, int rating, String content, Instant now) {
        ReviewEntity review = new ReviewEntity(); review.user = user; review.orderItem = item;
        review.product = item.getProduct(); review.shop = item.getOrder().getShop(); review.rating = (short) rating;
        review.content = content; review.status = ReviewStatus.PUBLISHED; review.createdAt = now; review.updatedAt = now;
        return review;
    }
    public UUID getId() { return id; }
    public UUID getUserId() { return user.getId(); }
    public UUID getShopId() { return shop.getId(); }
    public UUID getProductId() { return product.getId(); }
    public UUID getOrderItemId() { return orderItem.getId(); }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public ReviewStatus getStatus() { return status; }
    public String getSellerReply() { return sellerReply; }
    public Instant getSellerRepliedAt() { return sellerRepliedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(int rating, String content, Instant now) { this.rating = (short) rating; this.content = content; this.updatedAt = now; }
    public void reply(String reply, Instant now) { sellerReply = reply; sellerRepliedAt = now; updatedAt = now; }
    public void changeStatus(ReviewStatus status, Instant now) { this.status = status; this.updatedAt = now; }
}
