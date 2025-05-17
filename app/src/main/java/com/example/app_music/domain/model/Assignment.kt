package com.example.app_music.domain.model

data class Assignment(
    val id: Long,
    val question: String,
    val answer: String,
    val vector: String? = null // vector có thể bỏ qua khi hiển thị
)
