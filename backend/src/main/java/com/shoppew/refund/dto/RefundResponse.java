package com.shoppew.refund.dto;

import com.shoppew.order.entity.OrderStatus;
import com.shoppew.refund.entity.RefundReason;
import com.shoppew.refund.entity.RefundRequestStatus;
import com.shoppew.refund.entity.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefundResponse(UUID id, String requestNumber, UUID orderId, String orderNumber,
        UUID userId, UUID shopId, RefundReason reason, String description, RefundRequestStatus status,
        BigDecimal requestedAmount, BigDecimal approvedAmount, String currency, UUID reviewedById,
        String reviewNote, OrderStatus previousOrderStatus, List<Item> items, Refund refund,
        Instant createdAt, Instant updatedAt) {
    public record Item(UUID orderItemId, String productName, String variantName, long quantity,
            BigDecimal requestedAmount) {}
    public record Refund(UUID id, UUID paymentId, String providerReference, BigDecimal amount,
            RefundStatus status, Instant completedAt) {}
}
