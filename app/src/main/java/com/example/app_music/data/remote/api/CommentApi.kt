package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.CreateCommentRequest
import retrofit2.Response
import retrofit2.http.*

interface CommentApi {
    @GET("/api/comment/post/{postId}")
    suspend fun getRootCommentsByPostId(@Path("postId") postId: Long): Response<List<Comment>>

    @GET("/api/comment/{commentId}/replies")
    suspend fun getRepliesByParentCommentId(@Path("commentId") commentId: Long): Response<List<Comment>>

    @POST("/api/comment")
    suspend fun createComment(@Body request: CreateCommentRequest): Response<Comment>

    @POST("/api/comment/reply")
    suspend fun createReply(@Body request: CreateCommentRequest): Response<Comment>

    @GET("/api/comment/{id}")
    suspend fun getCommentById(@Path("id") id: Long): Response<Comment>

    @GET("/api/comment/post/{postId}/count")
    suspend fun countRootCommentsByPostId(@Path("postId") postId: Long): Response<Long>
}