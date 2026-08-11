package com.shoppew.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderEmail,
        boolean mine,
        String type,
        String textContent,
        String mediaUrl,
        UUID productId,
        String productName,
        UUID orderId,
        String orderNumber,
        Instant sentAt) {}
