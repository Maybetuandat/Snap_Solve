package com.example.app_music.domain.usecase

import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.AuthRepository
import retrofit2.Response
import java.time.LocalDate

class UpdateProfileUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        userId: Long,
        firstName: String,
        lastName: String,
        dob: LocalDate
    ): Response<User> {
        return authRepository.updateProfile(userId, firstName, lastName, dob)
    }
}