package com.example.app_music.data.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.app_music.data.model.FolderFirebaseModel
import com.example.app_music.data.model.NoteFirebaseModel
import com.example.app_music.data.model.NotePage
import com.example.app_music.utils.StorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import android.content.Context

class FirebaseNoteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val foldersCollection = db.collection("folders")
    private val notesCollection = db.collection("notes")
    private val storageRef = storage.reference.child("notes")
    private val pagesCollection = db.collection("note_pages")


    // Current user ID
    private val currentUserId: String
        get() = "test_user_1"


    suspend fun createFolder(title: String): Result<FolderFirebaseModel> {
        // Kiểm tra người dùng đã đăng nhập
        if (currentUserId.isEmpty()) {
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            val folderId = UUID.randomUUID().toString()
            val folder = FolderFirebaseModel(
                id = folderId,
                title = title,
                createdAt = Date().time,
                updatedAt = Date().time,
                ownerId = currentUserId
            )

            foldersCollection.document(folderId).set(folder).await()

            // Kiểm tra xem folder có được tạo thành công không
            val checkDoc = foldersCollection.document(folderId).get().await()
            if (checkDoc.exists()) {
                Result.success(folder)
            } else {
                Result.failure(Exception("Không thể tạo thư mục trên máy chủ"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo thư mục", e)
            Result.failure(e)
        }
    }

    suspend fun getFolders(): Result<List<FolderFirebaseModel>> {
        return try {
            val snapshot = foldersCollection
                .whereEqualTo("ownerId", "test_user_1")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val folders = snapshot.documents.mapNotNull { doc ->
                try {
                    val folder = doc.toObject(FolderFirebaseModel::class.java)
                    folder?.apply { id = doc.id }
                } catch (e: Exception) {
                    Log.e("Firebase", "Lỗi chuyển đổi tài liệu ${doc.id}: ${e.message}")
                    null
                }
            }

            Result.success(folders)
        } catch (e: Exception) {
            Log.e("Firebase", "Lỗi trong getFolders(): ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace() // In stack trace đầy đủ
            Result.failure(e)
        }
    }
    
    suspend fun updateFolder(folder: FolderFirebaseModel): Result<FolderFirebaseModel> {
        return try {
            folder.updatedAt = Date().time
            foldersCollection.document(folder.id).set(folder).await()
            Result.success(folder)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating folder", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteFolder(folderId: String): Result<Boolean> {
        return try {
            val notesSnapshot = notesCollection
                .whereEqualTo("folderId", folderId)
                .get()
                .await()
                
            val batch = db.batch()
            notesSnapshot.documents.forEach { doc ->
                batch.delete(notesCollection.document(doc.id))
            }

            batch.delete(foldersCollection.document(folderId))
            batch.commit().await()
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting folder", e)
            Result.failure(e)
        }
    }

    suspend fun createNote(title: String, folderId: String, imageBitmap: Bitmap? = null): Result<NoteFirebaseModel> {
        return try {
            val noteId = UUID.randomUUID().toString()
            var imagePath: String? = null

            if (imageBitmap != null) {
                val baos = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageRef = storageRef.child("$noteId.jpg")
                imageRef.putBytes(baos.toByteArray()).await()
                imagePath = imageRef.path
            }
            
            val note = NoteFirebaseModel(
                id = noteId,
                title = title,
                createdAt = Date().time,
                updatedAt = Date().time,
                ownerId = currentUserId,
                folderId = folderId,
                imagePath = imagePath
            )
            
            notesCollection.document(noteId).set(note).await()
            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating note", e)
            Result.failure(e)
        }
    }

    suspend fun getNotes(folderId: String): Result<List<NoteFirebaseModel>> {
        return try {
            val snapshot = notesCollection
                .whereEqualTo("folderId", folderId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NoteFirebaseModel::class.java)?.apply {
                    id = doc.id
                }
            }

            Result.success(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notes", e)
            Result.failure(e)
        }
    }

    suspend fun getNote(noteId: String): Result<NoteFirebaseModel> {
        return try {
            val doc = notesCollection.document(noteId).get().await()
            val note = doc.toObject(NoteFirebaseModel::class.java)?.apply {
                id = doc.id
            }

            if (note != null) {
                Result.success(note)
            } else {
                Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting note", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateNote(note: NoteFirebaseModel, newImageBitmap: Bitmap? = null): Result<NoteFirebaseModel> {
        return try {
            // Update image if provided
            if (newImageBitmap != null) {
                val baos = ByteArrayOutputStream()
                newImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageRef = storageRef.child("${note.id}.jpg")
                imageRef.putBytes(baos.toByteArray()).await()
                note.imagePath = imageRef.path
            }
            
            note.updatedAt = Date().time
            notesCollection.document(note.id).set(note).await()
            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating note", e)
            Result.failure(e)
        }
    }

    suspend fun createNoteWithId(note: NoteFirebaseModel): Result<NoteFirebaseModel> {
        return try {
            // Sử dụng ID đã có từ tham số
            val noteId = note.id

            // Lưu note với ID đã có
            notesCollection.document(noteId).set(note).await()

            // Log để debug
            Log.d(TAG, "Note created with ID: $noteId, imagePath: ${note.imagePath}")

            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating note with ID ${note.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Boolean> {
        return try {
            // First get the note to check if it has an image
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(NoteFirebaseModel::class.java)?.apply { id = noteDoc.id }

            // Delete image if exists
            if (note?.imagePath != null && note.imagePath!!.isNotEmpty()) {
                try {
                    // Xử lý đường dẫn - đảm bảo đúng định dạng
                    val path = if (!note.imagePath!!.startsWith("images/") && !note.imagePath!!.startsWith("thumbnails/")) {
                        "images/${note.imagePath}"
                    } else {
                        note.imagePath!!
                    }

                    Log.d(TAG, "Đang xóa ảnh từ path: $path")

                    // Thử xóa ảnh chính
                    try {
                        storage.reference.child(path).delete().await()
                        Log.d(TAG, "Đã xóa ảnh chính thành công")
                    } catch (se: StorageException) {
                        if (se.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w(TAG, "Ảnh chính không tồn tại, bỏ qua: $path")
                        } else {
                            Log.e(TAG, "Lỗi khi xóa ảnh chính: ${se.message}")
                        }
                    }

                    // Thử xóa thumbnail nếu có
                    try {
                        val thumbnailPath = "thumbnails/$noteId.jpg"
                        storage.reference.child(thumbnailPath).delete().await()
                        Log.d(TAG, "Đã xóa thumbnail thành công")
                    } catch (se: StorageException) {
                        if (se.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w(TAG, "Thumbnail không tồn tại, bỏ qua")
                        } else {
                            Log.e(TAG, "Lỗi khi xóa thumbnail: ${se.message}")
                        }
                    }
                } catch (e: Exception) {
                    // Bắt tất cả các ngoại lệ khác khi xóa ảnh, ghi log nhưng vẫn tiếp tục
                    Log.e(TAG, "Lỗi tổng thể khi xóa ảnh: ${e.message}")
                }
            }

            // Delete note document - luôn thực hiện bước này bất kể có lỗi ở trên
            try {
                notesCollection.document(noteId).delete().await()
                Log.d(TAG, "Đã xóa note thành công: $noteId")
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi xóa document note: ${e.message}")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tổng thể khi xóa note: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun shareNoteWithUser(noteId: String, userEmail: String): Result<Boolean> {
        return try {
            // First find the user by email
            val userQuerySnapshot = db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .await()
                
            if (userQuerySnapshot.documents.isEmpty()) {
                return Result.failure(Exception("User not found"))
            }
            
            val userId = userQuerySnapshot.documents[0].id
            
            // Add user to collaborators
            val noteRef = notesCollection.document(noteId)
            val note = noteRef.get().await().toObject(NoteFirebaseModel::class.java)
            
            if (note != null) {
                val collaborators = note.collaborators.toMutableList()
                if (!collaborators.contains(userId)) {
                    collaborators.add(userId)
                    noteRef.update("collaborators", collaborators).await()
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Note not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing note", e)
            Result.failure(e)
        }
    }

    suspend fun getImageBitmap(imagePath: String): Result<Uri> {
        return try {
            // Đầu tiên kiểm tra xem đường dẫn đã có "images/" chưa
            val fullPath = if (!imagePath.startsWith("images/")) {
                "images/$imagePath"
            } else {
                imagePath
            }

            Log.d(TAG, "Đang tải ảnh từ: $fullPath")
            val imageRef = storage.reference.child(fullPath)
            val uri = imageRef.downloadUrl.await()
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tải ảnh từ đường dẫn: $imagePath", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "FirebaseNoteRepository"
    }

    suspend fun createPage(noteId: String, imageUri: Uri? = null, pageIndex: Int = -1): Result<NotePage> {
        return try {
            // Get the note first
            val noteResult = getNote(noteId)
            if (noteResult.isFailure) {
                return Result.failure(Exception("Note not found"))
            }

            val note = noteResult.getOrNull()!!

            // Generate a new page ID
            val pageId = UUID.randomUUID().toString()

            // Determine page index (add to end if not specified)
            val index = if (pageIndex < 0) note.pageIds.size else pageIndex

            // Upload image if provided
            var imagePath: String? = null
            if (imageUri != null) {
                val imageName = "$pageId.jpg"
                val imageRef = storageRef.child("pages/$imageName")

                // Upload the image
                imageRef.putFile(imageUri).await()

                // Get the storage path
                imagePath = "pages/$imageName"
            }

            // Create the page
            val page = NotePage(
                id = pageId,
                noteId = noteId,
                pageIndex = index,
                imagePath = imagePath,
                createdAt = Date().time
            )

            // Add the page to Firestore
            pagesCollection.document(pageId).set(page).await()

            // Update the note's page list
            note.pageIds.add(pageId)
            notesCollection.document(noteId).update("pageIds", note.pageIds).await()

            Result.success(page)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating page", e)
            Result.failure(e)
        }
    }


    suspend fun createBlankPage(noteId: String, pageIndex: Int = -1): Result<NotePage> {
        return createPage(noteId, null, pageIndex)
    }

    suspend fun getPages(noteId: String): Result<List<NotePage>> {
        return try {
            val snapshot = pagesCollection
                .whereEqualTo("noteId", noteId)
                .orderBy("pageIndex")
                .get()
                .await()

            val pages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(NotePage::class.java)?.apply {
                    id = doc.id
                }
            }

            Result.success(pages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pages", e)
            Result.failure(e)
        }
    }

    suspend fun getPage(pageId: String): Result<NotePage> {
        return try {
            val doc = pagesCollection.document(pageId).get().await()
            val page = doc.toObject(NotePage::class.java)?.apply {
                id = doc.id
            }

            if (page != null) {
                Result.success(page)
            } else {
                Result.failure(Exception("Page not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting page", e)
            Result.failure(e)
        }
    }

    suspend fun updatePage(page: NotePage): Result<NotePage> {
        return try {
            pagesCollection.document(page.id).set(page).await()
            Result.success(page)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating page", e)
            Result.failure(e)
        }
    }

    suspend fun deletePage(pageId: String): Result<Boolean> {
        return try {
            // First find the page to get the note ID
            val pageResult = getPage(pageId)
            if (pageResult.isFailure) {
                return Result.failure(Exception("Page not found"))
            }

            val page = pageResult.getOrNull()!!
            val noteId = page.noteId

            // Delete the page document
            pagesCollection.document(pageId).delete().await()

            // Update the note's page list
            val noteResult = getNote(noteId)
            if (noteResult.isSuccess) {
                val note = noteResult.getOrNull()!!
                note.pageIds.remove(pageId)
                notesCollection.document(noteId).update("pageIds", note.pageIds).await()
            }

            // Delete image if exists
            page.imagePath?.let { path ->
                storage.reference.child(path).delete().await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting page", e)
            Result.failure(e)
        }
    }

    suspend fun reorderPages(noteId: String, pageIds: List<String>): Result<Boolean> {
        return try {
            // Get the note
            val noteResult = getNote(noteId)
            if (noteResult.isFailure) {
                return Result.failure(Exception("Note not found"))
            }

            val note = noteResult.getOrNull()!!

            // Verify that all provided page IDs belong to this note
            if (!note.pageIds.containsAll(pageIds)) {
                return Result.failure(Exception("Invalid page IDs"))
            }

            // Update the note's page list
            note.pageIds.clear()
            note.pageIds.addAll(pageIds)
            notesCollection.document(noteId).update("pageIds", note.pageIds).await()

            // Update page indices
            val batch = db.batch()
            for ((index, pageId) in pageIds.withIndex()) {
                val pageRef = pagesCollection.document(pageId)
                batch.update(pageRef, "pageIndex", index)
            }
            batch.commit().await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error reordering pages", e)
            Result.failure(e)
        }
    }
}