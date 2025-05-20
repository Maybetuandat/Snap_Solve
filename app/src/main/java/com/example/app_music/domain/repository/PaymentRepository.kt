package com.example.app_music.domain.repository

import com.example.app_music.domain.model.Payment
import retrofit2.Response

interface PaymentRepository {
    suspend fun createPayment(payment: Payment, userId: Long): Response<Payment>
    suspend fun getUserPayments(userId: Long): Response<List<Payment>>
}