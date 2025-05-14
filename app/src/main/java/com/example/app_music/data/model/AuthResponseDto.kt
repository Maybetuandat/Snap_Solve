package com.example.app_music.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: UserDto
)