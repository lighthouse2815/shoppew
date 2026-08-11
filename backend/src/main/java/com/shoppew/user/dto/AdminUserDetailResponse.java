package com.shoppew.user.dto;

import com.shoppew.shop.dto.ShopResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminUserDetailResponse(
        UUID id,
        String email,
        String phone,
        String displayName,
        String avatarUrl,
        String status,
        boolean emailVerified,
        List<String> roles,
        LocalDate dateOfBirth,
        String gender,
        String locale,
        long activeSessionCount,
        List<ShopResponse> shops,
        Instant createdAt,
        Instant updatedAt) {}
