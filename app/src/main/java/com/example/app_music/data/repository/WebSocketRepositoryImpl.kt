package com.example.app_music.data.repository

import com.example.app_music.data.remote.websocket.WebSocketClientImpl
import com.example.app_music.domain.model.WebSocketMessage
import com.example.app_music.domain.repository.WebSocketRepository
import com.example.app_music.domain.utils.ApiInstance
import kotlinx.coroutines.flow.Flow
import java.net.URI

class WebSocketRepositoryImpl : WebSocketRepository {

    private var webSocketClient: WebSocketClientImpl? = null

    override fun connect(userId: Long): Flow<WebSocketMessage> {
        val serverUrl = ApiInstance.baseUrl.replace("http", "ws")

        // Create new WebSocket client if not exists
        if (webSocketClient == null) {
            try {
                val uri = URI("$serverUrl/ws")
                webSocketClient = WebSocketClientImpl(uri)
            } catch (e: Exception) {
                throw RuntimeException("Failed to create WebSocket client: ${e.message}")
            }
        }

        return webSocketClient!!.connectAndListen(userId, serverUrl)
    }

    override fun disconnect() {
        webSocketClient?.disconnect()
        webSocketClient = null
    }

    override fun isConnected(): Boolean {
        return webSocketClient?.isConnected() ?: false
    }

    override fun sendMessage(message: String) {
        webSocketClient?.send(message)
    }
}