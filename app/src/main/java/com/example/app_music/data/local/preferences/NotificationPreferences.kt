package com.example.app_music.data.local.preferences

import android.content.Context

object NotificationPreferences {

    private const val PREF_NAME = "notification_preferences"
    private const val UNREAD_COUNT = "unread_count"
    private const val LAST_NOTIFICATION_TIME = "last_notification_time"
    private const val NOTIFICATIONS_ENABLED = "notifications_enabled"

    fun saveUnreadCount(context: Context, count: Long): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putLong(UNREAD_COUNT, count)
        return editor.commit()
    }

    fun getUnreadCount(context: Context): Long {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getLong(UNREAD_COUNT, 0)
    }

    fun saveLastNotificationTime(context: Context, timestamp: Long): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putLong(LAST_NOTIFICATION_TIME, timestamp)
        return editor.commit()
    }

    fun getLastNotificationTime(context: Context): Long {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getLong(LAST_NOTIFICATION_TIME, 0)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean(NOTIFICATIONS_ENABLED, enabled)
        return editor.commit()
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean(NOTIFICATIONS_ENABLED, true)
    }

    fun clearNotificationData(context: Context) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
    }
}