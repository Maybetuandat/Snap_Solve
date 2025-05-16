package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Topic
import retrofit2.Response
import retrofit2.http.GET

interface TopicApi {
    @GET("/api/topic")
    suspend fun getAllTopics(): Response<List<Topic>>
}