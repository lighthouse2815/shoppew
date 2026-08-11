package com.shoppew.android

import android.app.Application
import com.shoppew.android.core.push.PushNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShoppewApplication : Application() {
    @Inject lateinit var pushNotifications: PushNotificationManager

    override fun onCreate() {
        super.onCreate()
        pushNotifications.createChannel()
    }
}
