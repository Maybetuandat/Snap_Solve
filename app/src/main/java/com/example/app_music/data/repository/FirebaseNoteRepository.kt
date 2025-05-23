package com.example.app_music.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.app_music.data.model.FolderFirebaseModel
import com.example.app_music.data.model.NoteFirebaseModel
import com.example.app_music.data.model.NotePage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID
import android.graphics.Canvas
import android.graphics.Color
import com.google.firebase.database.FirebaseDatabase

class FirebaseNoteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val foldersCollection = db.collection("folders")
    private val notesCollection = db.collection("notes")
    private val storageRef = storage.reference.child("notes")
    private val pagesCollection = db.collection("note_pages")


    suspend fun createFolder(title: String, userId: String): Result<FolderFirebaseModel> {
        if (userId.isEmpty()) {
            return Result.failure(Exception("Chua dang nhap"))
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

            val checkDoc = foldersCollection.document(folderId).get().await()
            if (checkDoc.exists()) {
                Result.success(folder)
            } else {
                Result.failure(Exception("Khong the tao thu muc"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Loi khi tao thu muc", e)
            Result.failure(e)
        }
    }

    suspend fun getFolders(userId: String): Result<List<FolderFirebaseModel>> {
        return try {

            //lấy ra các folder của user theo thứ tự từ mới nhất đến cũ nhất
            val snapshot = foldersCollection
                .whereEqualTo("ownerId", userId)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            //lặp qua và chuyển các đối tượng thành FolderFirebaseModel, loại bỏ null
            val folders = snapshot.documents.mapNotNull { doc ->
                try {
                    val folder = doc.toObject(FolderFirebaseModel::class.java)
                    folder?.apply { id = doc.id }
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(folders)
        } catch (e: Exception) {
            e.printStackTrace()
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
                
            val batch = db.batch() // gom nhiều thao tác thêm sửa xóa được vào đây
            notesSnapshot.documents.forEach { doc ->
                batch.delete(notesCollection.document(doc.id))
            }

            //batch xóa được nhiều lần
            batch.delete(foldersCollection.document(folderId))
            batch.commit().await()
            
            Result.success(true)
        } catch (e: Exception) {
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

            if (note == null) {
                return Result.failure(Exception("Note không tìm thấy"))
            }


            //lấy ra các pages của note
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

                if (pages.isNotEmpty()) {
                    val pageIds = pages.map { it.id }
                    if (note.pageIds != pageIds) {
                        note.pageIds = pageIds.toMutableList()
                        notesCollection.document(noteId).update("pageIds", pageIds).await()
                    }
                }

                // Thêm thông tin pages vào kết quả
                note.pages = pages

            } catch (e: Exception) {
            }

            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lấy note", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateNote(note: NoteFirebaseModel, newImageBitmap: Bitmap? = null): Result<NoteFirebaseModel> {
        return try {

            //thay ảnh mới cho note
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
            val noteId = note.id

            if (note.pageIds.isNotEmpty()) {
                try {
                    //trang đầu tiên
                    val firstPageId = note.pageIds.first()
                    val pageSnapshot = pagesCollection.document(firstPageId).get().await()
                    val page = pageSnapshot.toObject(NotePage::class.java)

                    //lấy ảnh của nó đặt làm ảnh nền cho note, ảnh đại diện
                    if (page != null && page.imagePath != null) {
                        note.imagePath = page.imagePath
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting first page image: ${e.message}")
                }
            }

            notesCollection.document(noteId).set(note).await()

            Result.success(note)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating note with ID ${note.id}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Boolean> {
        return try {
            val noteDoc = notesCollection.document(noteId).get().await()
            val note = noteDoc.toObject(NoteFirebaseModel::class.java)?.apply { id = noteDoc.id }

            if (note == null) {
                return Result.failure(Exception("Note không tồn tại"))
            }

            //Xóa các pages liên quan đến note
            val pagesSnapshot = pagesCollection
                .whereEqualTo("noteId", noteId)
                .get()
                .await()

            for (pageDoc in pagesSnapshot.documents) {
                val page = pageDoc.toObject(NotePage::class.java)
                if (page != null) {
                    //xóa các ảnh của page trong storage
                    if (page.imagePath != null) {
                        try {
                            storage.reference.child(page.imagePath!!).delete()
                                .addOnFailureListener { e ->
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
                    pagesCollection.document(pageDoc.id).delete().await()
                    Log.d(TAG, "Đã xóa page: ${pageDoc.id}")
                }
            }

            val storagePaths = listOf(
                "images/${noteId}.jpg",
                "thumbnails/${noteId}.jpg",
                "drawings/${noteId}.png"
            )

            //Xóa thêm các ảnh khác trong storage
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

            //Xóa note luôn ở cuối cùng
            notesCollection.document(noteId).delete().await()

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
            val noteResult = getNote(noteId)
            if (noteResult.isFailure) {
                return Result.failure(Exception("Note không tìm thấy"))
            }

            val note = noteResult.getOrNull()!!
            val pageId = UUID.randomUUID().toString()

            //lấy index của page
            val index = if (pageIndex < 0) {
                if (note.pages.isEmpty()) 0 else note.pages.size
            } else {
                pageIndex
            }

            val whiteBitmap = createWhiteBackground(800, 1200)

            val imagePath = "pages/$pageId.jpg"
            val imageRef = storage.reference.child(imagePath)
            val baos = ByteArrayOutputStream()
            whiteBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            imageRef.putBytes(baos.toByteArray()).await()

            val page = NotePage(
                id = pageId,
                noteId = noteId,
                pageIndex = index,
                imagePath = imagePath,
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

    suspend fun deletePage(pageId: String): Result<Boolean> {
        return try {
            val pageResult = getPage(pageId)
            if (pageResult.isFailure) {
                return Result.failure(Exception("Page not found"))
            }

            val page = pageResult.getOrNull()!!
            val noteId = page.noteId
            val pagePath = page.imagePath

            val noteResult = getNote(noteId)
            if (noteResult.isFailure) {
                return Result.failure(Exception("Note not found"))
            }

            val note = noteResult.getOrNull()!!
            val isFirstPage = note.pageIds.indexOf(pageId) == 0

            // Xóa page document từ Firestore
            pagesCollection.document(pageId).delete().await()

            // Xóa ảnh của page
            if (pagePath != null) {
                try {
                    storage.reference.child(pagePath).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting page image: ${e.message}")
                }
            }

            try {
                val strokesRef = FirebaseDatabase.getInstance().getReference("strokes").child(pageId)
                strokesRef.removeValue().await()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting strokes: ${e.message}")
            }

            // Cập nhật danh sách pageIds trong note
            note.pageIds.remove(pageId)

            // Cập nhật dữ liệu note
            notesCollection.document(noteId).update("pageIds", note.pageIds).await()
            Log.d(TAG, "Updated pageIds list in note")

            // Cập nhật lại index cho tất cả các trang còn lại
            val remainingPages = mutableListOf<NotePage>()

            // Tìm tất cả các trang còn lại của note này theo thứ tự pageIndex
            val pagesSnapshot = pagesCollection
                .whereEqualTo("noteId", noteId)
                .orderBy("pageIndex")
                .get()
                .await()

            for (pageDoc in pagesSnapshot.documents) {
                val currentPage = pageDoc.toObject(NotePage::class.java)?.apply { id = pageDoc.id }
                if (currentPage != null) {
                    remainingPages.add(currentPage)
                }
            }

            remainingPages.sortBy { it.pageIndex }

            // Cập nhật index cho tất cả trang
            for (i in remainingPages.indices) {
                val currentPage = remainingPages[i]

                if (currentPage.pageIndex != i) {
                    val updatedPage = currentPage.copy(pageIndex = i)
                    pagesCollection.document(currentPage.id).set(updatedPage).await()
                }
            }

            //Cập nhật ảnh đại diện của note
            if (isFirstPage && note.pageIds.isNotEmpty()) {
                val newFirstPageId = note.pageIds.first()
                val newFirstPageResult = getPage(newFirstPageId)

                if (newFirstPageResult.isSuccess) {
                    val newFirstPage = newFirstPageResult.getOrNull()!!

                    if (newFirstPage.imagePath != null) {
                        notesCollection.document(noteId).update("imagePath", newFirstPage.imagePath).await()
                    } else {
                        notesCollection.document(noteId).update("imagePath", null).await()
                    }
                }
            } else if (note.pageIds.isEmpty()) {
                notesCollection.document(noteId).update("imagePath", null).await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting page", e)
            Result.failure(e)
        }
    }

}