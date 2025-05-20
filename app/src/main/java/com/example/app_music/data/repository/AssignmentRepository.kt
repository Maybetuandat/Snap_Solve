package com.example.app_music.data.repository

import com.example.app_music.domain.model.Assignment
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class AssignmentRepository {
    private val assignmentApi = RetrofitFactory.assignmentApi

    suspend fun getAssignmentsByIds(ids: List<Long>): Response<List<Assignment>> {
        return assignmentApi.getAssignmentsByIds(ids)
    }
}