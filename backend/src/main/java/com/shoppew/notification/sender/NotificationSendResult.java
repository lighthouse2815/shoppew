package com.shoppew.notification.sender;

public record NotificationSendResult(
        Status status,
        String providerReference,
        String message) {

    public enum Status { DELIVERED, SKIPPED }

    public static NotificationSendResult delivered(String providerReference) {
        return new NotificationSendResult(Status.DELIVERED, providerReference, null);
    }

    public static NotificationSendResult skipped(String message) {
        return new NotificationSendResult(Status.SKIPPED, null, message);
    }
}
