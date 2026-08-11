package com.shoppew.voucher.dto;

import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.voucher.entity.DiscountType;
import com.shoppew.voucher.entity.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record VoucherRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$") String code,
        @NotBlank @Size(max = 160) String name,
        @NotNull VoucherType voucherType,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0.01") BigDecimal discountValue,
        @DecimalMin("0.01") BigDecimal maxDiscount,
        @NotNull @DecimalMin("0.00") BigDecimal minimumSpend,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Min(1) long totalQuantity,
        @Min(1) int perUserLimit,
        @Size(max = 200) Set<UUID> productIds,
        @Size(max = 100) Set<UUID> categoryIds,
        @Size(max = 10) Set<PaymentProviderType> paymentProviders) {}
