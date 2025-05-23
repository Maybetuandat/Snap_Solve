package com.example.app_music.domain.repository

import com.example.app_music.domain.model.WebSocketMessage
import kotlinx.coroutines.flow.Flow

interface WebSocketRepository {
    fun connect(userId: Long): Flow<WebSocketMessage>
    fun disconnect()
    fun isConnected(): Boolean
    fun sendMessage(message: String)
}