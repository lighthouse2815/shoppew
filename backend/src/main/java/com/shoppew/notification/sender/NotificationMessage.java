package com.shoppew.notification.sender;

import java.util.Map;
import java.util.UUID;

public record NotificationMessage(
        UUID userId,
        String recipientEmail,
        String title,
        String body,
        Map<String, Object> data) {}
