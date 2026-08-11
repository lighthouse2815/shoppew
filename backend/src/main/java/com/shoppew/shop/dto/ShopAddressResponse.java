package com.shoppew.shop.dto;

import java.time.Instant;
import java.util.UUID;

public record ShopAddressResponse(
        UUID id,
        String type,
        String contactName,
        String phone,
        String countryCode,
        String province,
        String district,
        String ward,
        String addressLine,
        String postalCode,
        boolean defaultAddress,
        Instant createdAt,
        Instant updatedAt) {}
