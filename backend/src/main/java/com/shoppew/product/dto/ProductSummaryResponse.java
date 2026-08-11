package com.shoppew.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        UUID shopId,
        String shopName,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String name,
        String slug,
        String shortDescription,
        String status,
        String primaryImageUrl,
        BigDecimal minimumPrice,
        BigDecimal originalMinimumPrice,
        String currency,
        BigDecimal ratingAverage,
        long reviewCount,
        long soldCount,
        Instant publishedAt,
        Instant createdAt) {}
