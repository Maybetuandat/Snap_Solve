package com.example.app_music.presentation.feature.menu.transactions

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.Payment
import com.example.app_music.domain.usecase.payment.GetUserPaymentsUseCase
import kotlinx.coroutines.launch

class TransactionHistoryViewModel : ViewModel() {
    private val getUserPaymentsUseCase = GetUserPaymentsUseCase()

    private val _transactions = MutableLiveData<List<Payment>>()
    val transactions: LiveData<List<Payment>> = _transactions

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadTransactions(userId: Long) {
        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                val response = getUserPaymentsUseCase(userId)

                if (response.isSuccessful) {
                    val paymentList = response.body() ?: emptyList()
                    // Sort by payment date descending (newest first)
                    val sortedPayments = paymentList.sortedByDescending { it.paymentDate }
                    _transactions.value = sortedPayments
                } else {
                    _error.value = "Error loading transactions: ${response.message()}"
                    Log.e("TransactionHistory", "Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                Log.e("TransactionHistory", "Exception loading transactions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}