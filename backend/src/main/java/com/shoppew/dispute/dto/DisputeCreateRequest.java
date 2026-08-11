package com.shoppew.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DisputeCreateRequest(@NotNull UUID orderId, UUID refundRequestId,
        @NotBlank @Size(max = 80) String reason, @NotBlank @Size(max = 5000) String description) {}
