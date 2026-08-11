package com.shoppew.shop.dto;

import com.shoppew.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateShopRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug không hợp lệ") @Size(max = 180) String slug,
        @Size(max = 5000) String description,
        @Size(max = 1000) @HttpUrl String logoUrl,
        @Size(max = 1000) @HttpUrl String bannerUrl) {}
