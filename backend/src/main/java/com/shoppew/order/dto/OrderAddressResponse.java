package com.shoppew.order.dto;

public record OrderAddressResponse(
        String recipientName,
        String phone,
        String countryCode,
        String province,
        String district,
        String ward,
        String addressLine,
        String postalCode) {}
