package com.example.app_music.data.remote.api

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/**
 * A mock service to simulate the backend image processing API
 * In a real app, this would be replaced with actual API calls to a server
 */
class ImageProcessingMockApi {

    companion object {
        private val MOCK_QUESTIONS = listOf(
            "Tìm hệ số a,b,c để y sau có cực trị: y = a·x² + b·x + c·y + x²",
            "Find the values of x for which f(x) = x³ - 6x² + 9x - 2 has a local minimum.",
            "Consider f(x) = ln(x). Calculate ∫[1,e] f(x) dx.",
            "Solve the equation: sin(x) + cos(x) = 1 for x in [0, 2π]",
            "Evaluate the limit: lim(x→0) (sin(3x)/x)"
        )

        private val MOCK_ANSWERS = listOf(
            "Để có cực trị, y' = 0:\n2a·x + b + 2x = 0\nSắp xếp: (2a+2)x + b = 0\nVì phương trình này có nghiệm với mọi x, nên:\n2a+2 = 0 → a = -1\nb = 0\nDo đó (a,b,c) = (-1, 0, c) với c bất kỳ.",
            "To find local minima, set f'(x) = 0 and check f''(x) > 0.\nf'(x) = 3x² - 12x + 9\nSolving 3x² - 12x + 9 = 0:\n3(x² - 4x + 3) = 0\n3(x - 1)(x - 3) = 0\nx = 1 or x = 3\nChecking f''(x) = 6x - 12\nf''(1) = 6 - 12 = -6 < 0, so x = 1 is a local maximum\nf''(3) = 18 - 12 = 6 > 0, so x = 3 is a local minimum\nTherefore, f(x) has a local minimum at x = 3.",
            "∫[1,e] ln(x) dx = [x·ln(x) - x][1,e]\n= e·ln(e) - e - (1·ln(1) - 1)\n= e·1 - e - (0 - 1)\n= e - e + 1 = 1",
            "sin(x) + cos(x) = 1\nSquaring both sides:\nsin²(x) + 2sin(x)cos(x) + cos²(x) = 1\n1 + 2sin(x)cos(x) = 1\n2sin(x)cos(x) = 0\nsin(2x) = 0\n2x = nπ where n is an integer\nx = nπ/2\nFor x in [0, 2π]: x = 0, π/2, π, 3π/2, 2π\nChecking each value:\nAt x = 0: sin(0) + cos(0) = 0 + 1 = 1 ✓\nAt x = π/2: sin(π/2) + cos(π/2) = 1 + 0 = 1 ✓\nAt x = π: sin(π) + cos(π) = 0 + (-1) = -1 ✗\nAt x = 3π/2: sin(3π/2) + cos(3π/2) = -1 + 0 = -1 ✗\nAt x = 2π: sin(2π) + cos(2π) = 0 + 1 = 1 ✓\nSolution set: x = 0, π/2, 2π",
            "lim(x→0) (sin(3x)/x)\n= lim(x→0) (3·sin(3x)/3x)\n= 3·lim(x→0) (sin(3x)/3x)\nLet u = 3x, as x→0, u→0\n= 3·lim(u→0) (sin(u)/u)\n= 3·1 = 3"
        )
    }

    /**
     * Mock method to process an image and return recognized text and solution
     * @param imageFile The captured and cropped image file
     * @param callback Callback to deliver the result
     */
    fun processImage(imageFile: File, callback: (ImageProcessingResult) -> Unit) {
        // Simulate network delay
        Handler(Looper.getMainLooper()).postDelayed({
            // In a real app, this would send the image to a server for OCR and problem-solving
            // Here we just return random mock data
            val randomIndex = Random.nextInt(MOCK_QUESTIONS.size)
            val result = ImageProcessingResult(
                question = MOCK_QUESTIONS[randomIndex],
                answer = MOCK_ANSWERS[randomIndex],
                success = true
            )
            callback(result)
        }, 2000) // 2 seconds delay to simulate network request
    }

    /**
     * Save a bitmap to a file
     * @param bitmap The bitmap to save
     * @param file The file to save to
     * @return Whether the operation was successful
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    data class ImageProcessingResult(
        val question: String,
        val answer: String,
        val success: Boolean
    )
}