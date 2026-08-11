package com.shoppew.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OptionValueRequest(
        @NotBlank @Size(max = 120) String value,
        @Min(0) int sortOrder) {}
