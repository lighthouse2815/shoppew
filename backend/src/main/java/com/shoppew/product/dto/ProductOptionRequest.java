package com.shoppew.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductOptionRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(0) int sortOrder,
        @NotEmpty @Size(max = 30) List<@Valid OptionValueRequest> values) {}
