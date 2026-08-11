package com.shoppew.cart.dto;

import com.shoppew.product.dto.VariantSelectionResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID shopId,
        UUID productId,
        String productName,
        String productSlug,
        String imageUrl,
        UUID variantId,
        String sku,
        String variantName,
        List<VariantSelectionResponse> selections,
        long quantity,
        boolean selected,
        BigDecimal unitPrice,
        BigDecimal originalUnitPrice,
        UUID promotionId,
        String promotionName,
        BigDecimal lineTotal,
        String currency,
        long availableQuantity,
        String stockStatus,
        boolean eligible,
        List<String> issues,
        Instant updatedAt) {}
