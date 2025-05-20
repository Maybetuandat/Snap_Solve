package com.example.app_music.data.repository

import android.util.Log
import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class SearchHistoryRepository {
    private val TAG = "SearchHistoryRepo"
    private val searchHistoryApi = RetrofitFactory.searchHistoryApi

    suspend fun getSearchHistory(userId: Long, limit: Int = 10): Response<List<SearchHistory>> {
        Log.d(TAG, "getSearchHistory: userId=$userId, limit=$limit")
        val response = searchHistoryApi.getSearchHistory(userId, limit)
        Log.d(TAG, "getSearchHistory response: ${response.code()}")
        return response
    }

    suspend fun getSearchHistoryPaginated(userId: Long, limit: Int = 10, page: Int = 0): Response<List<SearchHistory>> {
        Log.d(TAG, "getSearchHistoryPaginated: userId=$userId, limit=$limit, page=$page")
        val response = searchHistoryApi.getSearchHistoryPaginated(userId, limit, page)
        Log.d(TAG, "getSearchHistoryPaginated response: ${response.code()}")
        return response
    }

    suspend fun searchHistoryByQuery(userId: Long, query: String, limit: Int = 50): Response<List<SearchHistory>> {
        Log.d(TAG, "searchHistoryByQuery: userId=$userId, query=$query, limit=$limit")
        val response = searchHistoryApi.getSearchHistory(userId, limit, query)
        Log.d(TAG, "searchHistoryByQuery response: ${response.code()}")
        return response
    }

    suspend fun searchHistoryByQueryPaginated(userId: Long, query: String, limit: Int = 10, page: Int = 0): Response<List<SearchHistory>> {
        Log.d(TAG, "searchHistoryByQueryPaginated: userId=$userId, query=$query, limit=$limit, page=$page")
        try {
            val response = searchHistoryApi.getSearchHistoryPaginated(userId, limit, page, query)
            Log.d(TAG, "searchHistoryByQueryPaginated response: ${response.code()}")
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Error in searchHistoryByQueryPaginated", e)
            throw e
        }
    }
}