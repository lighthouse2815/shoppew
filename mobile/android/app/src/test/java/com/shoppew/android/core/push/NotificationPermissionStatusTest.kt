package com.shoppew.android.core.push

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionStatusTest {
    @Test
    fun `permission is treated as enabled before Android 13`() {
        assertEquals(
            NotificationPermissionStatus.NotRequired,
            resolveNotificationPermissionStatus(
                runtimePermissionRequired = false,
                permissionGranted = false,
                requestedBefore = false,
                shouldShowRationale = false,
            ),
        )
    }

    @Test
    fun `first visit offers contextual request without assuming a denial`() {
        assertEquals(
            NotificationPermissionStatus.ReadyToRequest,
            resolveNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = false,
                requestedBefore = false,
                shouldShowRationale = false,
            ),
        )
    }

    @Test
    fun `granted permission takes precedence over request history`() {
        assertEquals(
            NotificationPermissionStatus.Granted,
            resolveNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = true,
                requestedBefore = true,
                shouldShowRationale = false,
            ),
        )
    }

    @Test
    fun `denied permission can be requested again when Android shows rationale`() {
        assertEquals(
            NotificationPermissionStatus.Denied,
            resolveNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = false,
                requestedBefore = true,
                shouldShowRationale = true,
            ),
        )
    }

    @Test
    fun `previous request without rationale routes user to system settings`() {
        assertEquals(
            NotificationPermissionStatus.Blocked,
            resolveNotificationPermissionStatus(
                runtimePermissionRequired = true,
                permissionGranted = false,
                requestedBefore = true,
                shouldShowRationale = false,
            ),
        )
    }
}
