package com.example.app_music.presentation.feature.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.database.AppDatabase
import com.example.app_music.data.local.database.entity.UserCredentials
import com.example.app_music.data.repository.AuthRepository
import com.example.app_music.domain.model.User
import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(RetrofitFactory.authApi)
    private val database = AppDatabase.getDatabase(application)
    private val credentialsDao = database.userCredentialsDao()


    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult


    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private val _autoLoginCheckComplete = MutableLiveData<Boolean>()
    val autoLoginCheckComplete: LiveData<Boolean> = _autoLoginCheckComplete

    private var registeredUser: User? = null


    private fun saveUserCredentials(username: String, password: String, rememberMe: Boolean) {
        viewModelScope.launch {
            if (rememberMe) {

                credentialsDao.deleteAllCredentials()


                credentialsDao.saveCredentials(
                    UserCredentials(
                        username = username,
                        password = password,
                        isRemembered = true
                    )
                )
                Log.d("AuthViewModel", "Đã lưu thông tin đăng nhập: $username")
            } else {

                credentialsDao.deleteAllCredentials()
                Log.d("AuthViewModel", "Đã xóa thông tin đăng nhập đã lưu")
            }
        }
    }


    fun checkSavedCredentials() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val savedCredentials = credentialsDao.getRememberedCredentials()
                if (savedCredentials != null) {
                    Log.d("AuthViewModel", "Tìm thấy thông tin đăng nhập đã lưu: ${savedCredentials.username}")

                    login(savedCredentials.username, savedCredentials.password, true)
                } else {
                    Log.d("AuthViewModel", "Không tìm thấy thông tin đăng nhập đã lưu")
                    _autoLoginCheckComplete.value = true
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi khi kiểm tra thông tin đăng nhập đã lưu: ${e.message}")
                _autoLoginCheckComplete.value = true
                _isLoading.value = false
            }
        }
    }


    fun login(username: String, password: String, rememberMe: Boolean = false) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.login(username, password)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!


                    saveUserCredentials(username, password, rememberMe)

                    _loginResult.value = LoginResult(
                        isSuccess = true,
                        token = authResponse.token,
                        user = authResponse.user
                    )
                } else {
                    _loginResult.value = LoginResult(
                        isSuccess = false,
                        errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi đăng nhập: ${e.message}")
                _loginResult.value = LoginResult(
                    isSuccess = false,
                    errorMessage = "Lỗi: ${e.message ?: "Không xác định"}"
                )
            } finally {
                _isLoading.value = false
                _autoLoginCheckComplete.value = true
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
                    val errorBody = response.errorBody()?.string() ?: "Đăng ký thất bại"
                    _registerResult.value = RegisterResult(
                        isSuccess = false,
                        errorMessage = errorBody
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Lỗi đăng ký: ${e.message}")
                _registerResult.value = RegisterResult(
                    isSuccess = false,
                    errorMessage = "Lỗi: ${e.message ?: "Không xác định"}"
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