package com.shoppew.catalog.dto;

import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
        UUID id,
        String name,
        String slug,
        String imageUrl,
        List<CategoryTreeResponse> children) {}
