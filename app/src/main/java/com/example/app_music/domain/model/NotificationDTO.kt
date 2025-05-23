package com.example.app_music.domain.model

import com.google.gson.annotations.SerializedName

data class NotificationDTO(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val notiDate: String = "",  // Keep exact field name from JSON
    val userId: Long = 0,
    val type: String = "notification",
    @SerializedName("read") val isRead: Boolean = false  // Use SerializedName for clarity
) {
    override fun toString(): String {
        return "NotificationDTO(id=$id, title='$title', content='$content', " +
                "notiDate='$notiDate', userId=$userId, type='$type', isRead=$isRead)"
    }
}