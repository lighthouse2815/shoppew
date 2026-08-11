package com.shoppew.promotion.entity;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderItemEntity;
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
@Table(name = "promotion_usages")
public class PromotionUsageEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "promotion_product_id")
    private PromotionProductEntity promotionProduct;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "checkout_group_id")
    private CheckoutGroupEntity checkoutGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id")
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id")
    private OrderItemEntity orderItem;
    @Column(nullable = false) private long quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private PromotionUsageStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "released_at") private Instant releasedAt;

    protected PromotionUsageEntity() {}
    public static PromotionUsageEntity reserve(PromotionProductEntity product, CheckoutGroupEntity checkout,
            OrderEntity order, OrderItemEntity item, long quantity, Instant now) {
        PromotionUsageEntity usage = new PromotionUsageEntity();
        usage.promotionProduct = product; usage.checkoutGroup = checkout; usage.order = order;
        usage.orderItem = item; usage.quantity = quantity; usage.status = PromotionUsageStatus.RESERVED;
        usage.createdAt = now; return usage;
    }
    public UUID getPromotionProductId() { return promotionProduct.getId(); }
    public UUID getCheckoutGroupId() { return checkoutGroup.getId(); }
    public UUID getOrderId() { return order.getId(); }
    public long getQuantity() { return quantity; }
    public PromotionUsageStatus getStatus() { return status; }
    public void consume(Instant now) { if (status == PromotionUsageStatus.RESERVED) { status = PromotionUsageStatus.CONSUMED; consumedAt = now; } }
    public void release(Instant now) { if (status != PromotionUsageStatus.RELEASED) { status = PromotionUsageStatus.RELEASED; releasedAt = now; } }
}
