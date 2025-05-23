package com.example.app_music.domain.model

import java.time.LocalDate

data class Post(
    val id: Long,
    val title: String,
    val content: String,
    val image: String?,
    val additionalImages: List<String> = emptyList(),
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

    fun getAllImages(): List<String> {
        val allImages = mutableListOf<String>()
        image?.let { allImages.add(it) }
        allImages.addAll(additionalImages)
        return allImages
    }
}

data class React(
    val id: Long,
    val type: String,
    val createDate: LocalDate,
    val user: User
)


data class Topic(
    val id: Long,
    val name: String,
    val description: String?
)