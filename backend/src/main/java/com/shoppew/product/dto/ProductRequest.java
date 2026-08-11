package com.shoppew.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID categoryId,
        UUID brandId,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 280) String slug,
        @Size(max = 500) String shortDescription,
        @NotBlank @Size(max = 50_000) String description) {}
