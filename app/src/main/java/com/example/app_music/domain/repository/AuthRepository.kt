package com.example.app_music.domain.repository

import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import retrofit2.Response
import java.time.LocalDate

interface AuthRepository {
    suspend fun login(username: String, password: String): Response<AuthResponse>

    suspend fun register(
       user : User
    ): Response<User>






}