package com.shoppew.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String productSlug,
        String sku,
        String variantName,
        String variantStatus,
        long availableQuantity,
        long reservedQuantity,
        long soldQuantity,
        long lowStockThreshold,
        boolean lowStock,
        Instant updatedAt) {}
