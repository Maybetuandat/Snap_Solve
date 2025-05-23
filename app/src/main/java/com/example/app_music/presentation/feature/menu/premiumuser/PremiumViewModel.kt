package com.example.app_music.presentation.feature.menu.premiumuser

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.UserRepository
import com.example.app_music.domain.model.Payment
import com.example.app_music.domain.usecase.payment.CreatePaymentUseCase
import com.example.app_music.domain.usecase.user.UpdateUserRankUseCase
import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.zalopay.sdk.ZaloPayError
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PremiumViewModel(application: Application) : AndroidViewModel(application) {

    private val _paymentState = MutableLiveData<PaymentState>()
    val paymentState: LiveData<PaymentState> = _paymentState

    private val userRepository = UserRepository(RetrofitFactory.userApi)
    private val updateUserRankUseCase = UpdateUserRankUseCase(userRepository)
    private val createPaymentUseCase = CreatePaymentUseCase()

    private val _updateRankResult = MutableLiveData<UpdateRankResult>()
    val updateRankResult: LiveData<UpdateRankResult> = _updateRankResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val premiumPrice = "98000"
    private var appTransId: String? = null

    companion object {
        private const val TAG = "PremiumViewModel"
    }

    init {
        _paymentState.value = PaymentState.Idle
        _isLoading.value = false
    }

    fun updateUserRankToPremium(userId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                Log.d("PremiumViewModel", "Starting update user rank to premium for userId: $userId")

                val response = updateUserRankUseCase(userId, "premium")

                if (response.isSuccessful) {
                    Log.d("PremiumViewModel", "User rank updated to premium successfully")
                    _updateRankResult.value = UpdateRankResult(
                        isSuccess = true,
                        message = "Welcome to Premium!"
                    )
                } else {
                    Log.e("PremiumViewModel", "Failed to update user rank: ${response.message()}")
                    _updateRankResult.value = UpdateRankResult(
                        isSuccess = false,
                        message = "Failed to update user rank: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("PremiumViewModel", "Error updating user rank", e)
                _updateRankResult.value = UpdateRankResult(
                    isSuccess = false,
                    message = "Error: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createOrderAndInitiatePayment() {
        if (_isLoading.value == true) {
            Log.w(TAG, "Payment already in progress")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _paymentState.value = PaymentState.Processing

                val orderResponse = withContext(Dispatchers.IO) {
                    val result = CreateOrder().createOrder(premiumPrice)
                    result
                }

                handleOrderResponse(orderResponse)

            } catch (e: Exception) {
                Log.e(TAG, "Error creating order", e)
                _paymentState.value = PaymentState.Error("Không thể tạo đơn hàng. Vui lòng thử lại.\nLỗi: ${e.message}")
                _errorMessage.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleOrderResponse(orderResponse: JSONObject?) {
        if (orderResponse == null) {
            Log.e(TAG, "Order response is null")
            _paymentState.value = PaymentState.Error("Không nhận được phản hồi từ server. Vui lòng thử lại.")
            return
        }

        Log.d(TAG, "Order response: $orderResponse")

        val returnCode = try {
            when {
                orderResponse.has("return_code") -> {
                    try {
                        orderResponse.getString("return_code")
                    } catch (e: Exception) {
                        orderResponse.getInt("return_code").toString()
                    }
                }
                else -> {
                    Log.e(TAG, "No return_code found in response")
                    "0"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing return_code", e)
            "0"
        }

        Log.d(TAG, "Return code: $returnCode")

        when (returnCode) {
            "1" -> {
                if (orderResponse.has("zp_trans_token")) {
                    val zpTransToken = orderResponse.getString("zp_trans_token")
                    // Save app_trans_id for future reference
                    appTransId = orderResponse.optString("app_trans_id", "")

                    Log.d(TAG, "Got zp_trans_token: $zpTransToken, app_trans_id: $appTransId")
                    _paymentState.value = PaymentState.OrderCreated(zpTransToken)
                } else {
                    Log.e(TAG, "No zp_trans_token found in response")
                    _paymentState.value = PaymentState.Error("Không nhận được token thanh toán. Vui lòng thử lại.")
                }
            }
            else -> {
                val message = orderResponse.optString("return_message", "Có lỗi xảy ra khi tạo đơn hàng")
                Log.e(TAG, "Order creation failed: $message")
                _paymentState.value = PaymentState.Error("Tạo đơn hàng thất bại: $message")
            }
        }
    }

    fun onPaymentSuccess(transactionId: String, transToken: String, appTransID: String) {
        Log.d(TAG, "=== PAYMENT SUCCESS ===")
        Log.d(TAG, "Transaction ID: $transactionId")
        Log.d(TAG, "Trans Token: $transToken")
        Log.d(TAG, "App Trans ID: $appTransID")

        _paymentState.value = PaymentState.Success(transactionId, transToken, premiumPrice)
    }

    fun onPaymentCancelled(zpTransToken: String, appTransID: String) {
        Log.d(TAG, "=== PAYMENT CANCELLED ===")
        Log.d(TAG, "ZP Trans Token: $zpTransToken")
        Log.d(TAG, "App Trans ID: $appTransID")

        _paymentState.value = PaymentState.Cancelled
    }

    fun onPaymentError(zaloPayError: ZaloPayError, zpTransToken: String, appTransID: String) {
        Log.e(TAG, "=== PAYMENT ERROR ===")
        Log.e(TAG, "ZaloPay Error: $zaloPayError")
        Log.e(TAG, "Error Code: ${zaloPayError.name}")
        Log.e(TAG, "ZP Trans Token: $zpTransToken")
        Log.e(TAG, "App Trans ID: $appTransID")

        _paymentState.value = PaymentState.Error("Thanh toán thất bại: ${zaloPayError.name}")
    }

    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
        _isLoading.value = false
        _errorMessage.value = ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun handlePremiumActivation(transactionId: String, transToken: String): PremiumActivationResult {
        // Save payment info
        viewModelScope.launch {
            try {
                val userId = com.example.app_music.data.local.preferences.UserPreference.getUserId(getApplication())

                // Create payment object
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                val expiryDate = now.plusMonths(1) // 1 month subscription

                val payment = Payment(
                    transactionId = transactionId,
                    orderId = appTransId ?: "order_${System.currentTimeMillis()}",
                    amount = BigDecimal(premiumPrice),
                    currency = "VND",
                    paymentMethod = Payment.METHOD_ZALOPAY,
                    paymentStatus = Payment.STATUS_COMPLETED,
                    subscriptionType = Payment.TYPE_MONTHLY,
                    durationMonths = 1,
                    paymentDate = now.format(formatter),
                    expiryDate = expiryDate.format(formatter),
                    userId = userId
                )

                // Save payment to server
                val result = createPaymentUseCase(payment)

                if (result.isSuccessful) {
                    Log.d(TAG, "Payment saved successfully: ${result.body()}")
                } else {
                    Log.e(TAG, "Failed to save payment: ${result.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving payment", e)
            }
        }

        return PremiumActivationResult(
            success = true,
            transactionId = transactionId,
            amount = premiumPrice
        )
    }

    data class PremiumActivationResult(
        val success: Boolean,
        val transactionId: String,
        val amount: String,
        val errorMessage: String? = null
    )

    data class UpdateRankResult(
        val isSuccess: Boolean,
        val message: String
    )
}