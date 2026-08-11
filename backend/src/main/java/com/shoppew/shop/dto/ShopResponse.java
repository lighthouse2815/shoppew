package com.shoppew.shop.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShopResponse(
        UUID id,
        UUID ownerId,
        String name,
        String slug,
        String description,
        String logoUrl,
        String bannerUrl,
        BigDecimal ratingAverage,
        long reviewCount,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
