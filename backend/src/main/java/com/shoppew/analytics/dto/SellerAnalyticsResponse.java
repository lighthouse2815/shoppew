package com.shoppew.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SellerAnalyticsResponse(UUID shopId, Instant from, Instant to, BigDecimal revenue,
        long completedOrders, BigDecimal averageOrderValue, List<TopProduct> topProducts, List<LowStock> lowStock) {
    public record TopProduct(UUID productId, String productName, long quantity, BigDecimal revenue) {}
    public record LowStock(UUID productId, UUID variantId, String productName, String variantName,
            String sku, long availableQuantity, long threshold) {}
}
