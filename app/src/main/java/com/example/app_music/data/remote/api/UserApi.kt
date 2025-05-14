package com.example.app_music.data.remote.api

import com.example.app_music.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("/api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<UserDto>

    @PUT("/api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body userUpdateDto: UserDto
    ): Response<UserDto>
}