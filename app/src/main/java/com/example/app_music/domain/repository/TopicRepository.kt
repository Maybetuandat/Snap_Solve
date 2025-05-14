package com.example.app_music.domain.repository

import com.example.app_music.data.model.Topic
import retrofit2.Response

interface TopicRepository {
    suspend fun getAllTopics(): Response<List<Topic>>
}