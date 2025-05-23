package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Notification
import retrofit2.Response
import retrofit2.http.*

interface NotificationApi {
    @GET("/api/notifications/user/{userId}")
    suspend fun getNotificationsByUserId(@Path("userId") userId: Long): Response<List<Notification>>

    @GET("/api/notifications/user/{userId}/unread")
    suspend fun getUnreadNotificationsByUserId(@Path("userId") userId: Long): Response<List<Notification>>

    @GET("/api/notifications/user/{userId}/count")
    suspend fun getUnreadNotificationCount(@Path("userId") userId: Long): Response<Long>

    @GET("/api/notifications/{id}")
    suspend fun getNotificationById(@Path("id") notificationId: Long): Response<Notification>

    @POST("/api/notifications/user/{userId}/mark-read")
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: Long): Response<Unit>

    @POST("/api/notifications/{id}/mark-read")
    suspend fun markNotificationAsRead(@Path("id") notificationId: Long): Response<Unit>

    @DELETE("/api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") notificationId: Long): Response<Unit>
}