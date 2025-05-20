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
import android.graphics.Canvas
import android.graphics.Color
import com.example.app_music.data.local.preferences.UserPreference

class FirebaseNoteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val foldersCollection = db.collection("folders")
    private val notesCollection = db.collection("notes")
    private val storageRef = storage.reference.child("notes")
    private val pagesCollection = db.collection("note_pages")




    suspend fun createFolder(title: String, userId: String): Result<FolderFirebaseModel> {
        // Kiểm tra người dùng đã đăng nhập
        if (userId.isEmpty()) {
            return Result.failure(Exception("Người dùng chưa đăng nhập"))
        }

        return try {
            val folderId = UUID.randomUUID().toString()
            val folder = FolderFirebaseModel(
                id = folderId,
                title = title,
                createdAt = Date().time,
                updatedAt = Date().time,
                ownerId = userId
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

    suspend fun getFolders(userId: String): Result<List<FolderFirebaseModel>> {
        return try {
            val snapshot = foldersCollection
                .whereEqualTo("ownerId", userId)
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
            // Lấy note
            val doc = notesCollection.document(noteId).get().await()
            val note = doc.toObject(NoteFirebaseModel::class.java)?.apply {
                id = doc.id
            }

            if (note == null) {
                return Result.failure(Exception("Note không tìm thấy"))
            }

            // Lấy các pages của note luôn
            try {
                val pagesSnapshot = pagesCollection
                    .whereEqualTo("noteId", noteId)
                    .orderBy("pageIndex", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val pages = pagesSnapshot.documents.mapNotNull { pageDoc ->
                    pageDoc.toObject(NotePage::class.java)?.apply {
                        id = pageDoc.id
                    }
                }

                // Cập nhật pageIds trong note để đảm bảo đồng bộ
                if (pages.isNotEmpty()) {
                    val pageIds = pages.map { it.id }
                    if (note.pageIds != pageIds) {
                        // Cập nhật pageIds trong Firestore nếu không khớp
                        note.pageIds = pageIds.toMutableList()
                        notesCollection.document(noteId).update("pageIds", pageIds).await()
                        Log.d(TAG, "Đã cập nhật pageIds cho note: $noteId")
                    }
                }

                // Thêm thông tin pages vào kết quả
                note.pages = pages

                Log.d(TAG, "Đã lấy ${pages.size} trang cho note: $noteId")
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi lấy các trang cho note: $noteId", e)
                // Tiếp tục và trả về note ngay cả khi không lấy được pages
            }

            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy note", e)
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

            // Kiểm tra xem note có trang nào không
            if (note.pageIds.isNotEmpty()) {
                try {
                    // Lấy trang đầu tiên
                    val firstPageId = note.pageIds.first()
                    val pageSnapshot = pagesCollection.document(firstPageId).get().await()
                    val page = pageSnapshot.toObject(NotePage::class.java)

                    // Nếu trang có ảnh, đặt đường dẫn ảnh vào note.imagePath
                    if (page != null && page.imagePath != null) {
                        // Sử dụng đường dẫn ảnh của trang đầu tiên làm imagePath của note
                        // Điều này sẽ giúp sử dụng ảnh đầu tiên làm thumbnail
                        note.imagePath = page.imagePath
                        Log.d(TAG, "Using first page image as thumbnail: ${page.imagePath}")
                    }
                } catch (e: Exception) {
                    // Chỉ log lỗi mà không làm gián đoạn quá trình tạo note
                    Log.e(TAG, "Error getting first page image: ${e.message}")
                }
            }

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

    // Trong FirebaseNoteRepository.kt
    suspend fun deleteNote(noteId: String): Result<Boolean> {
        return try {
            // Lấy thông tin note
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(NoteFirebaseModel::class.java)?.apply { id = noteDoc.id }

            if (note == null) {
                return Result.failure(Exception("Note không tồn tại"))
            }

            // 1. Xóa tất cả các pages
            val pagesSnapshot = pagesCollection
                .whereEqualTo("noteId", noteId)
                .get()
                .await()

            for (pageDoc in pagesSnapshot.documents) {
                val page = pageDoc.toObject(NotePage::class.java)
                if (page != null) {
                    // Xóa ảnh page nếu có
                    if (page.imagePath != null) {
                        try {
                            storage.reference.child(page.imagePath!!).delete()
                                .addOnFailureListener { e ->
                                    // Chỉ log lỗi, không dừng quá trình
                                    if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                        Log.w(TAG, "Ảnh không tồn tại, bỏ qua: ${page.imagePath}")
                                    } else {
                                        Log.e(TAG, "Lỗi xóa ảnh: ${e.message}")
                                    }
                                }
                        } catch (e: Exception) {
                            Log.w(TAG, "Lỗi khi xóa ảnh page: ${e.message}")
                        }
                    }

                    // Xóa page document không phụ thuộc vào việc xóa ảnh
                    pagesCollection.document(pageDoc.id).delete().await()
                    Log.d(TAG, "Đã xóa page: ${pageDoc.id}")
                }
            }

            // 2. Xóa các tài nguyên khác của note
            val storagePaths = listOf(
                "images/${noteId}.jpg",
                "thumbnails/${noteId}.jpg",
                "drawings/${noteId}.png"
            )

            for (path in storagePaths) {
                try {
                    storage.reference.child(path).delete()
                        .addOnSuccessListener {
                            Log.d(TAG, "Đã xóa: $path")
                        }
                        .addOnFailureListener { e ->
                            if (e is StorageException && e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                Log.w(TAG, "Tệp không tồn tại, bỏ qua: $path")
                            } else {
                                Log.e(TAG, "Lỗi xóa tệp: ${e.message}")
                            }
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Lỗi khi xóa tệp: $path - ${e.message}")
                }
            }

            // 3. Cuối cùng xóa document note
            notesCollection.document(noteId).delete().await()
            Log.d(TAG, "Đã xóa document note: $noteId")

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tổng thể khi xóa note: ${e.message}")
            Result.failure(e)
        }
    }


    companion object {
        private const val TAG = "FirebaseNoteRepository"
    }

    suspend fun createBlankPage(noteId: String, pageIndex: Int = -1): Result<NotePage> {
        return try {
            // Lấy note trước
            val noteResult = getNote(noteId)
            if (noteResult.isFailure) {
                return Result.failure(Exception("Note không tìm thấy"))
            }

            val note = noteResult.getOrNull()!!

            // Tạo ID page mới
            val pageId = UUID.randomUUID().toString()

            // Xác định index
            val index = if (pageIndex < 0) {
                if (note.pages.isEmpty()) 0 else note.pages.size
            } else {
                pageIndex
            }

            // Tạo bitmap trắng để làm nền
            val whiteBitmap = createWhiteBackground(800, 1200)

            // Lưu bitmap trắng vào storage
            val imagePath = "pages/$pageId.jpg"
            val imageRef = storage.reference.child(imagePath)
            val baos = ByteArrayOutputStream()
            whiteBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            imageRef.putBytes(baos.toByteArray()).await()

            // Tạo page với đường dẫn ảnh nền trắng
            val page = NotePage(
                id = pageId,
                noteId = noteId,
                pageIndex = index,
                imagePath = imagePath, // Quan trọng: đường dẫn đến ảnh nền trắng
                createdAt = Date().time,
                vectorDrawingData = """{"strokes":[],"width":800,"height":1200}"""
            )

            // Lưu page vào Firestore
            pagesCollection.document(pageId).set(page).await()

            // Cập nhật pageIds
            note.pageIds.add(pageId)
            notesCollection.document(noteId).update("pageIds", note.pageIds).await()

            Result.success(page)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo trang trắng", e)
            Result.failure(e)
        }
    }



    private fun createWhiteBackground(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        return bitmap
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

    // Thay thế phương thức deletePage hiện có bằng phương thức này
    suspend fun deletePage(pageId: String): Result<Boolean> {
        return try {
            // First find the page to get the note ID
            val pageResult = getPage(pageId)
            if (pageResult.isFailure) {
                return Result.failure(Exception("Page not found"))
            }

            val page = pageResult.getOrNull()!!
            val noteId = page.noteId
            val pagePath = page.imagePath

            // Get the note
            val noteResult = getNote(noteId)
            if (noteResult.isSuccess) {
                val note = noteResult.getOrNull()!!

                // Check if the deleted page is the first page
                val isFirstPage = note.pageIds.indexOf(pageId) == 0

                // Remove the page ID from the list
                note.pageIds.remove(pageId)

                // Delete the page document
                pagesCollection.document(pageId).delete().await()

                // Delete page image if exists
                if (pagePath != null) {
                    try {
                        storage.reference.child(pagePath).delete().await()
                        Log.d(TAG, "Deleted page image: $pagePath")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting page image: ${e.message}")
                    }
                }

                // Update note's page list
                notesCollection.document(noteId).update("pageIds", note.pageIds).await()

                // If the first page was deleted and there are other pages
                if (isFirstPage && note.pageIds.isNotEmpty()) {
                    // Get the new first page
                    val newFirstPageId = note.pageIds.first()
                    val newFirstPageResult = getPage(newFirstPageId)

                    if (newFirstPageResult.isSuccess) {
                        val newFirstPage = newFirstPageResult.getOrNull()!!

                        // Update note's imagePath to use the new first page as thumbnail
                        if (newFirstPage.imagePath != null) {
                            notesCollection.document(noteId).update("imagePath", newFirstPage.imagePath).await()
                            Log.d(TAG, "Updated note thumbnail to: ${newFirstPage.imagePath}")
                        } else {
                            // If new first page has no image, remove imagePath
                            notesCollection.document(noteId).update("imagePath", null).await()
                            Log.d(TAG, "Removed thumbnail as new first page has no image")
                        }
                    }
                } else if (note.pageIds.isEmpty()) {
                    // If no pages left, remove imagePath
                    notesCollection.document(noteId).update("imagePath", null).await()
                    Log.d(TAG, "Removed thumbnail as no pages remain")
                }

                Result.success(true)
            } else {
                Result.failure(Exception("Could not get note"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting page", e)
            Result.failure(e)
        }
    }

}