package com.example.app_music.data.mapper

import com.example.app_music.data.model.AuthResponseDto
import com.example.app_music.data.model.UserDto
import com.example.app_music.domain.model.AuthResponse
import com.example.app_music.domain.model.User
import java.time.LocalDate

object Mappers {

    // User mappers
    fun mapUserDtoToDomain(userDto: UserDto): User {
        return User(
            id = userDto.id,
            username = userDto.username,
            email = userDto.email,
            phoneNumber = userDto.phoneNumber,
            userRank = userDto.userRank,



            avatarUrl = userDto.avatarUrl
        )
    }

    fun mapUserDomainToDto(user: User): UserDto {
        return UserDto(
            id = user.id,
            username = user.username,
            email = user.email,
            phoneNumber = user.phoneNumber,
            userRank = user.userRank,



            avatarUrl = user.avatarUrl
        )
    }

    // Auth response mappers
    fun mapAuthResponseDtoToDomain(authResponseDto: AuthResponseDto): AuthResponse {
        return AuthResponse(
            token = authResponseDto.token,
            user = mapUserDtoToDomain(authResponseDto.user)
        )
    }



}