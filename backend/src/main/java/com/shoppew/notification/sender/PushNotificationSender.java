package com.shoppew.notification.sender;

public interface PushNotificationSender {
    NotificationSendResult send(NotificationMessage message);
}
