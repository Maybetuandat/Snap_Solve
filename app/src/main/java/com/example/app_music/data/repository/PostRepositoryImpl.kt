package com.example.app_music.data.repository

import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.repository.PostRepository
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class PostRepositoryImpl : PostRepository {
    override suspend fun getAllPosts(): Response<List<Post>> {
        return RetrofitFactory.postApi.getAllPosts()
    }

    override suspend fun getLatestPosts(): Response<List<Post>> {
        return RetrofitFactory.postApi.getLatestPosts()
    }

    override suspend fun getPostsByTopic(topicId: Long): Response<List<Post>> {
        return RetrofitFactory.postApi.getPostsByTopic(topicId)
    }

    override suspend fun searchPosts(keyword: String): Response<List<Post>> {
        return RetrofitFactory.postApi.searchPosts(keyword)
    }

    override suspend fun getPostById(postId: Long): Response<Post> {
        return RetrofitFactory.postApi.getPostById(postId)
    }

    override suspend fun addComment(postId: Long, content: String, imageUrl: String?): Response<Comment> {
        val commentData = mutableMapOf<String, Any>(
            "content" to content
        )

        if (imageUrl != null) {
            commentData["image"] = imageUrl
        }

        return RetrofitFactory.postApi.addComment(postId, commentData)
    }

    override suspend fun likeComment(commentId: Long): Response<Comment> {
        return RetrofitFactory.postApi.likeComment(commentId)
    }

    override suspend fun likePost(postId: Long, userId: Long): Response<Post> {
        return RetrofitFactory.postApi.likePost(postId, userId)
    }

    override suspend fun unlikePost(postId: Long, userId: Long): Response<Post> {
        return RetrofitFactory.postApi.unlikePost(postId, userId)
    }

    override suspend fun getPostsByUserId(userId: Long): Response<List<Post>> {
        return RetrofitFactory.postApi.getPostsByUserId(userId)
    }

    override suspend fun getLikedPostsByUserId(userId: Long): Response<List<Post>> {
        return RetrofitFactory.postApi.getLikedPostsByUserId(userId)
    }
}