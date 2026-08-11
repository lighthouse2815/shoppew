package com.shoppew.address.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @Size(max = 80) String label,
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank @Pattern(regexp = "^[0-9+() .-]{8,32}$", message = "Số điện thoại không hợp lệ") String phone,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "Mã quốc gia phải dùng ISO alpha-2") String countryCode,
        @NotBlank @Size(max = 120) String province,
        @NotBlank @Size(max = 120) String district,
        @Size(max = 120) String ward,
        @NotBlank @Size(max = 255) String addressLine,
        @Size(max = 24) String postalCode,
        boolean defaultAddress) {}
