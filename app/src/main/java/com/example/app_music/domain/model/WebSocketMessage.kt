package com.example.app_music.domain.model

/**
 * This class is used as a wrapper for different types of WebSocket messages
 * It can contain either a notification or an unread count update
 */
data class WebSocketMessage(
    val type: String,  // Type of message: "notification" or "unread_count"
    val notification: NotificationDTO? = null,
    val unreadCount: Long? = null
) {
    override fun toString(): String {
        return "WebSocketMessage(type='$type', notification=$notification, unreadCount=$unreadCount)"
    }
}