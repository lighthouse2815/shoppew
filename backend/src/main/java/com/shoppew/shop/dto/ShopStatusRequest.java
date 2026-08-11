package com.shoppew.shop.dto;

import com.shoppew.shop.entity.ShopStatus;
import jakarta.validation.constraints.NotNull;

public record ShopStatusRequest(@NotNull ShopStatus status) {}
