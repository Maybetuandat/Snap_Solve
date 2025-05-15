package com.example.app_music.data.remote.api

import com.example.app_music.data.model.Post
import com.example.app_music.data.model.Topic
import com.example.app_music.data.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    @GET("/api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): Response<User>

}