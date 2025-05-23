package com.example.app_music.domain.repository

import com.example.app_music.domain.model.Notification
import retrofit2.Response

interface NotificationRepository {
    suspend fun getUnreadCount(userId: Long): Response<Long>
    suspend fun getNotificationsByUserId(userId: Long): Response<List<Notification>>
    suspend fun markAllAsRead(userId: Long): Response<Unit>
    suspend fun markAsRead(notificationId: Long): Response<Unit>
    suspend fun deleteNotification(notificationId: Long): Response<Unit>
    suspend fun getNotificationById(notificationId: Long): Response<Notification>
}