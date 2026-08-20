package com.shoppew.notification.sender;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(prefix = "app.push", name = "delivery-enabled", havingValue = "false", matchIfMissing = true)
public class LocalPushNotificationSender implements PushNotificationSender {
    @Override
    public NotificationSendResult send(NotificationMessage message) {
        return NotificationSendResult.skipped(
                "Push delivery is disabled; set Firebase credentials and APP_PUSH_DELIVERY_ENABLED to enable it");
    }
}
