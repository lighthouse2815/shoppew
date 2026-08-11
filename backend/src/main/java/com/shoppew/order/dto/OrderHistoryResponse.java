package com.shoppew.order.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderHistoryResponse(
        String fromStatus,
        String toStatus,
        UUID actorId,
        String actorType,
        String reason,
        Instant createdAt) {}
