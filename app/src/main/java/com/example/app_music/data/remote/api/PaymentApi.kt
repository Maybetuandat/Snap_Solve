package com.example.app_music.data.remote.api

import com.example.app_music.domain.model.Payment
import retrofit2.Response
import retrofit2.http.*

interface PaymentApi {
    @POST("/api/payments")
    suspend fun createPayment(
        @Body payment: Payment,
        @Query("userId") userId: Long
    ): Response<Payment>

    @GET("/api/payments/user/{userId}")
    suspend fun getUserPayments(
        @Path("userId") userId: Long
    ): Response<List<Payment>>
}