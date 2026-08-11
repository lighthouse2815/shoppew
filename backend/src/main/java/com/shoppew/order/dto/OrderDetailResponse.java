package com.shoppew.order.dto;

import com.shoppew.shipping.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
        UUID id,
        String orderNumber,
        UUID checkoutGroupId,
        UUID shopId,
        String shopName,
        String shopSlug,
        String status,
        BigDecimal itemsSubtotal,
        BigDecimal shippingTotal,
        BigDecimal shopDiscountTotal,
        BigDecimal platformDiscountTotal,
        BigDecimal grandTotal,
        String currency,
        String customerNote,
        OrderAddressResponse address,
        List<OrderItemResponse> items,
        List<OrderHistoryResponse> history,
        ShipmentResponse shipment,
        Instant placedAt,
        Instant paidAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant updatedAt) {}
