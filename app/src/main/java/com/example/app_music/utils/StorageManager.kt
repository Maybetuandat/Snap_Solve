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

/**
 * Utility class for managing storage of note thumbnails and images using Firebase Storage
 */
class StorageManager(private val context: Context) {
    
    private val TAG = "StorageManager"
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    // Path constants
    private val THUMBNAILS_PATH = "thumbnails"
    private val IMAGES_PATH = "images"
    private val DRAWINGS_PATH = "drawings"
    private val PAGES_PATH = "pages"
    private val PAGE_THUMBNAILS_PATH = "page_thumbnails"
    /**
     * Save a thumbnail to Firebase Storage
     */
    suspend fun savePageImage(pageId: String, image: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storageRef.child("$PAGES_PATH/$pageId.jpg")

                // Convert bitmap to byte array
                val baos = ByteArrayOutputStream()
                image.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val data = baos.toByteArray()

                // Upload to Firebase
                imageRef.putBytes(data).await()

                // Also save a thumbnail
                    val thumbnailBitmap = createThumbnail(image)
                savePageThumbnail(pageId, thumbnailBitmap)

                Log.d(TAG, "Saved page image for page $pageId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page image for page $pageId", e)
                false
            }
        }
    }
    /**
     * Create a thumbnail from a full-size image
     */
    private fun createThumbnail(original: Bitmap): Bitmap {
        try {
            val width = original.width
            val height = original.height
            val maxSize = 200 // Thumbnail size

            val scale = Math.min(
                maxSize.toFloat() / width.toFloat(),
                maxSize.toFloat() / height.toFloat()
            )

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            return Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail: ${e.message}")
            // Return a smaller version of the original if we can't create a proper thumbnail
            return if (original.width > 200 || original.height > 200) {
                try {
                    Bitmap.createScaledBitmap(original, 200, 200, true)
                } catch (e2: Exception) {
                    original // Last resort, return original
                }
            } else {
                original
            }
        }
    }
    /**
     * Save a page thumbnail
     */
    private suspend fun savePageThumbnail(pageId: String, thumbnail: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnailRef = storageRef.child("$PAGE_THUMBNAILS_PATH/$pageId.jpg")

                // Convert bitmap to byte array
                val baos = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val data = baos.toByteArray()

                // Upload to Firebase
                thumbnailRef.putBytes(data).await()

                Log.d(TAG, "Saved page thumbnail for page $pageId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page thumbnail for page $pageId", e)
                false
            }
        }
    }

    /**
     * Load a page image from Firebase Storage
     */
    suspend fun loadPageImage(pageId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Đang tải ảnh cho trang: $pageId")

                // Xóa cache trước khi tải nếu cần thiết
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
    /**
     * Load a page thumbnail
     */
    suspend fun loadPageThumbnail(pageId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnailRef = storageRef.child("$PAGE_THUMBNAILS_PATH/$pageId.jpg")

                // Create a temporary file to store the downloaded image
                val localFile = File.createTempFile("thumbnail", "jpg")

                // Download to the local file
                thumbnailRef.getFile(localFile).await()

                // Decode the file into a bitmap
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

                // Delete the temporary file
                localFile.delete()

                Log.d(TAG, "Loaded page thumbnail for page $pageId")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading page thumbnail for page $pageId", e)
                null
            }
        }
    }

    /**
     * Delete a page image
     */
    suspend fun deletePageImage(pageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storageRef.child("$PAGES_PATH/$pageId.jpg")
                val thumbnailRef = storageRef.child("$PAGE_THUMBNAILS_PATH/$pageId.jpg")

                // Delete both image and thumbnail
                imageRef.delete().await()
                thumbnailRef.delete().await()

                Log.d(TAG, "Deleted page image and thumbnail for page $pageId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting page image for page $pageId", e)
                false
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
    /**
     * Load a thumbnail from Firebase Storage
     */
    // Trong StorageManager.kt
    suspend fun loadThumbnail(noteId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Đầu tiên tìm trong bộ nhớ cache local
                val localThumbnail = loadThumbnailLocally(noteId)
                if (localThumbnail != null) {
                    Log.d(TAG, "Loaded thumbnail from local cache")
                    return@withContext localThumbnail
                }

                // 2. Nếu không có trong cache, tìm trong thumbnails folder
                try {
                    val thumbnailRef = storageRef.child("$THUMBNAILS_PATH/$noteId.jpg")
                    val bytes = thumbnailRef.getBytes(1 * 1024 * 1024).await()

                    if (bytes != null && bytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            saveThumbnailLocally(noteId, bitmap)
                            return@withContext bitmap
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "No thumbnail found, will try first page image")
                }

                // 3. Nếu không có thumbnail, tìm ảnh trang đầu tiên
                try {
                    val noteRepository = FirebaseNoteRepository()
                    val noteResult = noteRepository.getNote(noteId)

                    if (noteResult.isSuccess) {
                        val note = noteResult.getOrNull()!!
                        if (note.pageIds.isNotEmpty()) {
                            val firstPageId = note.pageIds.first()
                            val pageImage = loadPageImage(firstPageId)

                            if (pageImage != null) {
                                val thumbnail = createThumbnail(pageImage)
                                saveThumbnail(noteId, thumbnail)
                                return@withContext thumbnail
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting first page image", e)
                }

                null
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadThumbnail", e)
                null
            }
        }
    }
    
    /**
     * Delete a thumbnail from Firebase Storage
     */
    suspend fun deleteThumbnail(noteId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnailRef = storageRef.child("$THUMBNAILS_PATH/$noteId.jpg")
                thumbnailRef.delete().await()
                
                // Also delete from local cache
                deleteThumbnailLocally(noteId)
                
                Log.d(TAG, "Deleted thumbnail for note $noteId from Firebase")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting thumbnail for note $noteId from Firebase", e)
                false
            }
        }
    }
    
    /**
     * Save a full-size image to Firebase Storage
     */
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

    suspend fun loadImageFromPath(path: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Đầu tiên tìm trong bộ nhớ cache local
                val filename = path.substringAfterLast("/")
                val localImage = loadCachedImage(filename)
                if (localImage != null) {
                    return@withContext localImage
                }

                // Nếu không có trong cache, tải từ Firebase
                val imageRef = storageRef.child(path)

                // Tạo file tạm
                val localFile = File.createTempFile("image", "jpg")

                // Tải xuống file
                imageRef.getFile(localFile).await()

                // Giải mã file thành bitmap
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

                // Lưu vào cache cho lần sau
                if (bitmap != null) {
                    saveCachedImage(filename, bitmap)
                }

                // Xóa file tạm
                localFile.delete()

                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image from path: $path", e)
                null
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
    /**
     * Load a full-size image from Firebase Storage
     */
    suspend fun loadImage(noteId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // First try to load from local cache
                val localImage = loadImageLocally(noteId)
                if (localImage != null) {
                    Log.d(TAG, "Loaded image for note $noteId from local cache")
                    return@withContext localImage
                }
                
                // If not in cache, download from Firebase
                val imageRef = storageRef.child("$IMAGES_PATH/$noteId.jpg")
                
                // Create a temporary file to store the downloaded image
                val localFile = File.createTempFile("image", "jpg")
                
                // Download to the local file
                imageRef.getFile(localFile).await()
                
                // Decode the file into a bitmap
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                
                // Save to local cache for faster access next time
                if (bitmap != null) {
                    saveImageLocally(noteId, bitmap)
                }
                
                // Delete the temporary file
                localFile.delete()
                
                Log.d(TAG, "Loaded image for note $noteId from Firebase")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for note $noteId from Firebase", e)
                null
            }
        }
    }
    
    /**
     * Delete a full-size image from Firebase Storage
     */
    suspend fun deleteImage(noteId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val imageRef = storageRef.child("$IMAGES_PATH/$noteId.jpg")
                imageRef.delete().await()
                
                // Also delete from local cache
                deleteImageLocally(noteId)
                
                Log.d(TAG, "Deleted image for note $noteId from Firebase")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image for note $noteId from Firebase", e)
                false
            }
        }
    }
    
    /**
     * Save drawing data to Firebase Storage
     */
    suspend fun saveDrawing(noteId: String, drawingData: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drawingRef = storageRef.child("$DRAWINGS_PATH/$noteId.png")
                
                // Upload to Firebase
                val uploadTask = drawingRef.putBytes(drawingData).await()
                
                Log.d(TAG, "Saved drawing for note $noteId to Firebase")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error saving drawing for note $noteId to Firebase", e)
                false
            }
        }
    }
    
    /**
     * Load drawing data from Firebase Storage
     */
    suspend fun loadDrawing(noteId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val drawingRef = storageRef.child("$DRAWINGS_PATH/$noteId.png")
                
                // Download as bytes
                val maxSize = 10 * 1024 * 1024 // 10MB max
                val bytes = drawingRef.getBytes(maxSize.toLong()).await()
                
                Log.d(TAG, "Loaded drawing for note $noteId from Firebase")
                bytes
            } catch (e: Exception) {
                Log.e(TAG, "Error loading drawing for note $noteId from Firebase", e)
                null
            }
        }
    }
    
    /**
     * Get a download URL for sharing
     */
    suspend fun getDownloadUrl(noteId: String, type: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val path = when (type) {
                    "image" -> "$IMAGES_PATH/$noteId.jpg"
                    "thumbnail" -> "$THUMBNAILS_PATH/$noteId.jpg"
                    "drawing" -> "$DRAWINGS_PATH/$noteId.png"
                    else -> throw IllegalArgumentException("Invalid type: $type")
                }
                
                val ref = storageRef.child(path)
                val url = ref.downloadUrl.await()
                
                Log.d(TAG, "Got download URL for $type of note $noteId")
                url
            } catch (e: Exception) {
                Log.e(TAG, "Error getting download URL for $type of note $noteId", e)
                null
            }
        }
    }
    
    /**
     * Check if a file exists in Firebase Storage
     */
    suspend fun exists(noteId: String, type: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val path = when (type) {
                    "image" -> "$IMAGES_PATH/$noteId.jpg"
                    "thumbnail" -> "$THUMBNAILS_PATH/$noteId.jpg"
                    "drawing" -> "$DRAWINGS_PATH/$noteId.png"
                    else -> throw IllegalArgumentException("Invalid type: $type")
                }
                
                val ref = storageRef.child(path)
                ref.metadata.await() // Will throw exception if file doesn't exist
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Local cache methods for faster access
    
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
    
    public fun deleteImageLocally(noteId: String): Boolean {
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