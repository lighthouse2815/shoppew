package com.shoppew.notification.dto;

import com.shoppew.notification.entity.PushDeviceEntity.Platform;
import com.shoppew.notification.entity.PushDeviceEntity.TargetType;
import java.time.Instant;
import java.util.UUID;

public record PushDeviceResponse(
        UUID id,
        Platform platform,
        TargetType targetType,
        boolean active,
        Instant lastSeenAt) {}
