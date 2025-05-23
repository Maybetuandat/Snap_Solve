package com.example.app_music.domain.usecase.notification

import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class GetNotificationsUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long): Response<List<Notification>> {
        return repository.getNotificationsByUserId(userId)
    }
}