package com.example.app_music.domain.usecase.payment


import com.example.app_music.data.repository.PaymentRepository
import com.example.app_music.domain.model.Payment
import retrofit2.Response

class GetUserPaymentsUseCase {
    private val repository = PaymentRepository()

    suspend operator fun invoke(userId: Long): Response<List<Payment>> {
        return repository.getUserPayments(userId)
    }
}