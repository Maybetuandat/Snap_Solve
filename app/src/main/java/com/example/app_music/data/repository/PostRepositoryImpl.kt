package com.example.app_music.data.repository

import android.util.Log
import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.CreatePostRequest
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.repository.PostRepository
import com.example.app_music.domain.utils.RetrofitFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

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

    override suspend fun createPost(title: String, content: String, userId: Long, topicIds: List<Long>, imagePaths: List<String>): Response<Post> {
        // Đầu tiên, upload các ảnh
        val imageUrls = if (imagePaths.isNotEmpty()) {
            uploadImages(imagePaths)
        } else {
            emptyList()
        }

        // Tạo request object sử dụng data class
        val request = CreatePostRequest(
            title = title,
            content = content,
            userId = userId,
            topicIds = topicIds,
            images = imageUrls.ifEmpty { null }
        )

        // Gọi API tạo bài đăng
        return RetrofitFactory.postApi.createPost(request)
    }

    override suspend fun updatePost(
        postId: Long,
        title: String,
        content: String,
        userId: Long,
        topicIds: List<Long>,
        imagePaths: List<String>
    ): Response<Post> {
        Log.d("PostRepositoryImpl", "updatePost called with:")
        Log.d("PostRepositoryImpl", "- Image paths: ${imagePaths.size}")
        imagePaths.forEachIndexed { index, path ->
            Log.d("PostRepositoryImpl", "  Image [$index]: $path")
        }

        // Upload tất cả ảnh (cả cũ và mới) - giống logic createPost
        val imageUrls = if (imagePaths.isNotEmpty()) {
            // Tách ảnh cũ (URL) và ảnh mới (file path)
            val existingUrls = imagePaths.filter { it.startsWith("/images/") || it.startsWith("http") }
            val newPaths = imagePaths.filter { !it.startsWith("/images/") && !it.startsWith("http") }

            Log.d("PostRepositoryImpl", "Existing URLs: ${existingUrls.size}")
            Log.d("PostRepositoryImpl", "New paths to upload: ${newPaths.size}")

            // Upload chỉ ảnh mới
            val newUrls = if (newPaths.isNotEmpty()) {
                uploadImages(newPaths)
            } else {
                emptyList()
            }

            // Kết hợp ảnh cũ và ảnh mới
            existingUrls + newUrls
        } else {
            emptyList()
        }

        Log.d("PostRepositoryImpl", "Final image URLs: ${imageUrls.size}")

        // Tạo request object - giống createPost
        val request = CreatePostRequest(
            title = title,
            content = content,
            userId = userId,
            topicIds = topicIds,
            images = imageUrls.ifEmpty { null }
        )

        Log.d("PostRepositoryImpl", "Sending update request with ${request.images?.size ?: 0} images")

        // Gọi API cập nhật bài đăng
        return RetrofitFactory.postApi.updatePost(postId, request)
    }

    private suspend fun uploadImages(imagePaths: List<String>): List<String> {
        // Chuyển đổi đường dẫn thành MultipartBody.Part
        val parts = imagePaths.map { path ->
            val file = File(path)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, requestBody)
        }

        // Gọi API upload
        val response = RetrofitFactory.postApi.uploadMultipleFiles(parts)

        // Kiểm tra và trả về danh sách URL
        return if (response.isSuccessful && response.body() != null) {
            response.body()!!.map { it["fileUrl"] ?: "" }
        } else {
            emptyList()
        }
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