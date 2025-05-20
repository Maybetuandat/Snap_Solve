package com.example.app_music.data.repository

import android.util.Log
import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.data.remote.api.UserApi
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.UserRepository
import com.example.app_music.domain.utils.RetrofitFactory
import okhttp3.MultipartBody
import retrofit2.Response

class UserRepository(
    private val apiService: UserApi,
) : UserRepository {
    override suspend fun getUserById(id: Long): Response<User> {
          return apiService.getUserById(id);
    }
    override suspend fun updateUser(id: Long, user: User): Response<User> {
        Log.d("updateuserRepository", user.toString())
        return apiService.updateUser(id, user)
    }

    override suspend fun uploadAvatar(id: Long, imageFile: MultipartBody.Part): Response<User> {
        return apiService.uploadAvatar(id, imageFile)
    }

    override suspend fun deleteUserAccount(userId: Long): Response<Void> {
        return apiService.deleteUser(userId)
    }
}