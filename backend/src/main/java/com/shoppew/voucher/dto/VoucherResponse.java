package com.shoppew.voucher.dto;

import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.voucher.entity.DiscountType;
import com.shoppew.voucher.entity.VoucherOwnerType;
import com.shoppew.voucher.entity.VoucherStatus;
import com.shoppew.voucher.entity.VoucherType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record VoucherResponse(
        UUID id, VoucherOwnerType ownerType, UUID shopId, String code, String name,
        VoucherType voucherType, DiscountType discountType, BigDecimal discountValue,
        BigDecimal maxDiscount, BigDecimal minimumSpend, String currency, Instant startsAt,
        Instant endsAt, long totalQuantity, long usedQuantity, int perUserLimit, VoucherStatus status,
        Set<UUID> productIds, Set<UUID> categoryIds, Set<PaymentProviderType> paymentProviders,
        Instant createdAt, Instant updatedAt) {}
