package com.example.app_music.data.repository

import com.example.app_music.data.model.User
import com.example.app_music.domain.repository.UserRepository
import com.example.app_music.domain.utils.RetrofitInstance
import retrofit2.Response

class UserRepository : UserRepository {
    override suspend fun getUserById(id: Long): Response<User> {
          return RetrofitInstance.api.getUserById(id);
    }
}