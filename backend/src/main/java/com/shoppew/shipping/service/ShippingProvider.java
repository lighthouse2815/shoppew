package com.shoppew.shipping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface ShippingProvider {
    String provider();
    String methodCode();
    ShippingQuote quote(ShippingQuoteRequest request);

    record ShippingQuoteRequest(UUID shopId, long totalWeightGrams, BigDecimal itemsSubtotal, String province) {}
    record ShippingQuote(BigDecimal fee, String currency, LocalDate estimatedFrom, LocalDate estimatedTo) {}
}
