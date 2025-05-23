package com.example.app_music.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Notification(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val notiDate: String = "",
    val userId: Long = 0,
    val isRead: Boolean = false,
    val type: String = "notification"
) {
    fun getTimeAgo(): String {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val date = LocalDate.parse(notiDate, formatter)
            val now = LocalDate.now()
            val days = ChronoUnit.DAYS.between(date, now)

            return when {
                days == 0L -> "Hôm nay"
                days == 1L -> "Hôm qua"
                days < 7L -> "$days ngày trước"
                days < 30L -> "${days / 7} tuần trước"
                days < 365L -> "${days / 30} tháng trước"
                else -> "${days / 365} năm trước"
            }
        } catch (e: Exception) {
            return notiDate
        }
    }
}