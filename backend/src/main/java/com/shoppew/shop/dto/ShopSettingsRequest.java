package com.shoppew.shop.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShopSettingsRequest(
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currencyCode,
        @NotBlank @Size(max = 64) String timeZone,
        @Min(15) @Max(10080) int orderAutoCancelMinutes,
        @Min(0) @Max(90) int returnWindowDays,
        boolean chatEnabled,
        boolean vacationMode) {}
