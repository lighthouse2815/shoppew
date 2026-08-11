package com.shoppew.product.dto;

import java.util.UUID;

public record VariantSelectionResponse(
        UUID optionId,
        String optionName,
        UUID valueId,
        String value) {}
