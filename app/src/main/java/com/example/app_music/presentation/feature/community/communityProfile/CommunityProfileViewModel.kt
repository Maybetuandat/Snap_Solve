package com.example.app_music.presentation.feature.community.communityProfile

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.model.User
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.launch

class CommunityProfileViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val userRepository = UserRepository(RetrofitFactory.userApi)

    private val _userInfo = MutableLiveData<User?>()
    val userInfo: LiveData<User?> = _userInfo

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _likedPosts = MutableLiveData<List<Post>>()
    val likedPosts: LiveData<List<Post>> = _likedPosts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Tải thông tin người dùng
    fun loadUserInfo(userId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = userRepository.getUserById(userId)
                if (response.isSuccessful) {
                    _userInfo.value = response.body()
                } else {
                    _error.value = "Không thể tải thông tin người dùng: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi khi tải thông tin người dùng", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tải bài đăng của người dùng
    fun loadUserPosts(userId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = postRepository.getPostsByUserId(userId)
                if (response.isSuccessful) {
                    _userPosts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Không thể tải bài đăng: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi khi tải bài đăng của người dùng", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tải bài đăng đã thích
    fun loadLikedPosts(userId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = postRepository.getLikedPostsByUserId(userId)
                if (response.isSuccessful) {
                    _likedPosts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Không thể tải bài đăng đã thích: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Lỗi khi tải bài đăng đã thích", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}