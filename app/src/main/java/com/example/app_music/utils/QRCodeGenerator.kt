package com.example.app_music.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import java.util.EnumMap

object QRCodeGenerator {
    
    private const val QR_CODE_SIZE = 300
    
    /**
     * Generate a QR code bitmap from a string
     * @param content The content to encode in the QR code
     * @param size The size of the QR code bitmap (default 300x300)
     * @return The QR code bitmap or null if generation fails
     */
    fun generateQRCode(content: String, size: Int = QR_CODE_SIZE): Bitmap? {
        try {
            // Set QR code parameters
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 2 // Margin size
            
            // Create BitMatrix for the QR code
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            
            // Convert BitMatrix to Bitmap
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Generate a QR code for note sharing
     * @param noteId The ID of the note to share
     * @return QR code bitmap
     */
    fun generateNoteShareQRCode(noteId: String): Bitmap? {
        val shareableUrl = "snapsolve://notes/$noteId"
        return generateQRCode(shareableUrl)
    }
    
    /**
     * Generate a QR code for folder sharing
     * @param folderId The ID of the folder to share
     * @return QR code bitmap
     */
    fun generateFolderShareQRCode(folderId: String): Bitmap? {
        val shareableUrl = "snapsolve://folders/$folderId"
        return generateQRCode(shareableUrl)
    }
}