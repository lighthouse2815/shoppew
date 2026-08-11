package com.shoppew.refund.dto;

import com.shoppew.refund.entity.RefundReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record RefundCreateRequest(@NotNull UUID orderId, @NotNull RefundReason reason,
        @Size(max = 5000) String description,
        @NotNull @Size(min = 1, max = 50) List<@Valid Item> items) {
    public record Item(@NotNull UUID orderItemId, @Positive long quantity) {}
}
