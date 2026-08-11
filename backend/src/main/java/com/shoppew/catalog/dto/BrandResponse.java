package com.shoppew.catalog.dto;

import java.time.Instant;
import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug,
        String logoUrl,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
