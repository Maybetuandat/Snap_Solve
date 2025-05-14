package com.example.app_music.domain.repository

import com.example.app_music.data.model.Post
import retrofit2.Response

interface PostRepository {
    suspend fun getAllPosts(): Response<List<Post>>
    suspend fun getLatestPosts(): Response<List<Post>>
    suspend fun getPostsByTopic(topicId: Long): Response<List<Post>>
    suspend fun searchPosts(keyword: String): Response<List<Post>>
}