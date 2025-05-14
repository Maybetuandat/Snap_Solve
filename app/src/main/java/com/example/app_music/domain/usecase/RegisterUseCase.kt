//package com.example.app_music.domain.usecase
//
//import com.example.app_music.domain.model.User
//import com.example.app_music.domain.repository.AuthRepository
//import retrofit2.Response
//
//class RegisterUseCase(private val authRepository: AuthRepository) {
//    suspend operator fun invoke(
//        username: String,
//        email: String,
//        phoneNumber: String,
//        password: String
//    ): Response<User> {
//        return authRepository.register(username, email, phoneNumber, password)
//    }
//}