package com.example.app_music.data.repository

import com.example.app_music.domain.model.ImageUploadResponse
import com.example.app_music.domain.utils.RetrofitFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class ImageRepository {
    private val imageUploadApi = RetrofitFactory.imageUploadApi

    /**
     * Upload an image file to the server
     */
    suspend fun uploadImage(imageFile: File, description: String = "Uploaded image"): Response<ImageUploadResponse> {
        // Create request body for the file
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

        // Create MultipartBody.Part with file name
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            imageFile.name,
            requestFile
        )

        // Create request body for description
        val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())

        // Make the network request
        return imageUploadApi.uploadImage(imagePart, descriptionBody)
    }
}