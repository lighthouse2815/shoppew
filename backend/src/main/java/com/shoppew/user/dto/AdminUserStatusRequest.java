package com.shoppew.user.dto;

import com.shoppew.user.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserStatusRequest(
        @NotNull UserStatus status,
        @NotBlank @Size(min = 3, max = 500) String reason) {}
