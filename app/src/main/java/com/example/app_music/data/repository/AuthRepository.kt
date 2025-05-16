package com.example.app_music.data.repository

import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class AuthRepository(
    private val apiService: AuthApi,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Response<AuthResponse> {
        val response = apiService.login(username, password)
        if(response.isSuccessful) {
            val authResponse = response.body()!!
            return Response.success(authResponse)
        }
        else {
            // Use Kotlin extension functions here too
            return Response.error(
                response.code(),
                response.errorBody()?.string()?.toResponseBody("text/plain".toMediaTypeOrNull())
                    ?: "Unknown error".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        }
    }

    override suspend fun register(
        user: User
    ): Response<User> {
        val response = apiService.register(user)
        if(response.isSuccessful) {
            val user = response.body()
            return Response.success(user)
        }
        else {
            // Use Kotlin extension functions here too
            return Response.error(
                response.code(),
                response.errorBody()?.string()?.toResponseBody("text/plain".toMediaTypeOrNull())
                    ?: "Unknown error".toResponseBody("text/plain".toMediaTypeOrNull())
            )
        }
    }
}