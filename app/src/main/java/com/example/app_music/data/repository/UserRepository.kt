package com.example.app_music.data.repository

import android.util.Log
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.UserRepository
import com.example.app_music.domain.utils.RetrofitFactory
import okhttp3.MultipartBody
import retrofit2.Response

class UserRepository : UserRepository {
    override suspend fun getUserById(id: Long): Response<User> {
          return RetrofitFactory.userApi.getUserById(id);
    }
    override suspend fun updateUser(id: Long, user: User): Response<User> {
        Log.d("updateuserRepository", user.toString())
        return RetrofitFactory.userApi.updateUser(id, user)
    }

    override suspend fun uploadAvatar(id: Long, imageFile: MultipartBody.Part): Response<User> {
        return RetrofitFactory.userApi.uploadAvatar(id, imageFile)
    }
}