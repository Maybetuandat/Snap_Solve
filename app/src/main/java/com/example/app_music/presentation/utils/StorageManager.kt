package com.example.app_music.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.app_music.presentation.noteScene.model.NoteItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class StorageManager(private val context: Context) {

    companion object {
        private const val TAG = "StorageManager"
        private const val NOTES_DIRECTORY = "notes"
        private const val THUMBNAILS_DIRECTORY = "thumbnails"
        private const val DRAWING_DIRECTORY = "drawings"
        private const val NOTES_DATA_FILE = "notes_data.json"
    }

    init {
        // Tạo các thư mục cần thiết
        getNoteDirectory().mkdirs()
        getThumbnailDirectory().mkdirs()
        getDrawingDirectory().mkdirs()
    }

    private fun getNoteDirectory(): File {
        return File(context.filesDir, NOTES_DIRECTORY)
    }

    private fun getThumbnailDirectory(): File {
        return File(context.filesDir, THUMBNAILS_DIRECTORY)
    }

    private fun getDrawingDirectory(): File {
        return File(context.filesDir, DRAWING_DIRECTORY)
    }

    // Lưu file từ URI
    fun saveNoteFromUri(uri: Uri, noteId: String): Boolean {
        Log.d(TAG, "Đang lưu note từ URI: $uri")

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Không thể mở input stream từ URI")
                return false
            }

            val noteFile = File(getNoteDirectory(), "$noteId.jpg")
            Log.d(TAG, "Lưu vào file: ${noteFile.absolutePath}")

            // Đảm bảo thư mục tồn tại
            noteFile.parentFile?.mkdirs()

            // Sử dụng FileOutputStream để ghi dữ liệu
            val outputStream = FileOutputStream(noteFile)
            val buffer = ByteArray(1024)
            var length: Int

            // Copy dữ liệu từ inputStream sang outputStream
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Tạo thumbnail
            createThumbnail(noteId)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu note: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    fun saveDrawingLayer(noteId: String, drawingBitmap: Bitmap): Boolean {
        try {
            val drawingFile = File(getDrawingDirectory(), "$noteId.png")

            // Đảm bảo thư mục tồn tại
            drawingFile.parentFile?.mkdirs()

            // Lưu bitmap vào file
            val out = FileOutputStream(drawingFile)
            drawingBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()

            // Cập nhật thumbnail ngay lập tức để hiển thị cả ảnh gốc và phần vẽ
            createThumbnail(noteId)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu lớp vẽ: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // Tải lớp vẽ
    fun loadDrawingLayer(noteId: String): Bitmap? {
        val drawingFile = File(getDrawingDirectory(), "$noteId.png")
        if (!drawingFile.exists()) {
            return null
        }

        try {
            return BitmapFactory.decodeFile(drawingFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tải lớp vẽ: ${e.message}")
            return null
        }
    }

    private fun createThumbnail(noteId: String): Boolean {
        Log.d(TAG, "Tạo thumbnail cho note: $noteId")

        try {
            val noteFile = File(getNoteDirectory(), "$noteId.jpg")
            val drawingFile = File(getDrawingDirectory(), "$noteId.png")
            val thumbnailFile = File(getThumbnailDirectory(), "$noteId.jpg")

            // Đảm bảo thư mục tồn tại
            thumbnailFile.parentFile?.mkdirs()

            if (!noteFile.exists()) {
                Log.e(TAG, "File ảnh gốc không tồn tại")
                return false
            }

            // Giới hạn kích thước để tiết kiệm bộ nhớ
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(noteFile.absolutePath, options)

            // Tính toán tỷ lệ thu nhỏ
            val maxSize = 500 // Tăng kích thước lên để thumbnail chất lượng cao hơn
            var inSampleSize = 1

            if (options.outHeight > maxSize || options.outWidth > maxSize) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxSize)
                val widthRatio = Math.round(options.outWidth.toFloat() / maxSize)
                inSampleSize = Math.max(1, Math.min(heightRatio, widthRatio))
            }

            // Tải bitmap với tỷ lệ thu nhỏ
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                this.inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            // Tạo bitmap từ ảnh gốc
            val originalBitmap = BitmapFactory.decodeFile(noteFile.absolutePath, decodeOptions)
                ?: return false

            // Tạo bitmap mới với tỷ lệ khung hình phù hợp
            val thumbnailWidth = 300
            val thumbnailHeight = 400

            // Tính toán tỷ lệ
            val srcWidth = originalBitmap.width
            val srcHeight = originalBitmap.height

            // Tạo bitmap cho thumbnail
            val thumbnailBitmap = Bitmap.createBitmap(thumbnailWidth, thumbnailHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(thumbnailBitmap)

            // Vẽ nền trắng
            canvas.drawColor(Color.WHITE)

            // Tính toán tỷ lệ và vị trí để giữ ảnh trong khung
            val widthRatio = thumbnailWidth.toFloat() / srcWidth
            val heightRatio = thumbnailHeight.toFloat() / srcHeight
            val scale = Math.min(widthRatio, heightRatio)

            val scaledWidth = (srcWidth * scale).toInt()
            val scaledHeight = (srcHeight * scale).toInt()

            // Đặt ảnh gốc vào giữa
            val left = (thumbnailWidth - scaledWidth) / 2f
            val top = (thumbnailHeight - scaledHeight) / 2f

            // Scale bitmap gốc
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            canvas.drawBitmap(scaledBitmap, left, top, null)

            // Giải phóng bitmap gốc
            originalBitmap.recycle()

            // Vẽ thêm lớp drawing nếu có
            if (drawingFile.exists()) {
                try {
                    val drawingBitmap = BitmapFactory.decodeFile(drawingFile.absolutePath)
                    if (drawingBitmap != null) {
                        // Scale drawing bitmap để khớp với ảnh gốc đã scale
                        val scaledDrawing = Bitmap.createScaledBitmap(
                            drawingBitmap,
                            scaledWidth,
                            scaledHeight,
                            true
                        )
                        canvas.drawBitmap(scaledDrawing, left, top, null)

                        // Giải phóng bộ nhớ
                        drawingBitmap.recycle()
                        scaledDrawing.recycle()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi khi vẽ lớp drawing lên thumbnail: ${e.message}")
                    // Tiếp tục xử lý mà không dừng lại
                }
            }

            // Lưu thumbnail với chất lượng cao
            val out = FileOutputStream(thumbnailFile)
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            out.flush()
            out.close()

            // Giải phóng bộ nhớ
            thumbnailBitmap.recycle()
            scaledBitmap.recycle()

            Log.d(TAG, "Tạo thumbnail thành công")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo thumbnail: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // Tải thumbnail
    fun loadThumbnail(noteId: String): Bitmap? {
        val thumbnailFile = File(getThumbnailDirectory(), "$noteId.jpg")

        return if (thumbnailFile.exists()) {
            try {
                // Tải bitmap từ file
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFile(thumbnailFile.absolutePath, options)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi tải thumbnail: ${e.message}")
                null
            }
        } else {
            Log.d(TAG, "Không tìm thấy thumbnail cho note: $noteId")
            null
        }
    }

    // Lấy URI cho file ảnh
    fun getNoteImageUri(noteId: String): Uri? {
        val noteFile = File(getNoteDirectory(), "$noteId.jpg")

        return if (noteFile.exists()) {
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    noteFile
                )
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi lấy URI từ file: ${e.message}")
                null
            }
        } else {
            Log.d(TAG, "Không tìm thấy file ảnh cho note: $noteId")
            null
        }
    }

    // Lấy bitmap ảnh gốc
    fun getNoteImageBitmap(noteId: String): Bitmap? {
        val noteFile = File(getNoteDirectory(), "$noteId.jpg")

        return if (noteFile.exists()) {
            try {
                BitmapFactory.decodeFile(noteFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi tải ảnh gốc: ${e.message}")
                null
            }
        } else {
            Log.d(TAG, "Không tìm thấy ảnh gốc cho note: $noteId")
            null
        }
    }

    // Tạo note mới từ URI
    fun createNoteFromUri(uri: Uri, title: String): NoteItem? {
        val noteId = System.currentTimeMillis().toString()
        val dateFormat = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        return if (saveNoteFromUri(uri, noteId)) {
            val thumbnail = loadThumbnail(noteId)
            if (thumbnail != null) {
                NoteItem(noteId, title, currentDate, false, false, thumbnail)
            } else {
                NoteItem(noteId, title, currentDate)
            }
        } else {
            null
        }
    }

    // Xóa note
    fun deleteNote(noteId: String): Boolean {
        var success = true

        // Xóa file ảnh gốc
        val noteFile = File(getNoteDirectory(), "$noteId.jpg")
        if (noteFile.exists() && !noteFile.delete()) {
            success = false
        }

        // Xóa thumbnail
        val thumbnailFile = File(getThumbnailDirectory(), "$noteId.jpg")
        if (thumbnailFile.exists() && !thumbnailFile.delete()) {
            success = false
        }

        // Xóa file drawing nếu có
        val drawingFile = File(getDrawingDirectory(), "$noteId.png")
        if (drawingFile.exists() && !drawingFile.delete()) {
            success = false
        }

        return success
    }

    // Lưu danh sách notes vào bộ nhớ trong
    fun saveNotesList(notes: List<NoteItem>) {
        try {
            val jsonArray = JSONArray()

            // Chuyển đổi mỗi note thành JSON và thêm vào mảng
            for (note in notes) {
                val noteJson = JSONObject()
                noteJson.put("id", note.id)
                noteJson.put("title", note.title)
                noteJson.put("date", note.date)
                noteJson.put("isFolder", note.isFolder)

                // Nếu là thư mục, lưu thêm danh sách con
                if (note.isFolder) {
                    val childrenJson = JSONArray()

                    for (childNote in note.getChildNotes()) {
                        val childJson = JSONObject()
                        childJson.put("id", childNote.id)
                        childJson.put("title", childNote.title)
                        childJson.put("date", childNote.date)
                        childJson.put("isFolder", childNote.isFolder)

                        // Nếu con này cũng là thư mục, thêm danh sách cháu
                        if (childNote.isFolder) {
                            val grandChildrenJson = JSONArray()

                            for (grandChild in childNote.getChildNotes()) {
                                val grandChildJson = JSONObject()
                                grandChildJson.put("id", grandChild.id)
                                grandChildJson.put("title", grandChild.title)
                                grandChildJson.put("date", grandChild.date)
                                grandChildJson.put("isFolder", grandChild.isFolder)

                                grandChildrenJson.put(grandChildJson)
                            }

                            childJson.put("children", grandChildrenJson)
                        }

                        childrenJson.put(childJson)
                    }

                    noteJson.put("children", childrenJson)
                }

                jsonArray.put(noteJson)
            }

            // Lưu mảng JSON vào file
            val notesFile = File(context.filesDir, NOTES_DATA_FILE)
            FileOutputStream(notesFile).use { fos ->
                fos.write(jsonArray.toString().toByteArray())
            }

            Log.d(TAG, "Đã lưu ${notes.size} notes vào bộ nhớ")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu notes: ${e.message}")
            e.printStackTrace()
        }
    }

    // Tải danh sách notes từ bộ nhớ trong
    fun loadNotesList(): MutableList<NoteItem> {
        val notes = mutableListOf<NoteItem>()

        try {
            val notesFile = File(context.filesDir, NOTES_DATA_FILE)

            if (notesFile.exists()) {
                val jsonStr = notesFile.readText()
                val jsonArray = JSONArray(jsonStr)

                // Duyệt qua từng phần tử trong mảng JSON
                for (i in 0 until jsonArray.length()) {
                    val noteJson = jsonArray.getJSONObject(i)

                    val id = noteJson.getString("id")
                    val title = noteJson.getString("title")
                    val date = noteJson.getString("date")
                    val isFolder = noteJson.getBoolean("isFolder")

                    if (isFolder) {
                        // Đây là thư mục
                        val folder = NoteItem(id, title, date, true)

                        // Nếu có danh sách con
                        if (noteJson.has("children")) {
                            val childrenArray = noteJson.getJSONArray("children")

                            // Duyệt qua từng con
                            for (j in 0 until childrenArray.length()) {
                                val childJson = childrenArray.getJSONObject(j)

                                val childId = childJson.getString("id")
                                val childTitle = childJson.getString("title")
                                val childDate = childJson.getString("date")
                                val childIsFolder = childJson.getBoolean("isFolder")

                                if (childIsFolder) {
                                    // Đây là thư mục con
                                    val childFolder = NoteItem(childId, childTitle, childDate, true)

                                    // Nếu có danh sách cháu
                                    if (childJson.has("children")) {
                                        val grandChildrenArray = childJson.getJSONArray("children")

                                        // Duyệt qua từng cháu
                                        for (k in 0 until grandChildrenArray.length()) {
                                            val grandChildJson = grandChildrenArray.getJSONObject(k)

                                            val grandChildId = grandChildJson.getString("id")
                                            val grandChildTitle = grandChildJson.getString("title")
                                            val grandChildDate = grandChildJson.getString("date")
                                            val grandChildIsFolder = grandChildJson.getBoolean("isFolder")

                                            // Tạo note cháu
                                            val grandChild = if (grandChildIsFolder) {
                                                // Đây là thư mục cháu
                                                NoteItem(grandChildId, grandChildTitle, grandChildDate, true)
                                            } else {
                                                // Đây là note cháu
                                                val thumbnail = loadThumbnail(grandChildId)
                                                if (thumbnail != null) {
                                                    NoteItem(grandChildId, grandChildTitle, grandChildDate, false, false, thumbnail)
                                                } else {
                                                    NoteItem(grandChildId, grandChildTitle, grandChildDate)
                                                }
                                            }

                                            // Thêm cháu vào thư mục con
                                            childFolder.addChildNote(grandChild)
                                        }
                                    }

                                    // Thêm thư mục con vào thư mục cha
                                    folder.addChildNote(childFolder)
                                } else {
                                    // Đây là note con thường
                                    val thumbnail = loadThumbnail(childId)
                                    val childNote = if (thumbnail != null) {
                                        NoteItem(childId, childTitle, childDate, false, false, thumbnail)
                                    } else {
                                        NoteItem(childId, childTitle, childDate)
                                    }

                                    // Thêm note con vào thư mục cha
                                    folder.addChildNote(childNote)
                                }
                            }
                        }

                        // Thêm thư mục vào danh sách
                        notes.add(folder)
                    } else {
                        // Đây là note thường
                        val thumbnail = loadThumbnail(id)
                        val note = if (thumbnail != null) {
                            NoteItem(id, title, date, false, false, thumbnail)
                        } else {
                            NoteItem(id, title, date)
                        }

                        // Thêm note vào danh sách
                        notes.add(note)
                    }
                }
            } else {
                // Nếu file dữ liệu không tồn tại, kiểm tra xem có ảnh đã lưu trước đó không
                restoreLegacyData(notes)
            }

            Log.d(TAG, "Đã tải ${notes.size} notes từ bộ nhớ")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tải notes: ${e.message}")
            e.printStackTrace()

            // Thử khôi phục dữ liệu cũ nếu có lỗi
            restoreLegacyData(notes)
        }
        if (notes.isEmpty() || !notes.any { it.isFolder }) {
            // Tạo thư mục mặc định nếu chưa có thư mục nào
            val defaultFolder = NoteItem(
                "folder_default",
                "Album mặc định",
                SimpleDateFormat("d MMM, yyyy", Locale.getDefault()).format(Date()),
                true
            )
            notes.add(defaultFolder)

            // Lưu dữ liệu ngay lập tức
            saveNotesList(notes)
            Log.d(TAG, "Đã tạo thư mục mặc định")
        }

// Trả về danh sách đã tải hoặc đã tạo mới
        return notes

        return notes
    }
    // Thêm vào StorageManager.kt
    fun saveNoteImage(noteId: String, bitmap: Bitmap): Boolean {
        try {
            val noteFile = File(getNoteDirectory(), "$noteId.jpg")

            // Đảm bảo thư mục tồn tại
            noteFile.parentFile?.mkdirs()

            // Lưu bitmap vào file với chất lượng cao
            val out = FileOutputStream(noteFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi lưu ảnh note: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    private fun restoreLegacyData(notes: MutableList<NoteItem>) {
        try {
            // Nếu đã có dữ liệu, không cần khôi phục
            if (notes.isNotEmpty()) {
                return
            }

            // Lấy danh sách ảnh từ thư mục notes
            val noteFiles = getNoteDirectory().listFiles { file ->
                file.isFile && file.name.endsWith(".jpg")
            }

            // Nếu không có ảnh, không cần khôi phục
            if (noteFiles == null || noteFiles.isEmpty()) {
                return
            }

            // Tạo một thư mục để chứa tất cả ghi chú cũ không phân loại
            val legacyFolder = NoteItem(
                "folder_legacy",
                "Ghi chú cũ",
                SimpleDateFormat("d MMM, yyyy", Locale.getDefault()).format(Date()),
                true
            )

            // Duyệt qua từng file ảnh
            var hasNotes = false
            noteFiles.forEach { file ->
                val noteId = file.nameWithoutExtension

                // Tạo thông tin cho note
                val dateFormat = SimpleDateFormat("d MMM, yyyy", Locale.getDefault())
                val fileDate = Date(file.lastModified())
                val dateStr = dateFormat.format(fileDate)

                // Tạo note mới với title là tên file
                val thumbnail = loadThumbnail(noteId)
                val note = if (thumbnail != null) {
                    NoteItem(noteId, "Note $noteId", dateStr, false, false, thumbnail)
                } else {
                    // Nếu chưa có thumbnail, tạo mới
                    createThumbnail(noteId)
                    // Và thử tải lại
                    val newThumbnail = loadThumbnail(noteId)
                    if (newThumbnail != null) {
                        NoteItem(noteId, "Note $noteId", dateStr, false, false, newThumbnail)
                    } else {
                        NoteItem(noteId, "Note $noteId", dateStr)
                    }
                }

                // Thêm note vào thư mục legacy
                legacyFolder.addChildNote(note)
                hasNotes = true
            }

            // Chỉ thêm thư mục legacy nếu có notes
            if (hasNotes) {
                notes.add(legacyFolder)
                Log.d(TAG, "Đã khôi phục ghi chú cũ vào thư mục '${legacyFolder.title}'")
            }

            // Lưu thông tin notes vào file JSON để lần sau không cần khôi phục
            saveNotesList(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khôi phục dữ liệu cũ: ${e.message}")
            e.printStackTrace()
        }
    }
}