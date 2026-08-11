package com.shoppew.user.dto;

import com.shoppew.common.validation.HttpUrl;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 1000) @HttpUrl String avatarUrl,
        @Past LocalDate dateOfBirth,
        @Pattern(regexp = "FEMALE|MALE|NON_BINARY|UNDISCLOSED", message = "Giới tính không hợp lệ") String gender,
        @NotBlank @Pattern(regexp = "^[a-z]{2}(?:-[A-Z]{2})?$", message = "Locale không hợp lệ") String locale,
        @Pattern(regexp = "^$|^[0-9+() .-]{8,32}$", message = "Số điện thoại không hợp lệ") String phone) {}
