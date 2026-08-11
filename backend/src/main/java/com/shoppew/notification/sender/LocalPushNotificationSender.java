package com.shoppew.notification.sender;

import org.springframework.stereotype.Component;

@Component
public class LocalPushNotificationSender implements PushNotificationSender {
    @Override
    public NotificationSendResult send(NotificationMessage message) {
        return NotificationSendResult.skipped(
                "No push provider credentials configured; Android FCM adapter can replace this local sender");
    }
}
