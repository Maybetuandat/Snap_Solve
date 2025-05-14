package com.example.app_music.data.model



import java.time.LocalDate

data class User(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val dob: LocalDate? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null
)