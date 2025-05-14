package com.example.app_music.presentation.feature.community

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.model.Post
import com.example.app_music.data.model.Topic
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.TopicRepositoryImpl
import kotlinx.coroutines.launch

class CommunityViewModel : ViewModel() {
    private val repository = PostRepositoryImpl()
    private val topicRepository = TopicRepositoryImpl()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _topics = MutableLiveData<List<Topic>>()
    val topics: LiveData<List<Topic>> = _topics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentTopicId: Long? = null

    init {
        loadTopics()
    }

    // Load topics from API
    private fun loadTopics() {
        viewModelScope.launch {
            try {
                val response = topicRepository.getAllTopics()
                if (response.isSuccessful) {
                    _topics.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Lỗi tải chủ đề: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Error loading topics", e)
                _error.value = "Lỗi kết nối khi tải chủ đề: ${e.message}"
            }
        }
    }

    // Hàm gọi khi fragment được tạo hoặc khi người dùng làm mới dữ liệu
    fun loadPosts() {
        if (currentTopicId != null) {
            loadPostsByTopic(currentTopicId!!)
        } else {
            loadLatestPosts()
        }
    }

    // Lấy danh sách bài post mới nhất
    fun loadLatestPosts() {
        _isLoading.value = true
        currentTopicId = null

        viewModelScope.launch {
            try {
                val response = repository.getLatestPosts()
                if (response.isSuccessful) {
                    _posts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Lỗi: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Error loading posts", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Lấy danh sách bài post theo chủ đề
    fun loadPostsByTopic(topicId: Long) {
        _isLoading.value = true
        currentTopicId = topicId

        viewModelScope.launch {
            try {
                val response = repository.getPostsByTopic(topicId)
                if (response.isSuccessful) {
                    _posts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Lỗi: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Error loading posts by topic", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tìm kiếm bài post
    fun searchPosts(keyword: String) {
        if (keyword.isBlank()) {
            loadPosts()
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = repository.searchPosts(keyword)
                if (response.isSuccessful) {
                    _posts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Lỗi: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("CommunityViewModel", "Error searching posts", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}