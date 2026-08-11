package com.shoppew.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record InventoryAdjustmentRequest(
        @NotNull InventoryAdjustmentMode mode,
        @NotNull @PositiveOrZero Long quantity,
        @PositiveOrZero Long lowStockThreshold,
        @Size(max = 500) String note) {}
