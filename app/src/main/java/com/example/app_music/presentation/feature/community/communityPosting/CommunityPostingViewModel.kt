package com.example.app_music.presentation.feature.community.communityPosting

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.TopicRepositoryImpl
import com.example.app_music.domain.model.Topic
import kotlinx.coroutines.launch

class CommunityPostingViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val topicRepository = TopicRepositoryImpl()

    private val _topics = MutableLiveData<List<Topic>>()
    val topics: LiveData<List<Topic>> = _topics

    private val _isPosting = MutableLiveData<Boolean>()
    val isPosting: LiveData<Boolean> = _isPosting

    private val _postResult = MutableLiveData<Boolean>()
    val postResult: LiveData<Boolean> = _postResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Tải danh sách topic từ API
    fun loadTopics() {
        viewModelScope.launch {
            try {
                val response = topicRepository.getAllTopics()
                if (response.isSuccessful) {
                    _topics.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Không thể tải danh sách chủ đề: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("CommunityPostingVM", "Error loading topics", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    // Đăng bài viết mới
    fun createPost(title: String, content: String, userId: Long, topicIds: List<Long>, imagePaths: List<String>) {
        _isPosting.value = true

        viewModelScope.launch {
            try {
                // Gọi repository để tạo bài đăng
                val response = postRepository.createPost(title, content, userId, topicIds, imagePaths)

                if (response.isSuccessful) {
                    _postResult.value = true
                } else {
                    _error.value = "Không thể đăng bài: ${response.code()} - ${response.message()}"
                    _postResult.value = false
                }
            } catch (e: Exception) {
                Log.e("CommunityPostingVM", "Error creating post", e)
                _error.value = "Lỗi kết nối: ${e.message}"
                _postResult.value = false
            } finally {
                _isPosting.value = false
            }
        }
    }
}