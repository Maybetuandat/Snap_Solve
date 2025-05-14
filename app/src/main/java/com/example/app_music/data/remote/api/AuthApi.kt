package com.example.app_music.data.remote.api


import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<AuthResponse>

    @POST("/api/auth/register")
    suspend fun register(@Body userCreate: User): Response<User>
}