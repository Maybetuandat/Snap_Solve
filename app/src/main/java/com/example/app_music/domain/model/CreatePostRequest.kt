package com.example.app_music.domain.model

data class CreatePostRequest(
    val title: String,
    val content: String,
    val userId: Long,
    val topicIds: List<Long>,
    val images: List<String>? = null
)