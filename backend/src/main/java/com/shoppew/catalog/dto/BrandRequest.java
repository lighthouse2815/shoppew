package com.shoppew.catalog.dto;

import com.shoppew.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 180) String slug,
        @Size(max = 1000) @HttpUrl String logoUrl) {}
