package com.example.app_music.domain.repository

import com.example.app_music.data.model.Comment
import com.example.app_music.data.model.Post
import retrofit2.Response

interface PostRepository {
    suspend fun getAllPosts(): Response<List<Post>>
    suspend fun getLatestPosts(): Response<List<Post>>
    suspend fun getPostsByTopic(topicId: Long): Response<List<Post>>
    suspend fun searchPosts(keyword: String): Response<List<Post>>
    suspend fun getPostById(postId: Long): Response<Post>
    suspend fun addComment(postId: Long, content: String, imageUrl: String?): Response<Comment>
    suspend fun likePost(postId: Long, userId: Long): Response<Post>
    suspend fun likeComment(commentId: Long): Response<Comment>
    suspend fun unlikePost(postId: Long, userId: Long): Response<Post>
}