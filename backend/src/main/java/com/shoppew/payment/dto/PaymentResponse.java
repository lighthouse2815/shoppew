package com.shoppew.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID checkoutGroupId,
        String provider,
        String providerReference,
        String status,
        BigDecimal amount,
        String currency,
        String action,
        String failureCode,
        String failureMessage,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt) {}
