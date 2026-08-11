package com.shoppew.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID parentId,
        String name,
        String slug,
        String description,
        String imageUrl,
        int sortOrder,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
