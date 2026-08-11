package com.shoppew.checkout.dto;

import com.shoppew.order.dto.OrderSummaryResponse;
import com.shoppew.payment.dto.PaymentResponse;
import com.shoppew.voucher.dto.VoucherApplicationResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
        UUID id,
        String checkoutNumber,
        String status,
        BigDecimal itemsSubtotal,
        BigDecimal shippingTotal,
        BigDecimal discountTotal,
        BigDecimal grandTotal,
        String currency,
        List<OrderSummaryResponse> orders,
        PaymentResponse payment,
        List<VoucherApplicationResponse> appliedVouchers,
        Instant createdAt) {}
