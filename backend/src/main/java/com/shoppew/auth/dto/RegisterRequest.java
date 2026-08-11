package com.shoppew.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Pattern(regexp = "^$|^[0-9+() .-]{8,32}$", message = "Số điện thoại không hợp lệ") String phone,
        @Size(max = 160) String deviceName) {}
