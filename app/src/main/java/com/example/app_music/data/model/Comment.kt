package com.example.app_music.data.model

import java.time.LocalDate

data class Comment(
    val id: Long,
    val content: String,
    val image: String?,
    val createDate: LocalDate,
    val user: User
) {

    fun getTimeAgo(): String {
        val now = LocalDate.now()
        val days = java.time.Period.between(createDate, now).days

        return when {
            days == 0 -> "Today"
            days == 1 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days / 7} weeks ago"
            days < 365 -> "${days / 30} months ago"
            else -> "${days / 365} years ago"
        }
    }
}