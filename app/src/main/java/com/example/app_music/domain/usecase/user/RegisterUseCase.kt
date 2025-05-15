package com.example.app_music.domain.usecase

import com.example.app_music.data.repository.AuthRepository
import com.example.app_music.domain.model.User
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class RegisterUseCase {
    private val repository = AuthRepository(RetrofitFactory.authApi)

    suspend operator fun invoke(user: User): Response<User> {
        return repository.register(user)
    }
}