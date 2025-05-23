package com.example.app_music.domain.usecase.notification

import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class MarkNotificationsAsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long): Response<Unit> {
        return repository.markAllAsRead(userId)
    }
}