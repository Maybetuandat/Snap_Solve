package com.example.app_music.data.remote.api

import com.example.app_music.data.model.Post
import retrofit2.Response
import retrofit2.http.GET
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
}