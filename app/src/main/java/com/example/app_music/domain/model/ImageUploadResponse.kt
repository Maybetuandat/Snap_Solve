package com.example.app_music.domain.model

data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String?,
    val fileName: String?,
    val imageId: String?,
    val assignments: List<Assignment>?
)