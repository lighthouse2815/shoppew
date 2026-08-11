package com.shoppew.order.entity;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checkout_group_id", nullable = false)
    private CheckoutGroupEntity checkoutGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "items_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal itemsSubtotal;

    @Column(name = "shipping_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingTotal;

    @Column(name = "shop_discount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal shopDiscountTotal;

    @Column(name = "platform_discount_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformDiscountTotal;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "customer_note", length = 500)
    private String customerNote;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected OrderEntity() {}

    public static OrderEntity create(
            String orderNumber,
            CheckoutGroupEntity checkoutGroup,
            UserEntity user,
            ShopEntity shop,
            OrderStatus status,
            String currency,
            BigDecimal itemsSubtotal,
            BigDecimal shippingTotal,
            BigDecimal shopDiscountTotal,
            BigDecimal platformDiscountTotal,
            String customerNote,
            Instant now) {
        OrderEntity order = new OrderEntity();
        order.orderNumber = orderNumber;
        order.checkoutGroup = checkoutGroup;
        order.user = user;
        order.shop = shop;
        order.status = status;
        order.currency = currency;
        order.itemsSubtotal = itemsSubtotal;
        order.shippingTotal = shippingTotal;
        order.shopDiscountTotal = shopDiscountTotal;
        order.platformDiscountTotal = platformDiscountTotal;
        order.grandTotal = itemsSubtotal.add(shippingTotal).subtract(shopDiscountTotal).subtract(platformDiscountTotal);
        order.customerNote = customerNote;
        order.placedAt = now;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public CheckoutGroupEntity getCheckoutGroup() { return checkoutGroup; }
    public UUID getCheckoutGroupId() { return checkoutGroup.getId(); }
    public UUID getUserId() { return user.getId(); }
    public String getUserEmail() { return user.getEmail(); }
    public ShopEntity getShop() { return shop; }
    public UUID getShopId() { return shop.getId(); }
    public OrderStatus getStatus() { return status; }
    public String getCurrency() { return currency; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public BigDecimal getShippingTotal() { return shippingTotal; }
    public BigDecimal getShopDiscountTotal() { return shopDiscountTotal; }
    public BigDecimal getPlatformDiscountTotal() { return platformDiscountTotal; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public String getCustomerNote() { return customerNote; }
    public Instant getPlacedAt() { return placedAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void transition(OrderStatus next, Instant now) {
        status = next;
        updatedAt = now;
        if (next == OrderStatus.PAID) paidAt = now;
        if (next == OrderStatus.COMPLETED) completedAt = now;
        if (next == OrderStatus.CANCELLED) cancelledAt = now;
    }
}
