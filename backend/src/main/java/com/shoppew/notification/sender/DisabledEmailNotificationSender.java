package com.shoppew.notification.sender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.email.delivery-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DisabledEmailNotificationSender implements EmailNotificationSender {
    @Override
    public NotificationSendResult send(NotificationMessage message) {
        return NotificationSendResult.skipped("Email delivery is disabled by configuration");
    }
}
