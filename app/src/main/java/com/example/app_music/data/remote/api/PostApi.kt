package com.example.app_music.data.remote.api

import com.example.app_music.data.model.Comment
import com.example.app_music.data.model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PostApi {
    @GET("/api/post")
    suspend fun getAllPosts(): Response<List<Post>>

    @GET("/api/post/latest")
    suspend fun getLatestPosts(): Response<List<Post>>

    @GET("/api/post/topic/{topicId}")
    suspend fun getPostsByTopic(@Path("topicId") topicId: Long): Response<List<Post>>

    @GET("/api/post/search")
    suspend fun searchPosts(@Query("keyword") keyword: String): Response<List<Post>>

    @GET("/api/post/{id}")
    suspend fun getPostById(@Path("id") postId: Long): Response<Post>

    @POST("/api/post/{id}/comment")
    suspend fun addComment(
        @Path("id") postId: Long,
        @Body commentData: Map<String, Any>
    ): Response<Comment>

    @POST("/api/post/{id}/like")
    suspend fun likePost(
        @Path("id") postId: Long,
        @Query("userId") userId: Long
    ): Response<Post>

    @POST("/api/post/{id}/unlike")
    suspend fun unlikePost(
        @Path("id") postId: Long,
        @Query("userId") userId: Long
    ): Response<Post>

    @POST("/api/comment/{id}/like")
    suspend fun likeComment(@Path("id") commentId: Long): Response<Comment>

}