package com.shoppew.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID shopId,
        String shopName,
        UUID customerId,
        String customerEmail,
        String status,
        String lastMessagePreview,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt) {}
