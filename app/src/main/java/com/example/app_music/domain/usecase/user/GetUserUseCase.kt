package com.example.app_music.domain.usecase

import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.User
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class GetUserUseCase {
    private val repository = UserRepository(RetrofitFactory.userApi)

    suspend operator fun invoke(userId: Long): Response<User> {
        return repository.getUserById(userId)
    }
}