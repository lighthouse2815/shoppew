package com.shoppew.order.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    PROCESSING,
    READY_TO_SHIP,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUND_REQUESTED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
