package com.example.app_music.domain.repository

import com.example.app_music.domain.model.User
import retrofit2.Response

interface UserRepository {
    suspend fun  getUserById(id : Long): Response<User>
}