package com.shoppew.voucher.dto;

import com.shoppew.voucher.entity.VoucherType;
import java.math.BigDecimal;
import java.util.UUID;

public record VoucherApplicationResponse(
        UUID voucherId, String code, String name, VoucherType type,
        BigDecimal discountAmount, String currency) {}
