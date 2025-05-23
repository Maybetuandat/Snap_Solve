package com.example.app_music.domain.model

import java.time.LocalDate

data class Notification(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val notiDate: LocalDate = LocalDate.now(),
    val userId: Long = 0,
    val isRead: Boolean = false
) {
    fun getTimeAgo(): String {
        val now = LocalDate.now()
        val days = java.time.Period.between(notiDate, now).days

        return when {
            days == 0 -> "Hôm nay"
            days == 1 -> "Hôm qua"
            days < 7 -> "$days ngày trước"
            days < 30 -> "${days / 7} tuần trước"
            days < 365 -> "${days / 30} tháng trước"
            else -> "${days / 365} năm trước"
        }
    }
}