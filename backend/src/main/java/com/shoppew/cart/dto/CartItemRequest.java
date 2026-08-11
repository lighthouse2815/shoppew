package com.shoppew.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CartItemRequest(
        @NotNull UUID variantId,
        @Min(1) @Max(999) long quantity) {}
