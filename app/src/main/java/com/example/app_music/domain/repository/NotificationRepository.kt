package com.example.app_music.domain.repository

import com.example.app_music.domain.model.Notification
import retrofit2.Response

interface NotificationRepository {
    suspend fun getNotificationsByUserId(userId: Long): Response<List<Notification>>
    suspend fun getUnreadNotificationCount(userId: Long): Response<Long>
    suspend fun markAllAsRead(userId: Long): Response<Unit>
    suspend fun markAsRead(notificationId: Long): Response<Unit>
    suspend fun deleteNotification(notificationId: Long): Response<Unit>
}