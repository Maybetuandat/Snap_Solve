//package com.example.app_music.presentation.feature.auth
//
//import android.content.Context
//import android.util.Log
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.app_music.domain.repository.AuthRepository
//
//
//import kotlinx.coroutines.launch
//import java.time.LocalDate
//
//class AuthViewModel : ViewModel() {
//    private val repository = AuthRepository()
//
//    // For login
//    private val _loginResult = MutableLiveData<LoginResult>()
//    val loginResult: LiveData<LoginResult> = _loginResult
//
//    // For registration
//    private val _registerResult = MutableLiveData<RegisterResult>()
//    val registerResult: LiveData<RegisterResult> = _registerResult
//
//    // For profile completion
//    private val _profileUpdateResult = MutableLiveData<ProfileUpdateResult>()
//    val profileUpdateResult: LiveData<ProfileUpdateResult> = _profileUpdateResult
//
//    // Loading state
//    private val _isLoading = MutableLiveData<Boolean>()
//    val isLoading: LiveData<Boolean> = _isLoading
//
//    // Store registered user temporarily
//    private var registeredUser: User? = null
//
//    fun login(username: String, password: String) {
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                val response = repository.login(username, password)
//                if (response.isSuccessful && response.body() != null) {
//                    val authResponse = response.body()!!
//                    _loginResult.value = LoginResult(
//                        isSuccess = true,
//                        token = authResponse.token,
//                        user = authResponse.user
//                    )
//                } else {
//                    _loginResult.value = LoginResult(
//                        isSuccess = false,
//                        errorMessage = "Invalid username or password"
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("AuthViewModel", "Login error: ${e.message}")
//                _loginResult.value = LoginResult(
//                    isSuccess = false,
//                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
//                )
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun register(username: String, email: String, phoneNumber: String, password: String) {
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                val response = repository.register(username, email, phoneNumber, password)
//                if (response.isSuccessful && response.body() != null) {
//                    val user = response.body()!!
//                    _registerResult.value = RegisterResult(
//                        isSuccess = true,
//                        user = user
//                    )
//                } else {
//                    val errorBody = response.errorBody()?.string() ?: "Registration failed"
//                    _registerResult.value = RegisterResult(
//                        isSuccess = false,
//                        errorMessage = errorBody
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("AuthViewModel", "Register error: ${e.message}")
//                _registerResult.value = RegisterResult(
//                    isSuccess = false,
//                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
//                )
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun completeProfile(firstName: String, lastName: String, dob: String) {
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                val userId = registeredUser?.id ?: return@launch
//
//                val response = repository.updateProfile(
//                    userId,
//                    firstName,
//                    lastName,
//                    LocalDate.parse(dob)
//                )
//
//                if (response.isSuccessful && response.body() != null) {
//                    // Now that profile is complete, perform login to get token
//                    val loginResponse = repository.login(
//                        registeredUser?.username ?: "",
//                        repository.getStoredPassword() // You need to temporarily store this
//                    )
//
//                    if (loginResponse.isSuccessful && loginResponse.body() != null) {
//                        val authResponse = loginResponse.body()!!
//                        _profileUpdateResult.value = ProfileUpdateResult(
//                            isSuccess = true,
//                            token = authResponse.token,
//                            user = authResponse.user
//                        )
//                    } else {
//                        _profileUpdateResult.value = ProfileUpdateResult(
//                            isSuccess = false,
//                            errorMessage = "Could not complete authentication"
//                        )
//                    }
//                } else {
//                    val errorBody = response.errorBody()?.string() ?: "Profile update failed"
//                    _profileUpdateResult.value = ProfileUpdateResult(
//                        isSuccess = false,
//                        errorMessage = errorBody
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("AuthViewModel", "Profile completion error: ${e.message}")
//                _profileUpdateResult.value = ProfileUpdateResult(
//                    isSuccess = false,
//                    errorMessage = "Error: ${e.message ?: "Unknown error"}"
//                )
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
//
//    fun setRegisteredUser(user: User) {
//        registeredUser = user
//    }
//
//    fun saveUserSession(context: Context, token: String, user: User) {
//        repository.saveAuthToken(context, token)
//        repository.saveUserData(context, user)
//    }
//}
//
//// Result data classes
//data class LoginResult(
//    val isSuccess: Boolean = false,
//    val token: String = "",
//    val user: User? = null,
//    val errorMessage: String = ""
//)
//
//data class RegisterResult(
//    val isSuccess: Boolean = false,
//    val user: User? = null,
//    val errorMessage: String = ""
//)
//
//data class ProfileUpdateResult(
//    val isSuccess: Boolean = false,
//    val token: String = "",
//    val user: User? = null,
//    val errorMessage: String = ""
//)