package com.shoppew.dispute.dto;

import com.shoppew.dispute.entity.DisputeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(UUID id, String disputeNumber, UUID orderId, String orderNumber, UUID shopId,
        UUID customerId, UUID refundRequestId, UUID openedById, String reason, String description,
        DisputeStatus status, UUID assignedToId, String resolution, Instant resolvedAt,
        List<Message> messages, Instant createdAt, Instant updatedAt) {
    public record Message(UUID id, UUID authorId, String content, List<String> attachments, Instant createdAt) {}
}
