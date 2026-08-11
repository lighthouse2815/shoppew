package com.shoppew.refund.entity;

import com.shoppew.order.entity.OrderItemEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "refund_request_items")
@IdClass(RefundRequestItemId.class)
public class RefundRequestItemEntity {
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "refund_request_id") private RefundRequestEntity refundRequest;
    @Id @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id") private OrderItemEntity orderItem;
    @Column(nullable = false) private long quantity;
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2) private BigDecimal requestedAmount;
    @Column(name = "seller_charge_amount", nullable = false, precision = 19, scale = 2) private BigDecimal sellerChargeAmount;
    protected RefundRequestItemEntity() {}
    public static RefundRequestItemEntity create(RefundRequestEntity request, OrderItemEntity item, long quantity,
            BigDecimal requestedAmount, BigDecimal sellerChargeAmount) {
        RefundRequestItemEntity entity = new RefundRequestItemEntity(); entity.refundRequest = request;
        entity.orderItem = item; entity.quantity = quantity; entity.requestedAmount = requestedAmount;
        entity.sellerChargeAmount = sellerChargeAmount; return entity;
    }
    public java.util.UUID getOrderItemId() { return orderItem.getId(); }
    public String getProductName() { return orderItem.getProductName(); }
    public String getVariantName() { return orderItem.getVariantName(); }
    public long getQuantity() { return quantity; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getSellerChargeAmount() { return sellerChargeAmount; }
}
