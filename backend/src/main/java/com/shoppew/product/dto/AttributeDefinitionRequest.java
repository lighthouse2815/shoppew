package com.shoppew.product.dto;

import com.shoppew.product.entity.AttributeValueType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AttributeDefinitionRequest(
        UUID categoryId,
        @NotBlank @Size(max = 120) String name,
        @NotNull AttributeValueType valueType,
        boolean required,
        @Min(0) int sortOrder) {}
