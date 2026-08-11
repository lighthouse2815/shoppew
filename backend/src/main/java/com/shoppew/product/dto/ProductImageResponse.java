package com.shoppew.product.dto;

import java.time.Instant;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        String altText,
        int sortOrder,
        boolean primary,
        Instant createdAt) {}
