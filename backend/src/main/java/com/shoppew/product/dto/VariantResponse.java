package com.shoppew.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        BigDecimal originalPrice,
        UUID promotionId,
        String promotionName,
        BigDecimal compareAtPrice,
        String currency,
        Integer weightGrams,
        Integer lengthMm,
        Integer widthMm,
        Integer heightMm,
        String imageUrl,
        String status,
        List<VariantSelectionResponse> selections,
        Instant createdAt,
        Instant updatedAt) {}
