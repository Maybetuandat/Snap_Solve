package com.example.app_music.data.repository

import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class AISolutionRepository {

    private val textSearchApi = RetrofitFactory.textSearchApi

    suspend fun getAISolution(query: String, userId: Long): Response<Map<String, Any>> {
        return textSearchApi.getAISolution(query, userId)
    }
}