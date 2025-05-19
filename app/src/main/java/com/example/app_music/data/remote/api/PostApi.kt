package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.CreatePostRequest
import com.example.app_music.domain.model.Post
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @POST("/api/post")
    suspend fun createPost(@Body request: CreatePostRequest): Response<Post>

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

    @GET("/api/post/user/{userId}")
    suspend fun getPostsByUserId(@Path("userId") userId: Long): Response<List<Post>>

    @GET("/api/post/liked/{userId}")
    suspend fun getLikedPostsByUserId(@Path("userId") userId: Long): Response<List<Post>>

    @Multipart
    @POST("/api/files/upload-multiple")
    suspend fun uploadMultipleFiles(
        @Part files: List<MultipartBody.Part>
    ): Response<List<Map<String, String>>>
}