package com.example.app_music.domain.model

data class SearchHistory(
    val id: Long,
    val question: String,
    val image: String?,
    val createDate: String,
    val assignmentId1: Long?,
    val assignmentId2: Long?,
    val assignmentId3: Long?,
    val assignmentId4: Long?,
    val assignmentId5: Long?
)