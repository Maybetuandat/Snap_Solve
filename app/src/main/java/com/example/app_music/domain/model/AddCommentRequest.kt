package com.example.app_music.domain.model

data class AddCommentRequest(
    val content: String,
    val image: String? = null,
    val userId: Long? = null
)
