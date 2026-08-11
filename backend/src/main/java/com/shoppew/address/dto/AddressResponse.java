package com.shoppew.address.dto;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String recipientName,
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
