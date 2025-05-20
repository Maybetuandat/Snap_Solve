package com.example.app_music.presentation.feature.community.communityPostDetail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.repository.CommentRepository
import com.example.app_music.data.repository.CommentRepositoryImpl
import kotlinx.coroutines.launch

class CommentReplyViewModel : ViewModel() {
    private val repository: CommentRepository = CommentRepositoryImpl()

    private val _parentComment = MutableLiveData<Comment?>()
    val parentComment: LiveData<Comment?> = _parentComment

    private val _replies = MutableLiveData<List<Comment>>()
    val replies: LiveData<List<Comment>> = _replies

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _replyResult = MutableLiveData<Boolean>()
    val replyResult: LiveData<Boolean> = _replyResult

    // Tải thông tin comment và các replies
    fun loadCommentWithReplies(commentId: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Tải thông tin parent comment
                val commentResponse = repository.getCommentById(commentId)
                if (commentResponse.isSuccessful) {
                    _parentComment.value = commentResponse.body()
                } else {
                    _error.value = "Không thể tải thông tin comment: ${commentResponse.code()}"
                }

                // Tải danh sách replies
                loadReplies(commentId)
            } catch (e: Exception) {
                Log.e("CommentReplyViewModel", "Error loading comment with replies", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Tải danh sách replies
    fun loadReplies(commentId: Long) {
        viewModelScope.launch {
            try {
                val repliesResponse = repository.getRepliesByParentCommentId(commentId)
                if (repliesResponse.isSuccessful) {
                    _replies.value = repliesResponse.body() ?: emptyList()
                } else {
                    _error.value = "Không thể tải danh sách trả lời: ${repliesResponse.code()}"
                }
            } catch (e: Exception) {
                Log.e("CommentReplyViewModel", "Error loading replies", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    // Tạo reply mới
    fun createReply(content: String, userId: Long, parentCommentId: Long, imagePaths: List<String>) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.createReply(content, userId, parentCommentId, imagePaths)
                if (response.isSuccessful) {
                    _replyResult.value = true
                } else {
                    _error.value = "Không thể gửi trả lời: ${response.code()} - ${response.message()}"
                    _replyResult.value = false
                }
            } catch (e: Exception) {
                Log.e("CommentReplyViewModel", "Error creating reply", e)
                _error.value = "Lỗi kết nối: ${e.message}"
                _replyResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}