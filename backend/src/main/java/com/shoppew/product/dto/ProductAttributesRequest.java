package com.shoppew.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductAttributesRequest(
        @NotNull @Size(max = 100) List<@Valid AttributeValueInput> values) {}
