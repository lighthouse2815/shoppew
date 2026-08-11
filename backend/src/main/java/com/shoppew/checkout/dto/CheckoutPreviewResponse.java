package com.shoppew.checkout.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.shoppew.voucher.dto.VoucherApplicationResponse;

public record CheckoutPreviewResponse(
        List<CheckoutShopQuoteResponse> shops,
        BigDecimal itemsSubtotal,
        BigDecimal shippingTotal,
        BigDecimal discountTotal,
        BigDecimal grandTotal,
        String currency,
        String paymentProvider,
        String shippingMethodCode,
        List<VoucherApplicationResponse> appliedVouchers,
        Instant calculatedAt) {}
