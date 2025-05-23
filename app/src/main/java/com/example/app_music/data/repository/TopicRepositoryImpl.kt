package com.example.app_music.data.repository

import com.example.app_music.domain.model.Topic
import com.example.app_music.domain.repository.TopicRepository
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class TopicRepositoryImpl : TopicRepository {
    override suspend fun getAllTopics(): Response<List<Topic>> {
        return RetrofitFactory.topicApi.getAllTopics()
    }
}