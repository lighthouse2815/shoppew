package com.shoppew.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MockPaymentWebhookRequest(
        @NotBlank @Size(max = 180) String providerEventId,
        @NotBlank @Size(max = 160) String providerReference,
        @NotNull Boolean succeeded) {}
