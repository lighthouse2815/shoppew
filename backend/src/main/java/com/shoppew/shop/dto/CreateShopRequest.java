package com.shoppew.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShopRequest(
        @NotBlank @Size(max = 160) String name,
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug không hợp lệ") @Size(max = 180) String slug,
        @Size(max = 5000) String description) {}
