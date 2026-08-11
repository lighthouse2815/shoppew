package com.shoppew.shipping.service;

import com.shoppew.common.config.AppProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class MockShippingProvider implements ShippingProvider {

    private final AppProperties properties;
    private final Clock clock;

    public MockShippingProvider(AppProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public String provider() { return "MOCK"; }

    @Override
    public String methodCode() { return "MOCK_STANDARD"; }

    @Override
    public ShippingQuote quote(ShippingQuoteRequest request) {
        long kilograms = Math.max(1, (request.totalWeightGrams() + 999) / 1000);
        BigDecimal fee = new BigDecimal("22000.00")
                .add(BigDecimal.valueOf(Math.max(0, kilograms - 1)).multiply(new BigDecimal("3000.00")));
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(properties.timeZone())));
        return new ShippingQuote(fee, properties.currency(), today.plusDays(2), today.plusDays(5));
    }
}
