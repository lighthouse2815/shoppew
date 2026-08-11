package com.shoppew.user.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String email,
        String phone,
        String displayName,
        String avatarUrl,
        LocalDate dateOfBirth,
        String gender,
        String locale,
        Set<String> roles,
        String status,
        boolean emailVerified) {}
