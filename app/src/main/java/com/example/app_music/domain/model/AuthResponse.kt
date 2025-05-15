package com.example.app_music.domain.model

data class AuthResponse(
    val token: String,
    val user: User
)