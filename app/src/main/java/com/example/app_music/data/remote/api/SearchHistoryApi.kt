package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.SearchHistory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchHistoryApi {
    @GET("/api/search/history/{userId}")
    suspend fun getSearchHistory(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 10
    ): Response<List<SearchHistory>>
}