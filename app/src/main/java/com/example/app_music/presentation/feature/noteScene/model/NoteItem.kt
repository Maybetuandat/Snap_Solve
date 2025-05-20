package com.example.app_music.presentation.feature.noteScene.model

import android.graphics.Bitmap

class NoteItem(
    val id: String,
    var title: String,
    val date: String,
    val isFolder: Boolean = false,
    var isExpanded: Boolean = false,
    var imagePreview: Bitmap? = null
) {
    fun hasImage(): Boolean = imagePreview != null
}