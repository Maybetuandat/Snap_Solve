package com.example.app_music.data.repository

import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.UserRepository
import com.example.app_music.domain.utils.RetrofitClient
import retrofit2.Response
import retrofit2.Retrofit

class UserRepository : UserRepository {
    override suspend fun getUserById(id: Long): Response<User> {
          return RetrofitClient.userApi.getUserById(id);
    }
}