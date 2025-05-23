package com.example.app_music.domain.usecase.notification


import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class GetNotificationByIdUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: Long): Response<Notification> {
        return repository.getNotificationById(notificationId)
    }
}