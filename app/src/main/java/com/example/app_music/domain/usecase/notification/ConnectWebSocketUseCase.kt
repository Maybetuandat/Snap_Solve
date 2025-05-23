package com.example.app_music.domain.usecase.notification

import com.example.app_music.domain.model.WebSocketMessage
import com.example.app_music.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.Flow

class ConnectWebSocketUseCase(
    private val repository: WebSocketRepository
) {
    operator fun invoke(userId: Long): Flow<WebSocketMessage> {
        return repository.connect(userId)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun isConnected(): Boolean {
        return repository.isConnected()
    }
}