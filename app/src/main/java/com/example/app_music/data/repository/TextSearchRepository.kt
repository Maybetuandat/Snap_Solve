package com.example.app_music.data.repository

import com.example.app_music.domain.model.TextSearchResponse
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class TextSearchRepository {
    private val textSearchApi = RetrofitFactory.textSearchApi

    suspend fun searchByText(query: String, userId: Long): Response<TextSearchResponse> {
        return textSearchApi.searchByText(query, userId)
    }
}