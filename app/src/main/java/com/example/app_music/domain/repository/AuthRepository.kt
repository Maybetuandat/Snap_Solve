package com.example.app_music.domain.repository

import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import retrofit2.Response
import java.time.LocalDate

interface AuthRepository {
    suspend fun login(username: String, password: String): Response<AuthResponse>

    suspend fun register(
        username: String,
        email: String,
        phoneNumber: String,
        password: String
    ): Response<User>

    suspend fun updateProfile(
        userId: Long,
        firstName: String,
        lastName: String,
        dob: LocalDate
    ): Response<User>

    fun saveAuthToken(token: String)

    fun getAuthToken(): String?

    fun saveUserData(user: User)

    fun getUserData(): User?

    fun clearUserSession()

    fun getStoredPassword(): String
}