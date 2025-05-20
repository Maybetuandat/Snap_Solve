package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Assignment
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AssignmentApi {
    @GET("/api/assignments/byIds")
    suspend fun getAssignmentsByIds(@Query("ids") ids: List<Long>): Response<List<Assignment>>
}