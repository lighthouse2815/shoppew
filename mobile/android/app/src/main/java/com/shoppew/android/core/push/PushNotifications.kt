package com.shoppew.android.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shoppew.android.MainActivity
import com.shoppew.android.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface PushInstallation {
    suspend fun current(): String?
    fun cached(): String?
}

@Singleton
class FirebasePushInstallation @Inject constructor(
    @ApplicationContext context: Context,
) : PushInstallation {
    private val preferences = context.getSharedPreferences("push_registration", Context.MODE_PRIVATE)

    override suspend fun current(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            val fid = task.takeIf { it.isSuccessful }?.result?.takeIf(String::isNotBlank)
            if (fid != null) preferences.edit().putString(KEY_FID, fid).apply()
            if (continuation.isActive) continuation.resume(fid ?: cached())
        }
    }

    override fun cached(): String? = preferences.getString(KEY_FID, null)

    private companion object {
        const val KEY_FID = "firebase_installation_id"
    }
}

@Singleton
class PushNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val routeResolver: PushRouteResolver,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            },
        )
    }

    fun show(message: RemoteMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel()
        val route = routeResolver.fromData(message.data) ?: AppRoute.Notifications
        val intent = Intent(Intent.ACTION_VIEW, routeResolver.toUri(route), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = message.notification?.title ?: message.data["title"] ?: context.getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: context.getString(R.string.notification_default_body)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shoppew)
            .setContentTitle(title.take(MAX_TEXT_LENGTH))
            .setContentText(body.take(MAX_TEXT_LENGTH))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(MAX_BIG_TEXT_LENGTH)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        manager.notify(message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "shoppew_updates"
        const val MAX_TEXT_LENGTH = 160
        const val MAX_BIG_TEXT_LENGTH = 500
    }
}

@AndroidEntryPoint
class ShoppewMessagingService : FirebaseMessagingService() {
    @Inject lateinit var notifications: PushNotificationManager

    override fun onMessageReceived(message: RemoteMessage) {
        notifications.show(message)
    }
}
