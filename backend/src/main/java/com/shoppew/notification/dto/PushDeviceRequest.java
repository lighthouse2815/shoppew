package com.shoppew.notification.dto;

import com.shoppew.notification.entity.PushDeviceEntity.Platform;
import com.shoppew.notification.entity.PushDeviceEntity.TargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PushDeviceRequest(
        @NotNull Platform platform,
        @NotNull TargetType targetType,
        @NotBlank @Size(min = 10, max = 512) String target) {}
