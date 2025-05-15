package com.example.app_music.domain.usecase

import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.repository.AuthRepository
import retrofit2.Response

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): Response<AuthResponse> {
        return authRepository.login(username, password)
    }
}