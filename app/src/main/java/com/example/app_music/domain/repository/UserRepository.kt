package com.example.app_music.domain.repository

import com.example.app_music.domain.model.User
import okhttp3.MultipartBody
import retrofit2.Response

interface UserRepository {
    suspend fun  getUserById(id : Long): Response<User>

    suspend fun updateUser(id: Long, user: User): Response<User>
    suspend fun uploadAvatar(id: Long, imageFile: MultipartBody.Part): Response<User>
    suspend fun deleteUserAccount(userId: Long): Response<Void>
}