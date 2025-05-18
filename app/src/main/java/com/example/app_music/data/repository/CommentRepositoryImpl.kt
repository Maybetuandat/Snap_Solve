package com.example.app_music.data.repository

import com.example.app_music.domain.model.Comment
import com.example.app_music.domain.model.CreateCommentRequest
import com.example.app_music.domain.repository.CommentRepository
import com.example.app_music.domain.utils.RetrofitFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

class CommentRepositoryImpl : CommentRepository {

    override suspend fun getRootCommentsByPostId(postId: Long): Response<List<Comment>> {
        return RetrofitFactory.commentApi.getRootCommentsByPostId(postId)
    }

    override suspend fun getRepliesByParentCommentId(commentId: Long): Response<List<Comment>> {
        return RetrofitFactory.commentApi.getRepliesByParentCommentId(commentId)
    }

    override suspend fun createComment(content: String, userId: Long, postId: Long, imagePaths: List<String>): Response<Comment> {
        // Đầu tiên, upload các ảnh nếu có
        val imageUrls = if (imagePaths.isNotEmpty()) {
            uploadImages(imagePaths)
        } else {
            emptyList()
        }

        // Tạo request object
        val request = CreateCommentRequest(
            content = content,
            images = imageUrls.ifEmpty { null },
            userId = userId,
            postId = postId,
            parentCommentId = null // Comment gốc không có parent
        )

        return RetrofitFactory.commentApi.createComment(request)
    }

    override suspend fun createReply(content: String, userId: Long, parentCommentId: Long, imagePaths: List<String>): Response<Comment> {
        // Đầu tiên, upload các ảnh nếu có
        val imageUrls = if (imagePaths.isNotEmpty()) {
            uploadImages(imagePaths)
        } else {
            emptyList()
        }

        // Tạo request object
        val request = CreateCommentRequest(
            content = content,
            images = imageUrls.ifEmpty { null },
            userId = userId,
            postId = 0, // Sẽ được set từ parent comment ở backend
            parentCommentId = parentCommentId
        )

        return RetrofitFactory.commentApi.createReply(request)
    }

    override suspend fun getCommentById(id: Long): Response<Comment> {
        return RetrofitFactory.commentApi.getCommentById(id)
    }

    override suspend fun countRootCommentsByPostId(postId: Long): Response<Long> {
        return RetrofitFactory.commentApi.countRootCommentsByPostId(postId)
    }

    /**
     * Upload multiple images và trả về danh sách URL
     */
    private suspend fun uploadImages(imagePaths: List<String>): List<String> {
        try {
            // Chuyển đổi đường dẫn thành MultipartBody.Part
            val parts = imagePaths.map { path ->
                val file = File(path)
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("files", file.name, requestBody)
            }

            // Gọi API upload (sử dụng chung với PostApi)
            val response = RetrofitFactory.postApi.uploadMultipleFiles(parts)

            // Kiểm tra và trả về danh sách URL
            return if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it["fileUrl"] ?: "" }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Log error và trả về empty list
            e.printStackTrace()
            return emptyList()
        }
    }
}