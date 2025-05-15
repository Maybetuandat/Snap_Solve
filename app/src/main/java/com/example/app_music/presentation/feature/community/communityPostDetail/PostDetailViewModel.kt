package com.example.app_music.presentation.feature.community.communityPostDetail

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.model.Comment
import com.example.app_music.data.model.Post
import com.example.app_music.data.repository.PostRepositoryImpl
import kotlinx.coroutines.launch
import java.time.LocalDate

class PostDetailViewModel : ViewModel() {
    private val repository = PostRepositoryImpl()

    private val _post = MutableLiveData<Post>()
    val post: LiveData<Post> = _post

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
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.getPostById(postId)
                if (response.isSuccessful) {
                    val postData = response.body()
                    if (postData != null) {
                        _post.value = postData
                        _comments.value = postData.comment
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
                val postId = _post.value?.id ?: return@launch

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
                    user = _post.value?.user ?: return@launch // Use the current user
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

    // Like the post
    fun likePost() {
        viewModelScope.launch {
            try {
                val postId = _post.value?.id ?: return@launch

                // In a real app, you would call the repository
                // val response = repository.likePost(postId)

                // For now, just update the UI
                _post.value?.let { currentPost ->
                    // Create a new react list with one more like
                    val updatedReacts = currentPost.react.toMutableList()
                    // Add a new like (in a real app this would be handled properly)

                    // Update the post
                    _post.postValue(currentPost.copy(react = updatedReacts))
                }
            } catch (e: Exception) {
                Log.e("PostDetailViewModel", "Error liking post", e)
                _error.value = "Error: ${e.message}"
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