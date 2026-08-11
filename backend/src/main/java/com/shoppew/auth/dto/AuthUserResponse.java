package com.shoppew.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String displayName,
        Set<String> roles,
        String status,
        boolean emailVerified) {}
