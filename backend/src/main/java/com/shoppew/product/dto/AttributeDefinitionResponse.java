package com.shoppew.product.dto;

import java.util.UUID;

public record AttributeDefinitionResponse(
        UUID id,
        UUID categoryId,
        String name,
        String valueType,
        boolean required,
        int sortOrder) {}
