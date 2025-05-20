package com.example.app_music.data.repository

import com.example.app_music.data.remote.api.PaymentApi
import com.example.app_music.domain.model.Payment
import com.example.app_music.domain.repository.PaymentRepository
import com.example.app_music.domain.utils.RetrofitFactory
import retrofit2.Response

class PaymentRepository : PaymentRepository {
    private val paymentApi = RetrofitFactory.paymentApi

    override suspend fun createPayment(payment: Payment, userId: Long): Response<Payment> {
        return paymentApi.createPayment(payment, userId)
    }

    override suspend fun getUserPayments(userId: Long): Response<List<Payment>> {
        return paymentApi.getUserPayments(userId)
    }
}