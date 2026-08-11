package com.shoppew.finance.dto;

import com.shoppew.finance.entity.BalanceBucket;
import com.shoppew.finance.entity.SellerTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SellerTransactionResponse(UUID id, SellerTransactionType transactionType, BigDecimal amount,
        String currency, BalanceBucket balanceBucket, UUID orderId, UUID refundId,
        String referenceKey, String description, Instant createdAt) {}
