package com.menswear.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MenswearApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Promotions & Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Receive updates about new arrivals, sales, and order status."
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "menswear_default"
    }
}
