package com.example.app_music.domain.model

data class WebSocketMessage(
    val type: String = "",
    val notification: Notification? = null,
    val unreadCount: Long = 0,
    val userId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

enum class WebSocketMessageType {
    NOTIFICATION,
    UNREAD_COUNT,
    CONNECTION_STATUS
}