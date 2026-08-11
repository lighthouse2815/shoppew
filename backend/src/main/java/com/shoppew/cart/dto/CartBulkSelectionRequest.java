package com.shoppew.cart.dto;

import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record CartBulkSelectionRequest(
        @Size(max = 500) Set<UUID> itemIds,
        boolean selected) {}
