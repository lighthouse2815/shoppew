package com.shoppew.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PromotionTargetRequest(
        @NotNull UUID productId,
        UUID variantId,
        @DecimalMin("0.00") BigDecimal promotionalPrice,
        @Min(1) Long quantityLimit) {}
