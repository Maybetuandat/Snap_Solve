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
import kotlinx.coroutines.launch
import java.time.LocalDate

class PostDetailViewModel : ViewModel() {
    private val repository = PostRepositoryImpl()
    private var postId: Long = 0 // Thêm biến để lưu ID bài viết

    // Thay đổi kiểu dữ liệu để chấp nhận null
    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _commentImageUri = MutableLiveData<Uri?>()
    val commentImageUri: LiveData<Uri?> = _commentImageUri

    // Load post details by ID
    fun loadPostDetails(postId: Long) {
        this.postId = postId // Lưu ID bài viết
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getPostById(postId)
                if (response.isSuccessful) {
                    val postData = response.body()
                    // Kiểm tra null trước khi gán
                    if (postData != null) {
                        _post.value = postData
                        _comments.value = postData.comment ?: emptyList()
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

    // Thêm phương thức để xử lý thích/bỏ thích bài viết
    fun toggleLikePost(userId: Long) {
        if (postId <= 0) {
            _error.value = "Invalid post ID"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Kiểm tra xem người dùng đã thích bài viết này chưa
                val currentPost = _post.value
                val hasUserLiked = currentPost?.react?.any { it.user.id == userId } ?: false

                val response = if (hasUserLiked) {
                    // Nếu đã thích, gọi API để bỏ thích
                    repository.unlikePost(postId, userId)
                } else {
                    // Nếu chưa thích, gọi API để thích
                    repository.likePost(postId, userId)
                }

                if (response.isSuccessful) {
                    // Cập nhật bài viết với dữ liệu mới từ server
                    val updatedPost = response.body()
                    // Kiểm tra null trước khi gán
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
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Set selected image for comment
    fun setCommentImage(uri: Uri?) {
        _commentImageUri.value = uri
    }

    // Post a new comment
    fun postComment(content: String, imageUri: Uri?) {
        if (content.isBlank() && imageUri == null) {
            _error.value = "Comment cannot be empty"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val currentPost = _post.value
                if (currentPost == null) {
                    _error.value = "Post is not loaded"
                    return@launch
                }

                // In a real app, you would upload the image first if imageUri is not null
                // Then send the comment with image URL to the server

                // For now, we'll simulate adding a comment locally
                val currentComments = _comments.value?.toMutableList() ?: mutableListOf()

                // Create a new comment (in a real app this would come from the API response)
                val newComment = Comment(
                    id = System.currentTimeMillis(), // Fake ID
                    content = content,
                    image = imageUri?.toString(), // In a real app this would be the uploaded image URL
                    createDate = LocalDate.now(),
                    user = currentPost.user // Use the current user
                )

                // Add to the list
                currentComments.add(0, newComment)
                _comments.postValue(currentComments)

                // Clear the comment image
                _commentImageUri.postValue(null)

                // In a real app, you would call the repository method here
                // val response = repository.addComment(postId, content, imageUrl)
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error posting comment", e)
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Like a comment
    fun likeComment(comment: Comment) {
        // In a real app, you would call the repository
        // For now, just log the action
        Log.d("PostDetailViewModel", "Like comment: ${comment.id}")
    }

    // Reply to a comment
    fun replyToComment(comment: Comment) {
        // In a real app, you would handle this accordingly
        // For now, just log the action
        Log.d("PostDetailViewModel", "Reply to comment: ${comment.id}")
    }
}