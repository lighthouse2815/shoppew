package com.shoppew.android.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shoppew.android.core.push.NotificationPermissionStatus
import com.shoppew.android.core.push.resolveNotificationPermissionStatus

private const val PERMISSION_PREFERENCES = "notification_permission"
private const val KEY_REQUESTED_BEFORE = "post_notifications_requested"

private data class NotificationPermissionCopy(
    val title: String,
    val message: String,
    val actionLabel: String? = null,
)

@Composable
internal fun NotificationPermissionSection() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val preferences = remember(context) {
        context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun currentStatus(): NotificationPermissionStatus = resolveNotificationPermissionStatus(
        runtimePermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        requestedBefore = preferences.getBoolean(KEY_REQUESTED_BEFORE, false),
        shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
        } == true,
    )

    var status by remember(context, activity) { mutableStateOf(currentStatus()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        status = currentStatus()
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = currentStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val copy = status.copy()
    val enabled = status == NotificationPermissionStatus.Granted || status == NotificationPermissionStatus.NotRequired

    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(copy.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        copy.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            copy.actionLabel?.let { label ->
                val onClick = {
                    if (status == NotificationPermissionStatus.Blocked) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        preferences.edit().putBoolean(KEY_REQUESTED_BEFORE, true).apply()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                if (status == NotificationPermissionStatus.Blocked) {
                    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(label)
                    }
                } else {
                    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(label)
                    }
                }
            }
        }
    }
}

private fun NotificationPermissionStatus.copy(): NotificationPermissionCopy = when (this) {
    NotificationPermissionStatus.NotRequired -> NotificationPermissionCopy(
        title = "Thông báo trên thiết bị: Đã bật",
        message = "Thiết bị này không yêu cầu cấp quyền thông báo riêng. Bạn vẫn có thể quản lý thông báo trong phần cài đặt hệ thống.",
    )
    NotificationPermissionStatus.Granted -> NotificationPermissionCopy(
        title = "Thông báo trên thiết bị: Đã bật",
        message = "shoppew có thể gửi cập nhật đơn hàng, thanh toán và tài khoản tới thiết bị này.",
    )
    NotificationPermissionStatus.ReadyToRequest -> NotificationPermissionCopy(
        title = "Thông báo trên thiết bị: Chưa bật",
        message = "Bật thông báo để nhận cập nhật quan trọng ngay cả khi bạn không mở ứng dụng. Bạn vẫn đọc được mọi thông báo trong màn hình này nếu chưa bật.",
        actionLabel = "Bật thông báo",
    )
    NotificationPermissionStatus.Denied -> NotificationPermissionCopy(
        title = "Thông báo trên thiết bị: Đã từ chối",
        message = "Bạn đã từ chối quyền trước đó. Có thể thử cấp lại quyền; thông báo trong ứng dụng vẫn hoạt động bình thường.",
        actionLabel = "Cấp lại quyền",
    )
    NotificationPermissionStatus.Blocked -> NotificationPermissionCopy(
        title = "Thông báo trên thiết bị: Đang bị chặn",
        message = "Android không cho ứng dụng hỏi lại. Mở cài đặt của shoppew để bật quyền khi bạn sẵn sàng; thông báo trong ứng dụng vẫn xem được tại đây.",
        actionLabel = "Mở cài đặt thông báo",
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
