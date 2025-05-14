package com.example.app_music.data.repository

import com.example.app_music.data.model.Post
import com.example.app_music.domain.repository.PostRepository
import com.example.app_music.domain.utils.RetrofitInstance
import retrofit2.Response

class PostRepositoryImpl : PostRepository {
    override suspend fun getAllPosts(): Response<List<Post>> {
        return RetrofitInstance.api.getAllPosts()
    }

    override suspend fun getLatestPosts(): Response<List<Post>> {
        return RetrofitInstance.api.getLatestPosts()
    }

    override suspend fun getPostsByTopic(topicId: Long): Response<List<Post>> {
        return RetrofitInstance.api.getPostsByTopic(topicId)
    }

    override suspend fun searchPosts(keyword: String): Response<List<Post>> {
        return RetrofitInstance.api.searchPosts(keyword)
    }
}