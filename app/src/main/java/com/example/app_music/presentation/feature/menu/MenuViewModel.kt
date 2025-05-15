package com.example.app_music.presentation.feature.menu

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.User
import com.example.app_music.domain.usecase.GetUserUseCase
import kotlinx.coroutines.launch

class MenuViewModel : ViewModel() {
    private val getUserUseCase = GetUserUseCase()

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchUserData(userId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = getUserUseCase(userId)
                handleUserResponse(response)
            } catch (e: Exception) {
                Log.e("MenuViewModel", e.toString())
                _error.value = "Error fetching user data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleUserResponse(response: retrofit2.Response<User>) {
        if (response.isSuccessful) {
            Log.d("menuviewmodel", response.body().toString())
            response.body()?.let {
                _user.value = it
            } ?: run {
                _error.value = "User data is empty"
            }
        } else {
            _error.value = "Error: ${response.code()} - ${response.message()}"
        }
    }
}