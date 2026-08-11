package com.shoppew.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        String sku,
        String imageUrl,
        BigDecimal unitPrice,
        long quantity,
        BigDecimal subtotal,
        String currency) {}
