package com.example.app_music.domain.usecase.user

import android.util.Log
import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.UserRepository
import retrofit2.Response

class UpdateUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(userId: Long, user: User): Response<User> {
        Log.d("updateuserusecase", user.toString())
        return repository.updateUser(userId, user)

    }
}