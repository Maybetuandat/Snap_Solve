package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.TextSearchResponse
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Query

interface TextSearchApi {
    @POST("/api/search/text")
    suspend fun searchByText(
        @Query("query") query: String,
        @Query("userId") userId: Long
    ): Response<TextSearchResponse>
}