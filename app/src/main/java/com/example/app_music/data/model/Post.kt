package com.example.app_music.data.model

import java.time.LocalDate

data class Post(
    val id: Long,
    val title: String,
    val content: String,
    val image: String?,
    val createDate: LocalDate,
    val user: User,
    val reactCount: Int = 0,
    val commentCount: Int = 0,
    val topics: List<Topic> = emptyList()
)

data class Topic(
    val id: Long,
    val name: String,
    val description: String?
)