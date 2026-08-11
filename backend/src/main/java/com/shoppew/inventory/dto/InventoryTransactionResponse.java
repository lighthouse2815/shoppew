package com.shoppew.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryTransactionResponse(
        UUID id,
        UUID variantId,
        String type,
        long quantity,
        long availableBefore,
        long availableAfter,
        long reservedBefore,
        long reservedAfter,
        String referenceType,
        UUID referenceId,
        String note,
        UUID actorId,
        Instant createdAt) {}
