package com.example.app_music.domain.usecase

import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.User
import retrofit2.Response

class GetUserUseCase {
    private val repository = UserRepository()

    suspend operator fun invoke(userId: Long): Response<User> {
        return repository.getUserById(userId)
    }
}