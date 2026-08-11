package com.shoppew.notification.sender;

public interface EmailNotificationSender {
    NotificationSendResult send(NotificationMessage message);
}
