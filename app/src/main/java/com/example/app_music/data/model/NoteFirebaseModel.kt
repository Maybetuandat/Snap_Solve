package com.example.app_music.data.model

import android.util.Log
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.google.gson.Gson
import java.util.Date

@IgnoreExtraProperties
data class NoteFirebaseModel(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "", // Firebase document ID

    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("content")
    @set:PropertyName("content")
    var content: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = Date().time,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Long = Date().time,

    @get:PropertyName("ownerId")
    @set:PropertyName("ownerId")
    var ownerId: String = "",

    @get:PropertyName("folderId")
    @set:PropertyName("folderId")
    var folderId: String = "",

    @get:PropertyName("imagePath")
    @set:PropertyName("imagePath")
    var imagePath: String? = null,

    @get:PropertyName("collaborators")
    @set:PropertyName("collaborators")
    var collaborators: List<String> = listOf(),

    @get:PropertyName("drawingData")
    @set:PropertyName("drawingData")
    var drawingData: String? = null, // Base64 encoded drawing data

    @get:PropertyName("pageIds")
    @set:PropertyName("pageIds")
    var pageIds: MutableList<String> = mutableListOf(),

    @get:PropertyName("vectorDrawingData")
    @set:PropertyName("vectorDrawingData")
    var vectorDrawingData: String? = null,

    @get:Exclude
    @set:Exclude
    var pages: List<NotePage> = emptyList()
)

@IgnoreExtraProperties
data class NotePage(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("noteId")
    @set:PropertyName("noteId")
    var noteId: String = "",

    @get:PropertyName("pageIndex")
    @set:PropertyName("pageIndex")
    var pageIndex: Int = 0,

    @get:PropertyName("imagePath")
    @set:PropertyName("imagePath")
    var imagePath: String? = null,

    @get:PropertyName("drawingData")
    @set:PropertyName("drawingData")
    var drawingData: String? = null,

    @get:PropertyName("vectorDrawingData")
    @set:PropertyName("vectorDrawingData")
    var vectorDrawingData: String? = null,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = Date().time
)

@IgnoreExtraProperties
data class FolderFirebaseModel(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "", // Firebase document ID
    
    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = Date().time,
    
    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Long = Date().time,
    
    @get:PropertyName("ownerId")
    @set:PropertyName("ownerId")
    var ownerId: String = "",
    
    @get:PropertyName("sharedWith")
    @set:PropertyName("sharedWith")
    var sharedWith: List<String> = listOf()
)

// Thêm vào lớp NoteFirebaseModel
@get:PropertyName("vectorDrawingData")
@set:PropertyName("vectorDrawingData")
var vectorDrawingData: String? = null // Lưu dữ liệu vector dưới dạng JSON

fun DrawingData.toJson(): String {
    return Gson().toJson(this)
}

fun String.toDrawingData(): DrawingData? {
    return try {
        Gson().fromJson(this, DrawingData::class.java)
    } catch (e: Exception) {
        Log.e("NoteModel", "Error converting JSON to DrawingData", e)
        null
    }
}