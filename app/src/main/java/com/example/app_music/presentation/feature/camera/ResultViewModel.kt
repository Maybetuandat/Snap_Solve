package com.example.app_music.presentation.feature.camera

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.ImageRepository
import kotlinx.coroutines.launch
import java.io.File

class ResultViewModel : ViewModel() {
    private val imageRepository = ImageRepository()

    private val _uploadStatus = MutableLiveData<UploadStatus>()
    val uploadStatus: LiveData<UploadStatus> = _uploadStatus

    private val _analyzeResult = MutableLiveData<AnalyzeResult>()
    val analyzeResult: LiveData<AnalyzeResult> = _analyzeResult

    // Track if we've uploaded already to avoid duplicate uploads
    private var isImageUploaded = false

    // Upload the cropped image to server
    fun uploadImage(imageFile: File, uploadAutomatically: Boolean = true) {
        if (isImageUploaded && uploadAutomatically) {
            Log.d(TAG, "Image already uploaded, skipping")
            return
        }

        if (uploadAutomatically) {
            _uploadStatus.value = UploadStatus.Loading
        }

        viewModelScope.launch {
            try {
                val response = imageRepository.uploadImage(imageFile)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        isImageUploaded = true
                        _uploadStatus.value = UploadStatus.Success(
                            imageUrl = result.imageUrl,
                            imageId = result.imageId
                        )

                        // If we have an imageId, request analysis
                        result.imageId?.let {
                            requestImageAnalysis(it)
                        }
                    } else {
                        _uploadStatus.value = UploadStatus.Error(result.message)
                    }
                } else {
                    _uploadStatus.value = UploadStatus.Error(
                        "Upload failed: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                _uploadStatus.value = UploadStatus.Error(
                    "Upload error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    // Request server to analyze the uploaded image
    private fun requestImageAnalysis(imageId: String) {
        _analyzeResult.value = AnalyzeResult.Loading

        // In a real implementation, you'd make another API call here
        // For now, we'll simulate with mock data
        viewModelScope.launch {
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(1500)

                // Mock response with proper inline LaTeX formatting
                // Added spaces between $$ and text to prevent parsing issues
                _analyzeResult.value = AnalyzeResult.Success(
                    question = """
                Một chiếc thùng hình lập phương có chiều dài cạnh là $$ x $$ (cm).
                a) Viết công thức tính thể tích $$ V $$ ($$ cm^3 $$) và tổng diện tích $$ S $$ ($$ cm^2 $$) các mặt của hình lập phương theo $$ x $$
                b) Viết công thức tính $$ x $$ theo $$ S $$
                c) Viết công thức tính $$ V $$ theo $$ S $$. Tính $$ V $$ khi $$ S = 50 \text{ cm}^{2} $$
            """.trimIndent(),

                    answer = """
                a) Thể tích hình lập phương:
                $$ V = x^3 $$ ($$ cm^3 $$)
                Diện tích một mặt: $$ x^2 $$
                Tổng diện tích 6 mặt:
                $$ S = 6x^2 $$ ($$ cm^2 $$)
                b) Từ công thức $$ S = 6x^2 $$, ta có:
                $$ x^2 = \frac{S}{6} $$
                $$ x = \sqrt{\frac{S}{6}} $$
                c) Thay công thức của $$ x $$ theo $$ S $$ vào công thức tính $$ V $$:
                $$ V = x^3 = \left(\sqrt{\frac{S}{6}}\right)^3 = \frac{S^{3/2}}{6^{3/2}} = \frac{S^{3/2}}{6\sqrt{6}} $$
                Khi $$ S = 50 \text{ cm}^2 $$:
                $$ V = \frac{50^{3/2}}{6\sqrt{6}} = \frac{50\sqrt{50}}{6\sqrt{6}} \approx 47.54 \text{ cm}^3 $$
            """.trimIndent()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing image", e)
                _analyzeResult.value = AnalyzeResult.Error(
                    "Analysis error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    // Explicitly request manual upload (used when button is clicked)
    fun manuallyUploadImage(imageFile: File) {
        uploadImage(imageFile, false)
    }

    companion object {
        private const val TAG = "ResultViewModel"
    }

    // Sealed classes for state
    sealed class UploadStatus {
        object Loading : UploadStatus()
        data class Success(val imageUrl: String?, val imageId: String?) : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }

    sealed class AnalyzeResult {
        object Loading : AnalyzeResult()
        data class Success(val question: String, val answer: String) : AnalyzeResult()
        data class Error(val message: String) : AnalyzeResult()
    }
}