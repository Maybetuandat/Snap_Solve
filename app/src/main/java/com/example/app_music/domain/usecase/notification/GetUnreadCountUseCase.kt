package com.example.app_music.domain.usecase.notification

import com.example.app_music.domain.repository.NotificationRepository
import retrofit2.Response

class GetUnreadCountUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(userId: Long): Response<Long> {
        return repository.getUnreadNotificationCount(userId)
    }
}