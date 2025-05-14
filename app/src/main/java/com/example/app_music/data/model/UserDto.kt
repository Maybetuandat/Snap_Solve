package com.example.app_music.data.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate

data class UserDto(
    @SerializedName("id")
    val id: Long? = null,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,

    @SerializedName("userRank")
    val userRank: String? = null,

    @SerializedName("dob")
    val dob: LocalDate? = null,

    @SerializedName("firstName")
    val firstName: String? = null,

    @SerializedName("lastName")
    val lastName: String? = null,

    @SerializedName("avatarUrl")
    val avatarUrl: String? = null
)