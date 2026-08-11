package com.shoppew.dispute.dto;

import com.shoppew.dispute.entity.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisputeUpdateRequest(@NotNull DisputeStatus status, @Size(max = 1000) String resolution) {}
