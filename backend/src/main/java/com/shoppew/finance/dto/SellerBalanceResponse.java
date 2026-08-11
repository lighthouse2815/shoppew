package com.shoppew.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerBalanceResponse(UUID shopId, String currency, BigDecimal pendingAmount,
        BigDecimal availableAmount, BigDecimal heldAmount, BigDecimal paidOutAmount, Instant updatedAt) {}
