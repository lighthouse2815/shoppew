package com.shoppew.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        List<CartShopGroupResponse> shops,
        long itemCount,
        long selectedItemCount,
        BigDecimal selectedSubtotal,
        String currency,
        Instant revalidatedAt) {}
