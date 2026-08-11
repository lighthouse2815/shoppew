package com.shoppew.order.dto;

import jakarta.validation.constraints.Size;

public record OrderActionRequest(
        @Size(max = 500) String reason,
        @Size(max = 160) String trackingNumber,
        @Size(max = 255) String location) {}
