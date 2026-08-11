package com.shoppew.order.entity;

import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductVariantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariantEntity variant;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_name", nullable = false, length = 255)
    private String variantName;

    @Column(nullable = false, length = 120)
    private String sku;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrderItemEntity() {}

    public static OrderItemEntity snapshot(
            OrderEntity order,
            ProductEntity product,
            ProductVariantEntity variant,
            String productName,
            String variantName,
            String sku,
            String imageUrl,
            BigDecimal unitPrice,
            long quantity,
            String currency,
            Instant now) {
        OrderItemEntity item = new OrderItemEntity();
        item.order = order;
        item.product = product;
        item.variant = variant;
        item.productName = productName;
        item.variantName = variantName;
        item.sku = sku;
        item.imageUrl = imageUrl;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        item.currency = currency;
        item.createdAt = now;
        return item;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return order.getId(); }
    public OrderEntity getOrder() { return order; }
    public ProductEntity getProduct() { return product; }
    public ProductVariantEntity getVariant() { return variant; }
    public UUID getProductId() { return product == null ? null : product.getId(); }
    public UUID getVariantId() { return variant == null ? null : variant.getId(); }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }
    public String getSku() { return sku; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public long getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
