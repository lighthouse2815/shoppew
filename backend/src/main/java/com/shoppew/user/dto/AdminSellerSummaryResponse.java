package com.shoppew.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminSellerSummaryResponse(
        UUID userId,
        String email,
        String phone,
        String displayName,
        String status,
        boolean emailVerified,
        long shopCount,
        long activeShopCount,
        Instant createdAt,
        Instant updatedAt) {}
