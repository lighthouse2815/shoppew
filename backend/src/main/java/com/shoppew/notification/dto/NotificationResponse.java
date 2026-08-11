package com.shoppew.notification.dto;

import com.shoppew.notification.entity.NotificationType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String title, String body,
        Map<String, Object> data, boolean read, Instant readAt, Instant createdAt) {}
