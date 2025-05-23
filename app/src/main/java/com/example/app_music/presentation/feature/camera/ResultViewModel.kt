package com.example.app_music.presentation.feature.camera

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.AssignmentRepository
import com.example.app_music.data.repository.ImageRepository
import com.example.app_music.data.repository.TextSearchRepository
import com.example.app_music.data.repository.AISolutionRepository
import com.example.app_music.domain.model.Assignment
import kotlinx.coroutines.launch
import java.io.File

class ResultViewModel : ViewModel() {
    private val imageRepository = ImageRepository()
    private val textSearchRepository = TextSearchRepository()
    private val assignmentRepository = AssignmentRepository()
    private val aiSolutionRepository = AISolutionRepository()

    private val _uploadStatus = MutableLiveData<UploadStatus>()
    val uploadStatus: LiveData<UploadStatus> = _uploadStatus

    private val _assignments = MutableLiveData<List<Assignment>?>()
    val assignments: MutableLiveData<List<Assignment>?> = _assignments

    // Track current assignment index for pagination
    private val _currentAssignmentIndex = MutableLiveData<Int>(0)
    val currentAssignmentIndex: LiveData<Int> = _currentAssignmentIndex

    // AI Solution status
    private val _aiSolutionStatus = MutableLiveData<AISolutionStatus>()
    val aiSolutionStatus: LiveData<AISolutionStatus> = _aiSolutionStatus

    // Upload the cropped image to server
    fun uploadImage(imageFile: File, context: Context) {
        _uploadStatus.value = UploadStatus.Loading

        // Get userId from preferences
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _uploadStatus.value = UploadStatus.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val response = imageRepository.uploadImage(imageFile, "Image search", userId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        _uploadStatus.value = UploadStatus.Success(
                            imageUrl = result.imageUrl,
                            imageId = result.imageId
                        )

                        // Process assignments if available
                        if (!result.assignments.isNullOrEmpty()) {
                            _assignments.value = result.assignments
                            _currentAssignmentIndex.value = 0 // Start with first assignment
                        } else {
                            _assignments.value = emptyList()
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

    // Search by text query
    fun searchByText(query: String, context: Context) {
        _uploadStatus.value = UploadStatus.Loading

        // Get userId from preferences
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _uploadStatus.value = UploadStatus.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val response = textSearchRepository.searchByText(query, userId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        _uploadStatus.value = UploadStatus.Success(
                            imageUrl = null,
                            imageId = null
                        )

                        // Process assignments if available
                        if (!result.assignments.isNullOrEmpty()) {
                            _assignments.value = result.assignments
                            _currentAssignmentIndex.value = 0
                        } else {
                            _assignments.value = emptyList()
                        }
                    } else {
                        _uploadStatus.value = UploadStatus.Error(result.message)
                    }
                } else {
                    _uploadStatus.value = UploadStatus.Error(
                        "Search failed: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching by text", e)
                _uploadStatus.value = UploadStatus.Error(
                    "Search error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun loadAssignmentsByIds(assignmentIds: List<Long>) {
        _uploadStatus.value = UploadStatus.Loading

        viewModelScope.launch {
            try {
                val response = assignmentRepository.getAssignmentsByIds(assignmentIds)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.isNotEmpty()) {
                        _assignments.value = result
                        _currentAssignmentIndex.value = 0
                        _uploadStatus.value = UploadStatus.Success(
                            imageUrl = null,
                            imageId = null
                        )
                    } else {
                        _uploadStatus.value = UploadStatus.Error("No assignments found")
                    }
                } else {
                    _uploadStatus.value = UploadStatus.Error(
                        "Failed to load assignments: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading assignments", e)
                _uploadStatus.value = UploadStatus.Error(
                    "Error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    // Get AI Solution
    fun getAISolution(query: String, context: Context) {
        _aiSolutionStatus.value = AISolutionStatus.Loading

        // Get userId from preferences
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _aiSolutionStatus.value = AISolutionStatus.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val response = aiSolutionRepository.getAISolution(query, userId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result["success"] == true) {
                        val solution = result["solution"] as? String
                        if (solution != null) {
                            _aiSolutionStatus.value = AISolutionStatus.Success(solution)
                        } else {
                            _aiSolutionStatus.value = AISolutionStatus.Error("Invalid response format")
                        }
                    } else if (result["requirePremium"] == true) {
                        _aiSolutionStatus.value = AISolutionStatus.PremiumRequired
                    } else {
                        val message = result["message"] as? String ?: "Unknown error"
                        _aiSolutionStatus.value = AISolutionStatus.Error(message)
                    }
                } else {
                    _aiSolutionStatus.value = AISolutionStatus.Error(
                        "Request failed: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting AI solution", e)
                _aiSolutionStatus.value = AISolutionStatus.Error(
                    "Error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    // Functions to navigate between assignments
    fun nextAssignment() {
        val current = _currentAssignmentIndex.value ?: 0
        val assignments = _assignments.value ?: emptyList()
        if (current < assignments.size - 1) {
            _currentAssignmentIndex.value = current + 1
        }
    }

    fun previousAssignment() {
        val current = _currentAssignmentIndex.value ?: 0
        if (current > 0) {
            _currentAssignmentIndex.value = current - 1
        }
    }

    fun goToAssignment(index: Int) {
        val assignments = _assignments.value ?: emptyList()
        if (index >= 0 && index < assignments.size) {
            _currentAssignmentIndex.value = index
        }
    }

    fun setAssignments(assignments: List<Assignment>) {
        _assignments.value = assignments
        _currentAssignmentIndex.value = 0
        _uploadStatus.value = UploadStatus.Success(imageUrl = null, imageId = null)
    }

    // Sealed classes for state
    sealed class UploadStatus {
        object Loading : UploadStatus()
        data class Success(val imageUrl: String?, val imageId: String?) : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }

    sealed class AISolutionStatus {
        object Loading : AISolutionStatus()
        data class Success(val solution: String) : AISolutionStatus()
        data class Error(val message: String) : AISolutionStatus()
        object PremiumRequired : AISolutionStatus()
    }

    companion object {
        private const val TAG = "ResultViewModel"
    }
}