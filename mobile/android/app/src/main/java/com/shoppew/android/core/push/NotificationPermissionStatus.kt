package com.shoppew.android.core.push

internal enum class NotificationPermissionStatus {
    NotRequired,
    Granted,
    ReadyToRequest,
    Denied,
    Blocked,
}

internal fun resolveNotificationPermissionStatus(
    runtimePermissionRequired: Boolean,
    permissionGranted: Boolean,
    requestedBefore: Boolean,
    shouldShowRationale: Boolean,
): NotificationPermissionStatus = when {
    !runtimePermissionRequired -> NotificationPermissionStatus.NotRequired
    permissionGranted -> NotificationPermissionStatus.Granted
    !requestedBefore -> NotificationPermissionStatus.ReadyToRequest
    shouldShowRationale -> NotificationPermissionStatus.Denied
    else -> NotificationPermissionStatus.Blocked
}
