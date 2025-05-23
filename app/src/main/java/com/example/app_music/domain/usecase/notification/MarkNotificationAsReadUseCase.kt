package com.example.app_music.domain.usecase.notification


import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class MarkNotificationAsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: Long): Response<Unit> {
        return repository.markAsRead(notificationId)
    }
}