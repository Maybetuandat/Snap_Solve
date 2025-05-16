package com.example.app_music.presentation.feature.menu.profile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.User
import com.example.app_music.domain.usecase.GetUserUseCase
import com.example.app_music.domain.usecase.user.UpdateUserUseCase
import com.example.app_music.domain.usecase.user.UploadAvatarUseCase
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody


class ProfileEditViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val getUserUseCase = GetUserUseCase()
    private val updateUserUseCase = UpdateUserUseCase(userRepository)
    private val uploadAvatarUseCase = UploadAvatarUseCase()

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _updateResult = MutableLiveData<UpdateResult>()
    val updateResult: LiveData<UpdateResult> = _updateResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error


    private val _avatarUploaded = MutableLiveData<Boolean>()
    val avatarUploaded: LiveData<Boolean> = _avatarUploaded
    fun fetchUserData(userId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = getUserUseCase(userId)
                Log.d("profileViewmpdel", response.body().toString())
                if (response.isSuccessful && response.body() != null) {
                    _user.value = response.body()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUsername(userId: Long, newUsername: String) {
        updateUserField(userId) { currentUser ->
            currentUser.copy(username = newUsername)
        }
    }

    fun updateStatusMessage(userId: Long, newStatusMessage: String) {
        updateUserField(userId) { currentUser ->
            currentUser.copy(statusMessage = newStatusMessage)
        }
    }

    fun updateStudentInformation(userId: Long, newStudentInfo: String) {
        updateUserField(userId) { currentUser ->
            currentUser.copy(studentInformation = newStudentInfo)
        }
    }

    private fun updateUserField(userId: Long, updateFunction: (User) -> User) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val currentUser = _user.value
                Log.d("currentUser", currentUser.toString())
                if (currentUser != null) {
                    val updatedUser = updateFunction(currentUser)
                    Log.d("currentUser", updatedUser.toString())
                    val response = updateUserUseCase(userId, updatedUser)

                    if (response.isSuccessful && response.body() != null) {
                        _user.value = response.body()
                        Log.d("responseUser", _user.value.toString())
                        _updateResult.value = UpdateResult(true, "Update successful")
                    } else {
                        _updateResult.value = UpdateResult(false, "Failed to update: ${response.message()}")
                    }
                } else {
                    _updateResult.value = UpdateResult(false, "No user data available")
                }
            } catch (e: Exception) {
                _updateResult.value = UpdateResult(false, "Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun uploadAvatar(userId: Long, imageFile: File) {
        _isLoading.value = true
        viewModelScope.launch {
            try {

                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())


                val body = MultipartBody.Part.createFormData("avatar", imageFile.name, requestFile)


                val response = uploadAvatarUseCase(userId, body)

                if (response.isSuccessful) {
                    _user.value = response.body()
                    _avatarUploaded.value = true
                } else {
                    _error.value = "Error uploading avatar: ${response.code()} - ${response.message()}"
                    _avatarUploaded.value = false
                }
            } catch (e: Exception) {
                _error.value = "Error uploading avatar: ${e.message}"
                _avatarUploaded.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class UpdateResult(
    val isSuccess: Boolean,
    val message: String
)