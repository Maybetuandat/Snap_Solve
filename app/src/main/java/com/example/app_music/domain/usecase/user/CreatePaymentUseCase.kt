package com.example.app_music.domain.usecase.payment


import com.example.app_music.data.repository.PaymentRepository
import com.example.app_music.domain.model.Payment
import retrofit2.Response

class CreatePaymentUseCase {
    private val repository = PaymentRepository()

    suspend operator fun invoke(payment: Payment): Response<Payment> {
        return repository.createPayment(payment)
    }
}