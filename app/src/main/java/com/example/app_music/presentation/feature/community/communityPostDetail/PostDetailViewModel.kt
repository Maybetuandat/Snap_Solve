package com.example.app_music.presentation.feature.community.communityPostDetail

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.Post
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.CommentRepositoryImpl
import kotlinx.coroutines.launch

class PostDetailViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val commentRepository = CommentRepositoryImpl()
    private var postId: Long = 0

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _commentImageUris = MutableLiveData<List<Uri>>()
    val commentImageUris: LiveData<List<Uri>> = _commentImageUris

    private val _commentResult = MutableLiveData<Boolean>()
    val commentResult: LiveData<Boolean> = _commentResult

    // Load post details by ID
    fun loadPostDetails(postId: Long) {
        this.postId = postId
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = postRepository.getPostById(postId)
                if (response.isSuccessful) {
                    val postData = response.body()
                    if (postData != null) {
                        _post.value = postData
                        // Tải comment riêng biệt
                        loadComments(postId)
                    } else {
                        _error.value = "Post data is empty"
                    }
                } else {
                    _error.value = "Error loading post: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error loading post details", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tải danh sách comment gốc của bài viết
    private fun loadComments(postId: Long) {
        viewModelScope.launch {
            try {
                val response = commentRepository.getRootCommentsByPostId(postId)
                if (response.isSuccessful) {

                    val comments = response.body() ?: emptyList()

                    // Debug: Log toàn bộ response
                    Log.d("PostDetailViewModel", "Raw response: ${response.raw()}")
                    Log.d("PostDetailViewModel", "Comments received: ${comments.size}")

                    // Debug: Log từng comment
                    comments.forEach { comment ->
                        Log.d("PostDetailViewModel",
                            "Comment ID: ${comment.id}, replyCount: ${comment.replyCount}, content: ${comment.content}")
                    }
                    _comments.value = response.body() ?: emptyList()

                } else {
                    _error.value = "Error loading comments: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error loading comments", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    // Thêm phương thức để xử lý thích/bỏ thích bài viết
    fun toggleLikePost(userId: Long) {
        if (postId <= 0) {
            _error.value = "Invalid post ID"
            return
        }

        viewModelScope.launch {
            try {
                val currentPost = _post.value
                val hasUserLiked = currentPost?.react?.any { it.user.id == userId } ?: false

                val response = if (hasUserLiked) {
                    postRepository.unlikePost(postId, userId)
                } else {
                    postRepository.likePost(postId, userId)
                }

                if (response.isSuccessful) {
                    val updatedPost = response.body()
                    if (updatedPost != null) {
                        _post.postValue(updatedPost)
                    } else {
                        Log.e("PostDetailViewModel", "Updated post is null")
                    }
                } else {
                    _error.value = "Error toggling like: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error toggling like", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    // Set selected images for comment
    fun setCommentImages(uris: List<Uri>) {
        _commentImageUris.value = uris
    }

    // Post a new comment
    fun postComment(content: String, userId: Long, imagePaths: List<String>) {
        if (content.isBlank() && imagePaths.isEmpty()) {
            _error.value = "Comment cannot be empty"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = commentRepository.createComment(content, userId, postId, imagePaths)
                if (response.isSuccessful) {
                    _commentResult.value = true
                    // Tải lại danh sách comment sau khi đăng thành công
                    loadComments(postId)
                    // Xóa danh sách ảnh đã chọn
                    _commentImageUris.value = emptyList()
                } else {
                    _error.value = "Error posting comment: ${response.code()} - ${response.message()}"
                    _commentResult.value = false
                }
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error posting comment", e)
                _error.value = "Error: ${e.message}"
                _commentResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Xóa ảnh khỏi danh sách ảnh comment
    fun removeCommentImage(index: Int) {
        val currentImages = _commentImageUris.value?.toMutableList() ?: mutableListOf()
        if (index >= 0 && index < currentImages.size) {
            currentImages.removeAt(index)
            _commentImageUris.value = currentImages
        }
    }

    // Thêm ảnh vào danh sách ảnh comment
    fun addCommentImages(uris: List<Uri>) {
        val currentImages = _commentImageUris.value?.toMutableList() ?: mutableListOf()
        currentImages.addAll(uris)
        _commentImageUris.value = currentImages
    }
}