package com.shoppew.refund.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RefundRequestItemId implements Serializable {
    private UUID refundRequest;
    private UUID orderItem;
    protected RefundRequestItemId() {}
    public RefundRequestItemId(UUID refundRequest, UUID orderItem) { this.refundRequest = refundRequest; this.orderItem = orderItem; }
    public UUID getRefundRequest() { return refundRequest; }
    public UUID getOrderItem() { return orderItem; }
    @Override public boolean equals(Object other) { return this == other || other instanceof RefundRequestItemId id && Objects.equals(refundRequest, id.refundRequest) && Objects.equals(orderItem, id.orderItem); }
    @Override public int hashCode() { return Objects.hash(refundRequest, orderItem); }
}
