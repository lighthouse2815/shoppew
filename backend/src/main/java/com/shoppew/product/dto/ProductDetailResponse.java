package com.shoppew.product.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
        UUID id,
        UUID shopId,
        String shopName,
        String shopSlug,
        UUID categoryId,
        String categoryName,
        UUID brandId,
        String brandName,
        String name,
        String slug,
        String shortDescription,
        String description,
        String status,
        String moderationNote,
        BigDecimal ratingAverage,
        long reviewCount,
        long soldCount,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<ProductImageResponse> images,
        List<ProductOptionResponse> options,
        List<VariantResponse> variants,
        List<ProductAttributeResponse> attributes) {}
