package com.shoppew.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID shopId,
        String shopName,
        String status,
        long itemCount,
        BigDecimal grandTotal,
        String currency,
        Instant placedAt,
        Instant updatedAt) {}
