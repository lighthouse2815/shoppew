package com.shoppew.checkout.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CheckoutShopQuoteResponse(
        UUID shopId,
        String shopName,
        List<UUID> cartItemIds,
        BigDecimal itemsSubtotal,
        BigDecimal shippingFee,
        BigDecimal discountTotal,
        BigDecimal grandTotal,
        LocalDate estimatedDeliveryFrom,
        LocalDate estimatedDeliveryTo) {}
