package com.example.app_music.domain.utils

import com.example.app_music.data.remote.api.ApiInterface
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

object RetrofitInstance {
    val api: ApiInterface by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .create()

        Retrofit.Builder()
            .baseUrl(ApiInstance.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiInterface::class.java)
    }
}