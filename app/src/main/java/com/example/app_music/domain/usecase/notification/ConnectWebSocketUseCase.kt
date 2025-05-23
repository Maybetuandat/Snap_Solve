package com.example.app_music.domain.usecase.notification

import com.example.app_music.data.remote.websocket.WebSocket
import com.example.app_music.domain.model.WebSocketMessage
import kotlinx.coroutines.flow.Flow

class ConnectWebSocketUseCase(
    private val socket: WebSocket
) {
    operator fun invoke(userId: Long): Flow<WebSocketMessage> {
        return socket.connect(userId)
    }

    fun disconnect() {
        socket.disconnect()
    }

    fun isConnected(): Boolean {
        return socket.isConnected()
    }
}