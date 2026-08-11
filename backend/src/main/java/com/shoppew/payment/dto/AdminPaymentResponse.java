package com.shoppew.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPaymentResponse(
        UUID id,
        UUID checkoutGroupId,
        String checkoutNumber,
        UUID userId,
        String customerEmail,
        String provider,
        String providerReference,
        String status,
        BigDecimal amount,
        String currency,
        String failureCode,
        String failureMessage,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt) {}
