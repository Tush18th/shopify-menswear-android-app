package com.menswear.app.service

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.menswear.app.MenswearApp
import com.menswear.app.R
import com.menswear.app.ui.MainActivity

/**
 * Firebase Cloud Messaging service.
 *
 * Handles incoming push notifications and FCM token refresh.
 * Deep link data can be included in notification payload:
 *   - "product_handle" -> opens product screen
 *   - "collection_handle" -> opens collection screen
 *   - "url" -> opens arbitrary deep link
 */
class MenswearFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        // In production, send this token to your backend or Shopify metafield
        // For MVP, we just log it
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received from: ${message.from}")

        val title = message.notification?.title ?: message.data["title"] ?: "Menswear"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        // Build deep link intent from payload
        val deepLinkUri = buildDeepLinkUri(message.data)
        showNotification(title, body, deepLinkUri)
    }

    private fun buildDeepLinkUri(data: Map<String, String>): Uri? {
        return when {
            data.containsKey("product_handle") ->
                Uri.parse("menswear://product/${data["product_handle"]}")
            data.containsKey("collection_handle") ->
                Uri.parse("menswear://collection/${data["collection_handle"]}")
            data.containsKey("url") ->
                Uri.parse(data["url"])
            else -> null
        }
    }

    private fun showNotification(title: String, body: String, deepLinkUri: Uri?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            deepLinkUri?.let { this.data = it }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MenswearApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }

    companion object {
        private const val TAG = "MenswearFCM"
    }
}
