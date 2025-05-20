package com.example.app_music.domain.repository

import com.example.app_music.domain.model.Comment
import retrofit2.Response

interface CommentRepository {
    suspend fun getRootCommentsByPostId(postId: Long): Response<List<Comment>>
    suspend fun getRepliesByParentCommentId(commentId: Long): Response<List<Comment>>
    suspend fun createComment(content: String, userId: Long, postId: Long, imagePaths: List<String>): Response<Comment>
    suspend fun createReply(content: String, userId: Long, parentCommentId: Long, imagePaths: List<String>): Response<Comment>
    suspend fun getCommentById(id: Long): Response<Comment>
    suspend fun countRootCommentsByPostId(postId: Long): Response<Long>
}