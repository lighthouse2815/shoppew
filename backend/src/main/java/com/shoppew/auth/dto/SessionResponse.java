package com.shoppew.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String deviceName,
        String userAgent,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean current) {}
