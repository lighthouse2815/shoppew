package com.shoppew.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserSummaryResponse(
        UUID id,
        String email,
        String phone,
        String displayName,
        String avatarUrl,
        String status,
        boolean emailVerified,
        List<String> roles,
        Instant createdAt,
        Instant updatedAt) {}
