package com.example.app_music.presentation.noteScene.model

import android.graphics.Bitmap

class NoteItem(
    val id: String,
    var title: String,
    val date: String,
    val isFolder: Boolean = false,
    var isExpanded: Boolean = false,
    val imagePreview: Bitmap? = null
) {
    private val childNotes: MutableList<NoteItem> = mutableListOf()

    // Constructor for image note
    constructor(id: String, title: String, date: String, imagePreview: Bitmap) :
            this(id, title, date, false, false, imagePreview)

    fun getChildNotes(): List<NoteItem> = childNotes

    fun addChildNote(note: NoteItem) {
        if (isFolder) {
            childNotes.add(note)
        }
    }

    fun hasImage(): Boolean = imagePreview != null
}