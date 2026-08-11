package com.shoppew.shop.dto;

import java.time.Instant;
import java.util.UUID;

public record ShopSettingsResponse(
        UUID shopId,
        String currencyCode,
        String timeZone,
        int orderAutoCancelMinutes,
        int returnWindowDays,
        boolean chatEnabled,
        boolean vacationMode,
        Instant updatedAt) {}
