package com.shoppew.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID id,
        UUID variantId,
        UUID userId,
        UUID orderId,
        long quantity,
        String status,
        Instant expiresAt,
        Instant createdAt) {}
