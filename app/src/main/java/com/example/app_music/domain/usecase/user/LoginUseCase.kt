package com.example.app_music.domain.usecase.user

import com.example.app_music.data.repository.AuthRepository
import com.example.app_music.domain.model.AuthResponse

import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response


// overload toán tử () cho phep goi nhu mot function
class LoginUseCase {
    private val repository = AuthRepository(RetrofitFactory.authApi)

    suspend operator fun invoke(username: String, password: String): Response<AuthResponse> {
        return repository.login(username, password)
    }
}