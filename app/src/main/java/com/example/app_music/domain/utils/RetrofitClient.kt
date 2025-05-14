package com.example.app_music.domain.utils

import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.data.remote.api.UserApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(ApiInstance.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }


}