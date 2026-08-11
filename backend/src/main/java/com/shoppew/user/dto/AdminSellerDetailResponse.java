package com.shoppew.user.dto;

import com.shoppew.shop.dto.ShopResponse;
import java.util.List;

public record AdminSellerDetailResponse(
        AdminUserDetailResponse seller,
        List<ShopResponse> shops) {}
