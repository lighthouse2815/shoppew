package com.shoppew.product.dto;

import java.util.UUID;

public record ProductAttributeResponse(
        UUID attributeId,
        String name,
        String valueType,
        boolean required,
        String value) {}
