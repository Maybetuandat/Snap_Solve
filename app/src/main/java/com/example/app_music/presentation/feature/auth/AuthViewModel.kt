package com.example.app_music.presentation.feature.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.AuthRepository
import com.example.app_music.domain.model.User
import com.example.app_music.domain.utils.RetrofitFactory


import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private final val repository = AuthRepository(RetrofitFactory.authApi)

    // For login
    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    // For registration
    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult




    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private var registeredUser: User? = null

    fun login(username: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.login(username, password)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    _loginResult.value = LoginResult(
                        isSuccess = true,
                        token = authResponse.token,
                        user = authResponse.user
                    )
                } else {
                    _loginResult.value = LoginResult(
                        isSuccess = false,
                        errorMessage = "Invalid username or password"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Login error: ${e.message}")
                _loginResult.value = LoginResult(
                    isSuccess = false,
                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, email: String, phoneNumber: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val createUser = User.builder()
                    .email(email)
                    .username(username)
                    .phoneNumber(phoneNumber)
                    .password(password)
                    .build()
                val response = repository.register(createUser)
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    _registerResult.value = RegisterResult(
                        isSuccess = true,
                        user = user
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Registration failed"
                    _registerResult.value = RegisterResult(
                        isSuccess = false,
                        errorMessage = errorBody
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Register error: ${e.message}")
                _registerResult.value = RegisterResult(
                    isSuccess = false,
                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun setRegisteredUser(user: User?) {
        registeredUser = user
    }


}

// Result data classes
data class LoginResult(
    val isSuccess: Boolean = false,
    val token: String = "",
    val user: User? = null,
    val errorMessage: String = ""
)

data class RegisterResult(
    val isSuccess: Boolean = false,
    val user: User? = null,
    val errorMessage: String = ""
)

