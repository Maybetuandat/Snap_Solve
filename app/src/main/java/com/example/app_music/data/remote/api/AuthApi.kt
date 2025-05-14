package com.example.app_music.data.remote.api

import com.example.app_music.data.model.AuthResponseDto
import com.example.app_music.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<AuthResponseDto>

    @POST("/api/auth/register")
    suspend fun register(@Body userCreateDto: UserDto): Response<UserDto>
}