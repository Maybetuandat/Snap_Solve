package com.example.app_music.utils
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.app_music.MainActivity
import com.example.app_music.R
import com.example.app_music.data.local.preferences.NotificationPreferences
import com.example.app_music.domain.model.Notification

class NotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "snapsolve_notifications"
        private const val CHANNEL_NAME = "SnapSolve Notifications"
        private const val NOTIFICATION_ID_BASE = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from SnapSolve app"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(notification: Notification) {
        // Check if notifications are enabled
        if (!NotificationPreferences.areNotificationsEnabled(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
            putExtra("notification_id", notification.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.content)
            )

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID_BASE + notification.id.toInt(), notificationBuilder.build())
            }

            // Save notification timestamp
            NotificationPreferences.saveLastNotificationTime(context, System.currentTimeMillis())

        } catch (e: SecurityException) {
            android.util.Log.e("NotificationManager", "Notification permission not granted")
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }
    }

    fun clearAllNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }

    fun clearNotification(notificationId: Long) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_BASE + notificationId.toInt())
    }
}