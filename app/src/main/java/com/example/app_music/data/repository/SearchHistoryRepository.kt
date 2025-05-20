package com.example.app_music.data.repository

import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class SearchHistoryRepository {
    private val searchHistoryApi = RetrofitFactory.searchHistoryApi

    suspend fun getSearchHistory(userId: Long, limit: Int = 3): Response<List<SearchHistory>> {
        return searchHistoryApi.getSearchHistory(userId, limit)
    }
}