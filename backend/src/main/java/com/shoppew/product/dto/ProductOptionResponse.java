package com.shoppew.product.dto;

import java.util.List;
import java.util.UUID;

public record ProductOptionResponse(
        UUID id,
        String name,
        int sortOrder,
        List<OptionValueResponse> values) {}
