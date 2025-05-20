package com.example.app_music.domain.repository

import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.Post
import retrofit2.Response

interface PostRepository {
    suspend fun getAllPosts(): Response<List<Post>>
    suspend fun getLatestPosts(): Response<List<Post>>
    suspend fun getPostsByTopic(topicId: Long): Response<List<Post>>
    suspend fun searchPosts(keyword: String): Response<List<Post>>
    suspend fun getPostById(postId: Long): Response<Post>
    suspend fun createPost(title: String, content: String, userId: Long, topicIds: List<Long>, imagePaths: List<String>): Response<Post>
    suspend fun updatePost(
        postId: Long,
        title: String,
        content: String,
        userId: Long,
        topicIds: List<Long>,
        imagePaths: List<String>
    ): Response<Post>
    suspend fun likePost(postId: Long, userId: Long): Response<Post>
    suspend fun unlikePost(postId: Long, userId: Long): Response<Post>
    suspend fun getPostsByUserId(userId: Long): Response<List<Post>>
    suspend fun getLikedPostsByUserId(userId: Long): Response<List<Post>>
}