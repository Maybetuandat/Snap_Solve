package com.example.app_music.domain.model

import java.time.LocalDate

data class Comment(
    val id: Long,
    val content: String,
    val images: List<String> = emptyList(),
    val createDate: LocalDate,
    val user: User,
    val replyCount: Int, // Nhận trực tiếp từ backend
    val parentComment: Comment? = null,
    val replies: List<Comment> = emptyList()
) {
    fun getTimeAgo(): String {
        val now = LocalDate.now()
        val days = java.time.Period.between(createDate, now).days

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

data class CreateCommentRequest(
    val content: String,
    val images: List<String>? = null,
    val userId: Long,
    val postId: Long,
    val parentCommentId: Long? = null
)