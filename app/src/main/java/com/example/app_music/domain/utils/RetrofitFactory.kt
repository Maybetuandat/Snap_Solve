package com.example.app_music.domain.utils

import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.data.remote.api.CommentApi
import com.example.app_music.data.remote.api.PostApi
import com.example.app_music.data.remote.api.TopicApi
import com.example.app_music.data.remote.api.UserApi
import com.example.app_music.domain.model.Comment
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

object RetrofitFactory {
    private val retrofit by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(Comment::class.java, CommentDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(ApiInstance.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }


    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val postApi: PostApi by lazy {
        retrofit.create(PostApi::class.java)
    }

    val topicApi: TopicApi by lazy {
        retrofit.create(TopicApi::class.java)
    }

    val commentApi: CommentApi by lazy {
        retrofit.create(CommentApi::class.java)
    }
}