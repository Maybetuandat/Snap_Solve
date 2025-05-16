package com.example.app_music.data.remote.api

import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Image Upload API interface to handle image upload operations
 */
interface ImageUploadApi {
    @Multipart
    @POST("/api/images/upload")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Response<ImageUploadResponse>
}

/**
 * Response data class for image upload
 */
data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val imageUrl: String? = null,
    val imageId: String? = null
)