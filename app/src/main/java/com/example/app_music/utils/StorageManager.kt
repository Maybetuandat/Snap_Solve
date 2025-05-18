package com.example.app_music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
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

    /**
     * Save a thumbnail to Firebase Storage including drawing content
     */
    suspend fun saveThumbnail(noteId: String, thumbnail: Bitmap, includingDrawing: Boolean = false): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Saving thumbnail for note: $noteId, includingDrawing: $includingDrawing")
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

    /**
     * Update thumbnail with drawing content
     */
    suspend fun updateThumbnailWithDrawing(noteId: String, combinedBitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating thumbnail with drawing for note: $noteId")

                // First resize the bitmap to thumbnail size if needed
                val maxSize = 200
                val width = combinedBitmap.width
                val height = combinedBitmap.height

                val scale = Math.min(
                    maxSize.toFloat() / width.toFloat(),
                    maxSize.toFloat() / height.toFloat()
                )

                val thumbnailBitmap = if (scale < 1) {
                    val scaledWidth = (width * scale).toInt()
                    val scaledHeight = (height * scale).toInt()
                    Bitmap.createScaledBitmap(combinedBitmap, scaledWidth, scaledHeight, true)
                } else {
                    combinedBitmap.copy(combinedBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                }

                // Save the updated thumbnail
                val result = saveThumbnail(noteId, thumbnailBitmap, true)

                if (!result) {
                    Log.e(TAG, "Failed to update thumbnail with drawing")
                }

                result
            } catch (e: Exception) {
                Log.e(TAG, "Error updating thumbnail with drawing: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Load a thumbnail from Firebase Storage
     */
    suspend fun loadThumbnail(noteId: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // First try to load from local cache
                val localThumbnail = loadThumbnailLocally(noteId)
                if (localThumbnail != null) {
                    Log.d(TAG, "Loaded thumbnail for note $noteId from local cache")
                    return@withContext localThumbnail
                }

                // If not in cache, download from Firebase
                val thumbnailRef = storageRef.child("$THUMBNAILS_PATH/$noteId.jpg")

                // Create a temporary file to store the downloaded image
                val localFile = File.createTempFile("thumbnail", "jpg")

                // Download to the local file
                thumbnailRef.getFile(localFile).await()

                // Decode the file into a bitmap
                val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

                // Save to local cache for faster access next time
                if (bitmap != null) {
                    saveThumbnailLocally(noteId, bitmap)
                }

                // Delete the temporary file
                localFile.delete()

                Log.d(TAG, "Loaded thumbnail for note $noteId from Firebase")
                bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error loading thumbnail for note $noteId from Firebase", e)
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

    private fun deleteThumbnailLocally(noteId: String): Boolean {
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

    private fun deleteImageLocally(noteId: String): Boolean {
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