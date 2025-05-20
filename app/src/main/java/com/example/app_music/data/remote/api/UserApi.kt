package com.example.app_music.data.remote.api


import com.example.app_music.domain.model.User
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApi {
    @GET("/api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>

    @PUT("/api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body userUpdateDto: User
    ): Response<User>
    @Multipart
    @POST("/api/users/{id}/avatar")
    suspend fun uploadAvatar(
        @Path("id") id: Long,
        @Part avatar: MultipartBody.Part
    ): Response<User>


    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Long): Response<Void>
}