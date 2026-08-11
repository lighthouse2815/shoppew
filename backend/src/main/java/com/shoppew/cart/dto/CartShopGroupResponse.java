package com.shoppew.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartShopGroupResponse(
        UUID shopId,
        String shopName,
        String shopSlug,
        String shopLogoUrl,
        List<CartItemResponse> items,
        BigDecimal selectedSubtotal,
        boolean eligible) {}
