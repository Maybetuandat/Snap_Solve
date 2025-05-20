package com.example.app_music.presentation.feature.menu.transactions

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.R
import com.example.app_music.databinding.ItemTransactionBinding
import com.example.app_music.domain.model.Payment
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private var transactions: List<Payment> = emptyList(),
    private val onItemClick: (Payment) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val currencyFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    @RequiresApi(Build.VERSION_CODES.O)
    private val storedDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME // For parsing stored dates

    fun updateTransactions(newTransactions: List<Payment>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(transactions[position])
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(payment: Payment) {
            try {
                // Format amount
                val formattedAmount = "${currencyFormat.format(payment.amount)} ${context.getString(R.string.payment_currency)}"
                binding.tvAmount.text = formattedAmount

                // Format payment date
                val paymentDate = parseDateTime(payment.paymentDate)
                binding.tvDate.text = paymentDate.format(dateFormatter)

                // Format subscription type
                val subscriptionText = when (payment.subscriptionType) {
                    Payment.TYPE_MONTHLY -> context.getString(R.string.subscription_monthly)
                    Payment.TYPE_QUARTERLY -> context.getString(R.string.subscription_quarterly)
                    Payment.TYPE_YEARLY -> context.getString(R.string.subscription_yearly)
                    else -> payment.subscriptionType
                }
                binding.tvSubscription.text = subscriptionText

                // Format payment status
                val statusText = when (payment.paymentStatus) {
                    Payment.STATUS_COMPLETED -> context.getString(R.string.payment_status_completed)
                    Payment.STATUS_PENDING -> context.getString(R.string.payment_status_pending)
                    Payment.STATUS_FAILED -> context.getString(R.string.payment_status_failed)
                    Payment.STATUS_CANCELLED -> context.getString(R.string.payment_status_cancelled)
                    else -> payment.paymentStatus
                }
                binding.tvStatus.text = statusText

                // Set status background
                val statusBackground = when (payment.paymentStatus) {
                    Payment.STATUS_COMPLETED -> R.drawable.bg_status_completed
                    Payment.STATUS_PENDING -> R.drawable.bg_status_pending
                    else -> R.drawable.bg_status_failed
                }
                binding.tvStatus.setBackgroundResource(statusBackground)

                // Format expiry date
                val expiryDate = parseDateTime(payment.expiryDate)
                binding.tvExpiry.text = expiryDate.format(dateFormatter)

                // Calculate days remaining
                val now = LocalDateTime.now()
                if (expiryDate.isAfter(now)) {
                    val daysRemaining = ChronoUnit.DAYS.between(now, expiryDate)
                    binding.tvRemaining.text = context.getString(R.string.payment_day_remaining, daysRemaining)
                    binding.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.green))
                } else {
                    binding.tvRemaining.text = context.getString(R.string.payment_expired)
                    binding.tvRemaining.setTextColor(ContextCompat.getColor(context, R.color.red))
                }
            } catch (e: Exception) {
                // Handle any binding errors to prevent crashes
                android.util.Log.e("TransactionAdapter", "Error binding payment: ${e.message}")
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun parseDateTime(dateTimeString: String?): LocalDateTime {
            return try {
                if (dateTimeString.isNullOrEmpty()) {
                    LocalDateTime.now()
                } else {
                    LocalDateTime.parse(dateTimeString, storedDateFormatter)
                }
            } catch (e: DateTimeParseException) {
                // Try alternate formats or return current time if parsing fails
                try {
                    // Try parsing with a more flexible formatter if the initial one fails
                    val alternateFormatter = DateTimeFormatter.ISO_DATE_TIME
                    LocalDateTime.parse(dateTimeString, alternateFormatter)
                } catch (e2: Exception) {
                    LocalDateTime.now()
                }
            }
        }
    }
}