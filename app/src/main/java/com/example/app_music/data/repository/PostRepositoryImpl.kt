package com.example.app_music.data.repository

import com.example.app_music.data.model.Comment
import com.example.app_music.data.model.Post
import com.example.app_music.domain.repository.PostRepository
import com.example.app_music.domain.utils.RetrofitClient
import retrofit2.Response

class PostRepositoryImpl : PostRepository {
    override suspend fun getAllPosts(): Response<List<Post>> {
        return RetrofitClient.postApi.getAllPosts()
    }

    override suspend fun getLatestPosts(): Response<List<Post>> {
        return RetrofitClient.postApi.getLatestPosts()
    }

    override suspend fun getPostsByTopic(topicId: Long): Response<List<Post>> {
        return RetrofitClient.postApi.getPostsByTopic(topicId)
    }

    override suspend fun searchPosts(keyword: String): Response<List<Post>> {
        return RetrofitClient.postApi.searchPosts(keyword)
    }

    override suspend fun getPostById(postId: Long): Response<Post> {
        return RetrofitClient.postApi.getPostById(postId)
    }

    override suspend fun addComment(postId: Long, content: String, imageUrl: String?): Response<Comment> {
        val commentData = mutableMapOf<String, Any>(
            "content" to content
        )

        if (imageUrl != null) {
            commentData["image"] = imageUrl
        }

        return RetrofitClient.postApi.addComment(postId, commentData)
    }

    override suspend fun likePost(postId: Long): Response<Post> {
        return RetrofitClient.postApi.likePost(postId)
    }

    override suspend fun likeComment(commentId: Long): Response<Comment> {
        return RetrofitClient.postApi.likeComment(commentId)
    }
}