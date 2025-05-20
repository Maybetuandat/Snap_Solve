package com.example.app_music.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Payment(
    val id: Long? = null,
    val transactionId: String,
    val orderId: String,
    val amount: BigDecimal,
    val currency: String = "VND",
    val paymentMethod: String = "ZALOPAY",
    val paymentStatus: String = "COMPLETED",
    val subscriptionType: String = "MONTHLY",
    val durationMonths: Int = 1,
    val paymentDate: String? = null,
    val expiryDate: String? = null,
    val userId: Long? = null
) {
    companion object {
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CANCELLED = "CANCELLED"

        const val METHOD_ZALOPAY = "ZALOPAY"

        const val TYPE_MONTHLY = "MONTHLY"
        const val TYPE_QUARTERLY = "QUARTERLY"
        const val TYPE_YEARLY = "YEARLY"
    }
}