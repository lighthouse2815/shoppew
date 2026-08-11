package com.shoppew.order.dto;

import com.shoppew.payment.dto.PaymentResponse;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID userId,
        String customerEmail,
        OrderDetailResponse order,
        PaymentResponse payment) {}
