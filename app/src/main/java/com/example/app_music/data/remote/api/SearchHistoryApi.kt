package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.SearchHistory
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchHistoryApi {
    // Original endpoint for backward compatibility
    @GET("/api/search/history/{userId}")
    suspend fun getSearchHistory(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 10,
        @Query("query") query: String? = null
    ): Response<List<SearchHistory>>

    // Paginated endpoint
    @GET("/api/search/history/{userId}")
    suspend fun getSearchHistoryPaginated(
        @Path("userId") userId: Long,
        @Query("limit") limit: Int = 10,
        @Query("page") page: Int = 0,
        @Query("query") query: String? = null
    ): Response<List<SearchHistory>>
}