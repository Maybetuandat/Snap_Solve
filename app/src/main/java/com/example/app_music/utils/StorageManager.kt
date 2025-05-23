package com.example.app_music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.example.app_music.data.repository.FirebaseNoteRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class StorageManager(private val context: Context) {
    
    private val TAG = "StorageManager"
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    // đường dẫn của các file trong storage
    private val THUMBNAILS_PATH = "thumbnails"
    private val IMAGES_PATH = "images"
    private val PAGES_PATH = "pages"
    private val PAGE_THUMBNAILS_PATH = "page_thumbnails"

    suspend fun savePageImage(pageId: String, image: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storageRef.child("$PAGES_PATH/$pageId.jpg")

                // chuyển thành byte array
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val data = baos.toByteArray()

                // Upload to Firebase
                imageRef.putBytes(data).await()

                // save luôn 1 cái thumbnail cho page luôn
                val thumbnailBitmap = createThumbnail(image)
                savePageThumbnail(pageId, thumbnailBitmap)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page image for page $pageId", e)
                false
            }
        }
    }

    //tạo thumbnail 200px từ ảnh ban đầu mà vẫn giữ tỷ lệ
    private fun createThumbnail(original: Bitmap): Bitmap {
        try {
            val width = original.width
            val height = original.height
            val maxSize = 200 // kicks thuoc

            val scale = Math.min(
                maxSize.toFloat() / width.toFloat(),
                maxSize.toFloat() / height.toFloat()
            )

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            return Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
        } catch (e: Exception) {
            return if (original.width > 200 || original.height > 200) {
                try {
                    Bitmap.createScaledBitmap(original, 200, 200, true)
                } catch (e2: Exception) {
                    original
                }
            } else {
                original
            }
        }
    }

    private suspend fun savePageThumbnail(pageId: String, thumbnail: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnailRef = storageRef.child("$PAGE_THUMBNAILS_PATH/$pageId.jpg")

                // chuyển thành byte array
                val baos = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val data = baos.toByteArray()

                // Upload to Firebase
                thumbnailRef.putBytes(data).await()

                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page thumbnail for page $pageId", e)
                false
            }
        }
    }

    suspend fun loadPageImage(pageId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val cachedFile = File(context.cacheDir, "pages/$pageId.jpg")
                if (cachedFile.exists()) {
                    Log.d(TAG, "Xóa ảnh cache cũ cho trang: $pageId")
                    cachedFile.delete()
                }

                // Tạo đường dẫn chính xác cho ảnh
                val imageRef = storage.reference.child("pages/$pageId.jpg")

                // Tạo file tạm để lưu ảnh
                val localFile = File.createTempFile("page", "jpg")

                // Download ảnh
                val downloadTask = imageRef.getFile(localFile)

                // Thêm timeout để tránh treo
                val result = withTimeout(5000) {
                    downloadTask.await()
                    true
                }

                // Kiểm tra tải thành công
                if (result) {
                    // Giải mã bitmap
                    val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

                    // Xóa file tạm
                    localFile.delete()

                    if (bitmap != null) {
                        Log.d(TAG, "Tải ảnh thành công cho trang: $pageId, kích thước: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e(TAG, "Tải ảnh thất bại cho trang: $pageId (bitmap null)")
                    }

                    bitmap
                } else {
                    Log.e(TAG, "Tải ảnh thất bại cho trang: $pageId (timeout)")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi tải ảnh cho trang: $pageId", e)
                null
            }
        }
    }

    suspend fun saveThumbnail(noteId: String, thumbnail: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving thumbnail for note: $noteId")
                val thumbnailRef = storageRef.child("$THUMBNAILS_PATH/$noteId.jpg")

                // Convert bitmap to byte array
                val baos = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val data = baos.toByteArray()

                // Upload to Firebase
                val uploadTask = thumbnailRef.putBytes(data).await()

                // Also save a local copy for faster access
                val localSaved = saveThumbnailLocally(noteId, thumbnail)

                Log.d(TAG, "Thumbnail saved to Firebase: success. Local save: $localSaved")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thumbnail for note $noteId: ${e.message}", e)
                false
            }
        }
    }

    fun clearImageCache(noteId: String) {
        // Clear both thumbnail and full image from cache
        deleteThumbnailLocally(noteId)
        deleteImageLocally(noteId)
    }

    suspend fun saveImage(noteId: String, image: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storageRef.child("$IMAGES_PATH/$noteId.jpg")

                // Chuyển bitmap thành byte array
                val baos = ByteArrayOutputStream()

                // Giảm chất lượng nếu kích thước quá lớn
                val quality = if (image.width * image.height > 1000000) 85 else 95
                image.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val data = baos.toByteArray()

                Log.d(TAG, "Uploading image for note $noteId, size: ${data.size} bytes")

                // Upload lên Firebase với timeout
                withTimeout(30000) { // 30 giây timeout
                    imageRef.putBytes(data).await()
                }

                // Lưu bản sao local
                saveImageLocally(noteId, image)

                Log.d(TAG, "Uploaded image for note $noteId successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image for note $noteId: ${e.message}", e)
                false
            }
        }
    }

    private fun loadCachedImage(filename: String): Bitmap? {
        try {
            val cacheDir = File(context.cacheDir, "images")
            val file = File(cacheDir, filename)

            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cached image", e)
        }
        return null
    }

    private fun saveCachedImage(filename: String, bitmap: Bitmap): Boolean {
        try {
            val cacheDir = File(context.cacheDir, "images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val file = File(cacheDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cached image", e)
            return false
        }
    }

    private fun saveThumbnailLocally(noteId: String, thumbnail: Bitmap): Boolean {
        return try {
            val thumbnailDir = File(context.cacheDir, THUMBNAILS_PATH)
            if (!thumbnailDir.exists()) {
                thumbnailDir.mkdirs()
            }
            
            val file = File(thumbnailDir, "$noteId.jpg")
            FileOutputStream(file).use { out ->
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thumbnail to local cache", e)
            false
        }
    }
    
    private fun loadThumbnailLocally(noteId: String): Bitmap? {
        return try {
            val thumbnailDir = File(context.cacheDir, THUMBNAILS_PATH)
            val file = File(thumbnailDir, "$noteId.jpg")
            
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading thumbnail from local cache", e)
            null
        }
    }
    
    fun deleteThumbnailLocally(noteId: String): Boolean {
        return try {
            val thumbnailDir = File(context.cacheDir, THUMBNAILS_PATH)
            val file = File(thumbnailDir, "$noteId.jpg")
            
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting thumbnail from local cache", e)
            false
        }
    }
    
    private fun saveImageLocally(noteId: String, image: Bitmap): Boolean {
        return try {
            val imageDir = File(context.cacheDir, IMAGES_PATH)
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }
            
            val file = File(imageDir, "$noteId.jpg")
            FileOutputStream(file).use { out ->
                image.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to local cache", e)
            false
        }
    }
    
    private fun loadImageLocally(noteId: String): Bitmap? {
        return try {
            val imageDir = File(context.cacheDir, IMAGES_PATH)
            val file = File(imageDir, "$noteId.jpg")
            
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from local cache", e)
            null
        }
    }
    
    fun deleteImageLocally(noteId: String): Boolean {
        return try {
            val imageDir = File(context.cacheDir, IMAGES_PATH)
            val file = File(imageDir, "$noteId.jpg")
            
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image from local cache", e)
            false
        }
    }
}