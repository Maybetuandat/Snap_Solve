package com.example.app_music.domain.usecase.user

import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.User
import okhttp3.MultipartBody
import retrofit2.Response

class UploadAvatarUseCase {
    private val repository = UserRepository()

    suspend operator fun invoke(userId: Long, imageFile: MultipartBody.Part): Response<User> {
        return repository.uploadAvatar(userId, imageFile)
    }
}