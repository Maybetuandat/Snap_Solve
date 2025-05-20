package com.example.app_music.domain.model

data class TextSearchResponse(
    val success: Boolean,
    val message: String,
    val query: String?,
    val assignments: List<Assignment>?
)