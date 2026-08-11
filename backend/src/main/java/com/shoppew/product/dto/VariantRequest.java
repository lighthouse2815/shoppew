package com.shoppew.product.dto;

import com.shoppew.common.validation.HttpUrl;
import com.shoppew.product.entity.VariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record VariantRequest(
        @NotBlank @Size(max = 120) String sku,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal price,
        @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal compareAtPrice,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @Min(1) Integer weightGrams,
        @Min(1) Integer lengthMm,
        @Min(1) Integer widthMm,
        @Min(1) Integer heightMm,
        @Size(max = 1000) @HttpUrl String imageUrl,
        @NotNull Set<UUID> optionValueIds,
        VariantStatus status) {}
