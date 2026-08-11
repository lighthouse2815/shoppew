package com.shoppew.payment.entity;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
