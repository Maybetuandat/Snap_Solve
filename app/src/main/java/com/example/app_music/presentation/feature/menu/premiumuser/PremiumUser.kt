package com.example.app_music.presentation.feature.menu.premiumuser

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityPremiumUserBinding
import com.example.app_music.domain.utils.AppInfo
import com.example.app_music.presentation.feature.common.BaseActivity
import kotlinx.coroutines.launch
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPaySDK
import vn.zalopay.sdk.listeners.PayOrderListener

class PremiumUser : BaseActivity() {

    private lateinit var binding: ActivityPremiumUserBinding
    private lateinit var viewModel: PremiumViewModel

    companion object {
        private const val TAG = "PremiumUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPremiumUserBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel = ViewModelProvider(this)[PremiumViewModel::class.java]

        setupStrictMode()
        initZaloPaySDK()
        setupClickListeners()
        observeViewModel()


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
            viewModel.createOrderAndInitiatePayment()
        }
    }

    private fun observeViewModel() {

        viewModel.paymentState.observe(this) { state ->
            handlePaymentState(state)
        }


        viewModel.isLoading.observe(this) { isLoading ->
            updateLoadingState(isLoading)
        }


        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Log.e(TAG, "Error: $errorMessage")
            }
        }

    }

    private fun handlePaymentState(state: PaymentState) {
        when (state) {
            is PaymentState.Idle -> {

                updateLoadingState(false)
            }

            is PaymentState.Processing -> {

                Log.d(TAG, "Processing payment...")
            }

            is PaymentState.OrderCreated -> {

                initiateZaloPayPayment(state.zpTransToken)
            }

            is PaymentState.Success -> {
                showSuccessDialog(state.transactionId, state.transToken)
            }

            is PaymentState.Error -> {

                showErrorDialog(getString(R.string.error_transaction), state.errorMessage)
            }

            is PaymentState.Cancelled -> {
                showCancelDialog()
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.btnZalopay.isEnabled = !isLoading
        binding.btnZalopay.text = if (isLoading) {
                    getString(R.string.processing)
        } else {
            getString(R.string.checkout_zalopay)
        }
    }




    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_app)
            )
        }
        startActivity(Intent.createChooser(shareIntent,  getString(R.string.share_app)))
    }

    private fun initiateZaloPayPayment(zpTransToken: String) {
        Log.d(TAG, "Initiating ZaloPay payment with token: $zpTransToken")

        ZaloPaySDK.getInstance().payOrder( //implicit intent
            this@PremiumUser,
            zpTransToken,
            "snapsolve://premium",   //url schema call back -> sau khi thanh toan zalopay se goi lai app theo intent nay
            object : PayOrderListener {   // doi tuong call back
                override fun onPaymentSucceeded(
                    transactionId: String,
                    transToken: String,
                    appTransID: String
                ) {
                    viewModel.onPaymentSuccess(transactionId, transToken, appTransID)
                }

                override fun onPaymentCanceled(zpTransToken: String, appTransID: String) {
                    viewModel.onPaymentCancelled(zpTransToken, appTransID)
                }

                override fun onPaymentError(
                    zaloPayError: vn.zalopay.sdk.ZaloPayError,
                    zpTransToken: String,
                    appTransID: String
                ) {
                    viewModel.onPaymentError(zaloPayError, zpTransToken, appTransID)
                }
            }
        )

        Log.d(TAG, "ZaloPay payOrder called successfully")
    }

    private fun showSuccessDialog(transactionId: String, transToken: String) {



        if (!isFinishing && !isDestroyed) {
            try {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.success_transaction_premium_user))
                    .setMessage(getString(R.string.congratulation_premium_user))
                    .setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                        dialog.dismiss()
                        handlePremiumActivation(transactionId, transToken)
                    }
                    .setCancelable(false)
                    .create()
                    .show()

              //  Log.d(TAG, "Success dialog shown for transaction: $transactionId")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing success dialog", e)
                // Fallback: Handle premium activation without dialog
                handlePremiumActivation(transactionId, transToken)
            }
        } else {
            Log.w(TAG, "Activity is finishing/destroyed, cannot show dialog")
            handlePremiumActivation(transactionId, transToken)
        }
    }

    private fun showCancelDialog() {
        if (!isFinishing && !isDestroyed) {
            try {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.payment_cancel_title))
                    .setMessage(getString(R.string.payment_cancel_message))
                    .setPositiveButton(getString(R.string.retry)) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.createOrderAndInitiatePayment()
                    }
                    .setNegativeButton(getString(R.string.try_later)) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.resetPaymentState()
                    }
                    .show()

                Log.d(TAG, "Cancel dialog shown")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing cancel dialog", e)
            }
        }
    }


    private fun showErrorDialog(title: String, message: String) {
        if (!isFinishing && !isDestroyed) {
            try {
                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.dialog_button_close)) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.resetPaymentState()
                    }
                    .setNeutralButton(getString(R.string.dialog_button_retry)) { dialog, _ ->
                        dialog.dismiss()
                        viewModel.createOrderAndInitiatePayment()
                    }
                    .show()

                Log.d(TAG, "Error dialog shown: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing error dialog", e)
                Toast.makeText(this, "$title: $message", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun handlePremiumActivation(transactionId: String, transToken: String) {
        lifecycleScope.launch {
            try {

                val paymentResult = viewModel.handlePremiumActivation(transactionId, transToken)

                if (paymentResult.success) {

                    val userId = UserPreference.getUserId(this@PremiumUser)
                    viewModel.updateUserRankToPremium(userId)


                    viewModel.updateRankResult.observe(this@PremiumUser) { rankResult ->
                        if (rankResult.isSuccess) {

                            val resultIntent = Intent().apply {
                                putExtra("payment_success", true)
                                putExtra("transaction_id", paymentResult.transactionId)
                                putExtra("amount", paymentResult.amount)
                                putExtra("rank_updated", true)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        } else {

                            showErrorDialog(
                                "Warning",
                                "Payment successful but failed to update profile. Please contact support."
                            )
                        }
                    }
                } else {

                    showErrorDialog(
                        getString(R.string.premium_activation_error_title),
                        paymentResult.errorMessage ?: getString(R.string.premium_activation_error_message)
                    )
                }
            } catch (e: Exception) {
                Log.e("PremiumActivation", "Error during premium activation", e)
                showErrorDialog("Error", "An unexpected error occurred. Please try again.")
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "=== onNewIntent called ===")
        Log.d(TAG, "Intent: $intent")
        Log.d(TAG, "Intent data: ${intent?.data}")
        Log.d(TAG, "Intent action: ${intent?.action}")

        // Set new intent
        setIntent(intent)

        // Handle ZaloPay callback
        ZaloPaySDK.getInstance().onResult(intent)
      //  Log.d(TAG, "ZaloPaySDK.onResult called with intent")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== onResume called ===")

        // Reset loading state when returning to activity
        updateLoadingState(false)
    }




}