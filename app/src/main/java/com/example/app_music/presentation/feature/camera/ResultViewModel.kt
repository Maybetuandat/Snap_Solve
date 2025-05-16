package com.example.app_music.presentation.feature.camera

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.remote.api.ImageUploadResponse
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

                // Mock response
                _analyzeResult.value = AnalyzeResult.Success(
                    question = "Tìm hệ số a,b,c để y sau có cực trị: y = a·x² + b·x + c·y + x²",
                    answer = "Để có cực trị, y' = 0:\n2a·x + b + 2x = 0\nSắp xếp: (2a+2)x + b = 0\nVì phương trình này có nghiệm với mọi x, nên:\n2a+2 = 0 → a = -1\nb = 0\nDo đó (a,b,c) = (-1, 0, c) với c bất kỳ."
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