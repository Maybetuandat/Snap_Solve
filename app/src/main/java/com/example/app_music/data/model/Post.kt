package com.example.app_music.data.model

import java.time.LocalDate

data class Post(
    val id: Long,
    val title: String,
    val content: String,
    val image: String?,
    val createDate: LocalDate,
    val user: User,
    val react: List<React> = emptyList(),
    val comment: List<Comment> = emptyList(),
    val topics: List<Topic> = emptyList()
) {
    val reactCount: Int
        get() = react.size

    val commentCount: Int
        get() = comment.size
}

data class React(
    val id: Long,
    val type: String,
    val createDate: LocalDate,
    val user: User
)

//data class Comment(
//    val id: Long,
//    val content: String,
//    val image: String?,
//    val createDate: LocalDate,
//    val user: User
//)

data class Topic(
    val id: Long,
    val name: String,
    val description: String?
)