package com.example.app_music.data.repository

import com.example.app_music.data.remote.api.NotificationApi
import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class NotificationRepositoryImpl(
    private val notificationApi: NotificationApi
) : NotificationRepository {

    override suspend fun getNotificationsByUserId(userId: Long): Response<List<Notification>> {
        return notificationApi.getNotificationsByUserId(userId)
    }

    override suspend fun getUnreadNotificationCount(userId: Long): Response<Long> {
        return notificationApi.getUnreadNotificationCount(userId)
    }

    override suspend fun markAllAsRead(userId: Long): Response<Unit> {
        return notificationApi.markAllNotificationsAsRead(userId)
    }

    override suspend fun markAsRead(notificationId: Long): Response<Unit> {
        return notificationApi.markNotificationAsRead(notificationId)
    }

    override suspend fun deleteNotification(notificationId: Long): Response<Unit> {
        return notificationApi.deleteNotification(notificationId)
    }
}