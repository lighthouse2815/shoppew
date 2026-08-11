package com.shoppew.shipping.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String provider,
        String methodCode,
        String methodName,
        String trackingNumber,
        String status,
        BigDecimal fee,
        String currency,
        LocalDate estimatedDeliveryFrom,
        LocalDate estimatedDeliveryTo,
        Instant shippedAt,
        Instant deliveredAt,
        List<TrackingResponse> tracking) {
    public record TrackingResponse(String status, String description, String location, Instant occurredAt) {}
}
