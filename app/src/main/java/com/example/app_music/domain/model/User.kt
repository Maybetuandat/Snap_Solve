package com.example.app_music.domain.model



data class User(
    val id: Long? = null,
    val username: String? = null,
    val statusMessage: String? = null,
    val studentInformation: String? = null,
    val suid: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val userRank: String? = null,
    val avatarUrl: String? = null,
    val password: String? = null
)
