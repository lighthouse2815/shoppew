package com.shoppew.catalog.dto;

import com.shoppew.common.validation.HttpUrl;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoryRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 180) String slug,
        UUID parentId,
        @Size(max = 5000) String description,
        @Size(max = 1000) @HttpUrl String imageUrl,
        @Min(0) int sortOrder) {}
