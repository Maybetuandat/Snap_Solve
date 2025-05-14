package com.example.app_music.domain.usecase

import com.example.app_music.domain.model.User
import com.example.app_music.domain.repository.AuthRepository

class AuthUseCases(
    val login: LoginUseCase,
    val register: RegisterUseCase,
    val updateProfile: UpdateProfileUseCase,
    val saveAuthToken: SaveAuthTokenUseCase,
    val getUserData: GetUserDataUseCase,
    val saveUserData: SaveUserDataUseCase,
    val clearUserSession: ClearUserSessionUseCase
)

/**
 * Use case for saving authentication token
 */
class SaveAuthTokenUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(token: String) {
        authRepository.saveAuthToken(token)
    }
}

/**
 * Use case for getting user data
 */
class GetUserDataUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): User? {
        return authRepository.getUserData()
    }
}

/**
 * Use case for saving user data
 */
class SaveUserDataUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(user: User) {
        authRepository.saveUserData(user)
    }
}

/**
 * Use case for clearing user session (logout)
 */
class ClearUserSessionUseCase(private val authRepository: AuthRepository) {
    operator fun invoke() {
        authRepository.clearUserSession()
    }
}