package com.example.app_music.presentation.feature.menu.premiumuser

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.app_music.R
import com.example.app_music.databinding.ActivityPremiumUserBinding
import com.example.app_music.domain.utils.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPayError
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener

class PremiumUser : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumUserBinding
    private val premiumPrice = "98000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStrictMode()
        initZaloPaySDK()
        setupClickListeners()

        // Check nếu activity được mở từ payment callback
        checkPaymentCallback()
    }

    private fun setupStrictMode() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun initZaloPaySDK() {
        ZaloPaySDK.init(AppInfo.APP_ID, Environment.SANDBOX)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnShare.setOnClickListener {
            shareApp()
        }

        binding.btnZalopay.setOnClickListener {
            processZaloPayPayment()
        }
    }

    private fun checkPaymentCallback() {
        val isFromCallback = intent.getBooleanExtra("from_payment_callback", false)

        if (isFromCallback) {
            Log.d("PremiumPayment", "=== Activity opened from payment callback ===")

            val paymentResult = intent.getStringExtra("payment_result")

            when (paymentResult) {
                "success" -> {
                    val transactionId = intent.getStringExtra("transaction_id")
                    val amount = intent.getStringExtra("amount")

                    Log.d("PremiumPayment", "Callback payment success: $transactionId")

                    // Hiển thị success ngay lập tức
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "🎉 Thanh toán thành công từ ZaloPay!",
                            Toast.LENGTH_LONG
                        ).show()

                        showSuccessDialog(transactionId ?: "N/A", "")
                    }
                }
                "error" -> {
                    val errorMessage = intent.getStringExtra("error_message")
                    showErrorDialog("Thanh toán thất bại", errorMessage ?: "Unknown error")
                }
                "cancelled" -> {
                    showCancelDialog()
                }
                else -> {
                    // Xử lý theo cách cũ với onNewIntent
                    handleIntentForCallback(intent)
                }
            }
        } else {
            // Xử lý intent thông thường hoặc deep link
            handleIntentForCallback(intent)
        }
    }

    private fun handleIntentForCallback(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            Log.d("PremiumPayment", "Received VIEW intent: ${intent.data}")

            // Forward đến ZaloPaySDK để xử lý callback
            ZaloPaySDK.getInstance().onResult(intent)
        }
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Khám phá SnapSolve Premium - Giải đáp mọi câu hỏi với AI thông minh!")
        }
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    private fun processZaloPayPayment() {
        // Disable button để tránh click nhiều lần
        binding.btnZalopay.isEnabled = false
        binding.btnZalopay.text = "Đang xử lý..."

        // Sử dụng coroutines cho network call (recommended)
        lifecycleScope.launch {
            try {
                createOrderAndPay()
            } catch (e: Exception) {
                handlePaymentError(e)
            } finally {
                // Re-enable button
                binding.btnZalopay.isEnabled = true
                binding.btnZalopay.text = getString(R.string.checkout_zalopay)
            }
        }
    }

    private suspend fun createOrderAndPay() {
        try {
            // Tạo đơn hàng với API
            val orderResponse = withContext(Dispatchers.IO) {
                CreateOrder().createOrder(premiumPrice)
            }

            Log.d("PremiumPayment", "Order response: $orderResponse")

            // Kiểm tra response có null không
            if (orderResponse == null) {
                Log.e("PremiumPayment", "Order response is null")
                showErrorDialog("Lỗi", "Không nhận được phản hồi từ server. Vui lòng thử lại.")
                return
            }

            // Fix: Lấy return_code dưới dạng int hoặc string
            val returnCode = when {
                orderResponse.has("return_code") -> {
                    try {
                        // Thử lấy dưới dạng string trước
                        orderResponse.getString("return_code")
                    } catch (e: Exception) {
                        // Nếu lỗi, thử lấy dưới dạng int
                        orderResponse.getInt("return_code").toString()
                    }
                }
                else -> {
                    Log.e("PremiumPayment", "No return_code found in response")
                    "0" // Default to error
                }
            }

            Log.d("PremiumPayment", "Return code: $returnCode")

            when (returnCode) {
                "1" -> {
                    // Thành công - Lấy token và thực hiện thanh toán
                    if (orderResponse.has("zp_trans_token")) {
                        val zpTransToken = orderResponse.getString("zp_trans_token")
                        Log.d("PremiumPayment", "Got zp_trans_token: $zpTransToken")

                        // Đảm bảo chạy trên main thread
                        runOnUiThread {
                            initiateZaloPayPayment(zpTransToken)
                        }
                    } else {
                        Log.e("PremiumPayment", "No zp_trans_token found in response")
                        showErrorDialog("Lỗi", "Không nhận được token thanh toán. Vui lòng thử lại.")
                    }
                }
                else -> {
                    val message = orderResponse.optString("return_message", "Có lỗi xảy ra khi tạo đơn hàng")
                    Log.e("PremiumPayment", "Order creation failed: $message")
                    showErrorDialog("Tạo đơn hàng thất bại", message)
                }
            }
        } catch (e: Exception) {
            Log.e("PremiumPayment", "Error creating order", e)
            Log.e("PremiumPayment", "Exception details: ${e.message}")
            e.printStackTrace()
            showErrorDialog("Lỗi", "Không thể tạo đơn hàng. Vui lòng thử lại.\nLỗi: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("PremiumPayment", "=== onNewIntent called ===")
        Log.d("PremiumPayment", "Intent: $intent")
        Log.d("PremiumPayment", "Intent data: ${intent?.data}")
        Log.d("PremiumPayment", "Intent action: ${intent?.action}")

        // Set intent mới
        setIntent(intent)

        // Xử lý callback từ ZaloPay
        ZaloPaySDK.getInstance().onResult(intent)
        Log.d("PremiumPayment", "ZaloPaySDK.onResult called with intent")
    }

    private fun initiateZaloPayPayment(zpTransToken: String) {
        Log.d("PremiumPayment", "Initiating ZaloPay payment with token: $zpTransToken")

        ZaloPaySDK.getInstance().payOrder(
            this@PremiumUser,
            zpTransToken,
            "snapsolve://premium", // Deep link để nhận callback
            object : PayOrderListener {
                override fun onPaymentSucceeded(
                    transactionId: String,
                    transToken: String,
                    appTransID: String
                ) {
                    Log.d("PremiumPayment", "=== PAYMENT SUCCESS CALLBACK ===")
                    Log.d("PremiumPayment", "Transaction ID: $transactionId")
                    Log.d("PremiumPayment", "Trans Token: $transToken")
                    Log.d("PremiumPayment", "App Trans ID: $appTransID")

                    runOnUiThread {
                        // Toast ngay lập tức khi callback được gọi
                        Toast.makeText(
                            this@PremiumUser,
                            "✅ Giao dịch hoàn tất thành công!",
                            Toast.LENGTH_LONG
                        ).show()

                        // Hiển thị dialog chi tiết
                        showSuccessDialog(transactionId, transToken)
                    }
                }

                override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                    Log.d("PremiumPayment", "=== PAYMENT CANCELED ===")
                    Log.d("PremiumPayment", "ZP Trans Token: $zpTransToken")
                    Log.d("PremiumPayment", "App Trans ID: $appTransID")

                    runOnUiThread {
                        showCancelDialog()
                    }
                }

                override fun onPaymentError(
                    zaloPayError: ZaloPayError,
                    zpTransToken: String,
                    appTransID: String
                ) {
                    Log.e("PremiumPayment", "=== PAYMENT ERROR ===")
                    Log.e("PremiumPayment", "ZaloPay Error: $zaloPayError")
                    Log.e("PremiumPayment", "Error Code: ${zaloPayError.name}")
                    Log.e("PremiumPayment", "ZP Trans Token: $zpTransToken")
                    Log.e("PremiumPayment", "App Trans ID: $appTransID")

                    runOnUiThread {
                        showErrorDialog("Thanh toán thất bại", "Lỗi: ${zaloPayError.name}")
                    }
                }
            }
        )

        Log.d("PremiumPayment", "ZaloPay payOrder called successfully")
    }

    private fun showSuccessDialog(transactionId: String, transToken: String) {
        // Hiển thị toast ngay lập tức khi thanh toán thành công
        Toast.makeText(
            this,
            "🎉 Thanh toán thành công! Chào mừng bạn đến với Premium!",
            Toast.LENGTH_LONG
        ).show()

        // Đảm bảo activity vẫn còn active trước khi hiển thị dialog
        if (!isFinishing && !isDestroyed) {
            try {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Thanh toán thành công! 🎉")
                    .setMessage("Chúc mừng! Bạn đã trở thành thành viên Premium.\n\nMã giao dịch: $transactionId")
                    .setPositiveButton("Tuyệt vời!") { dialog, _ ->
                        dialog.dismiss()
                        handlePremiumActivation(transactionId, transToken)
                    }
                    .setCancelable(false)
                    .create()

                // Hiển thị dialog
                dialog.show()

                Log.d("PremiumPayment", "Success dialog shown for transaction: $transactionId")
            } catch (e: Exception) {
                Log.e("PremiumPayment", "Error showing success dialog", e)
                // Fallback: Chỉ sử dụng toast và xử lý kích hoạt premium
                handlePremiumActivation(transactionId, transToken)
            }
        } else {
            Log.w("PremiumPayment", "Activity is finishing/destroyed, cannot show dialog")
            // Activity đã bị destroy, chỉ xử lý logic premium
            handlePremiumActivation(transactionId, transToken)
        }
    }

    private fun showCancelDialog() {
        if (!isFinishing && !isDestroyed) {
            try {
                AlertDialog.Builder(this)
                    .setTitle("Thanh toán bị hủy")
                    .setMessage("Bạn đã hủy quá trình thanh toán. Bạn có muốn thử lại không?")
                    .setPositiveButton("Thử lại") { dialog, _ ->
                        dialog.dismiss()
                        processZaloPayPayment()
                    }
                    .setNegativeButton("Để sau") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

                Log.d("PremiumPayment", "Cancel dialog shown")
            } catch (e: Exception) {
                Log.e("PremiumPayment", "Error showing cancel dialog", e)
            }
        }
    }

    private fun showErrorDialog(title: String, message: String) {
        if (!isFinishing && !isDestroyed) {
            try {
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Đóng") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton("Thử lại") { dialog, _ ->
                        dialog.dismiss()
                        processZaloPayPayment()
                    }
                    .show()

                Log.d("PremiumPayment", "Error dialog shown: $title")
            } catch (e: Exception) {
                Log.e("PremiumPayment", "Error showing error dialog", e)
                // Fallback với toast
                Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handlePaymentError(error: Exception) {
        Log.e("PremiumPayment", "Payment process error", error)
        runOnUiThread {
            Toast.makeText(this, "Có lỗi xảy ra: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handlePremiumActivation(transactionId: String, transToken: String) {
        // 1. Gửi thông tin giao dịch lên server để verify
        // 2. Cập nhật trạng thái premium trong local storage
        // 3. Refresh UI hoặc chuyển về màn hình chính

        // Toast xác nhận kích hoạt premium
        Toast.makeText(
            this,
            "🌟 Tài khoản Premium đã được kích hoạt thành công!",
            Toast.LENGTH_LONG
        ).show()

        // Ví dụ: Quay về màn hình trước với kết quả
        val resultIntent = Intent().apply {
            putExtra("payment_success", true)
            putExtra("transaction_id", transactionId)
            putExtra("amount", premiumPrice)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        Log.d("PremiumPayment", "=== onResume called ===")

        // Re-enable button khi quay lại activity
        binding.btnZalopay.isEnabled = true
        binding.btnZalopay.text = getString(R.string.checkout_zalopay)
    }

    override fun onPause() {
        super.onPause()
        Log.d("PremiumPayment", "=== onPause called ===")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PremiumPayment", "=== onDestroy called ===")
    }
}