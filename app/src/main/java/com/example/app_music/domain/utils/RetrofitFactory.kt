package com.example.app_music.domain.utils

import com.example.app_music.data.remote.api.AssignmentApi
import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.data.remote.api.CommentApi
import com.example.app_music.data.remote.api.ImageUploadApi
import com.example.app_music.data.remote.api.PaymentApi
import com.example.app_music.data.remote.api.PostApi
import com.example.app_music.data.remote.api.SearchHistoryApi
import com.example.app_music.data.remote.api.TextSearchApi
import com.example.app_music.data.remote.api.TopicApi
import com.example.app_music.data.remote.api.UserApi
import com.example.app_music.domain.model.Comment
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(Comment::class.java, CommentDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(ApiInstance.baseUrl)

            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }


    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }



    val paymentApi: PaymentApi by lazy {
        retrofit.create(PaymentApi::class.java)
    }




    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val imageUploadApi: ImageUploadApi by lazy {
        retrofit.create(ImageUploadApi::class.java)
    }
    val postApi: PostApi by lazy {
        retrofit.create(PostApi::class.java)
    }

    val textSearchApi: TextSearchApi by lazy {
        retrofit.create(TextSearchApi::class.java)
    }
    val searchHistoryApi: SearchHistoryApi by lazy {
        retrofit.create(SearchHistoryApi::class.java)
    }

    val assignmentApi: AssignmentApi by lazy {
        retrofit.create(AssignmentApi::class.java)
    }

    val topicApi: TopicApi by lazy {
        retrofit.create(TopicApi::class.java)
    }

    val commentApi: CommentApi by lazy {
        retrofit.create(CommentApi::class.java)
    }
}