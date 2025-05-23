package com.example.app_music.domain.utils

import com.example.app_music.data.remote.api.AuthApi
import com.example.app_music.data.remote.api.ImageUploadApi
import com.example.app_music.data.remote.api.TextSearchApi
import com.example.app_music.data.remote.api.PaymentApi
import com.example.app_music.data.remote.api.CommentApi
import com.example.app_music.data.remote.api.NotificationApi
import com.example.app_music.data.remote.api.PostApi
import com.example.app_music.data.remote.api.TopicApi
import com.example.app_music.data.remote.api.UserApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.example.app_music.domain.model.Comment
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.time.LocalDate

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
    val topicApi: TopicApi by lazy {
        retrofit.create(TopicApi::class.java)
    }

    val commentApi: CommentApi by lazy {
        retrofit.create(CommentApi::class.java)
    }
    val notificationApi: NotificationApi by lazy {
        retrofit.create(NotificationApi::class.java)
    }
}