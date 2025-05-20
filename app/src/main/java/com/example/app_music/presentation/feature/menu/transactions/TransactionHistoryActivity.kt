package com.example.app_music.presentation.feature.menu.transactions

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityTransactionHistoryBinding
import com.example.app_music.domain.model.Payment
import com.example.app_music.presentation.feature.common.BaseActivity

class TransactionHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityTransactionHistoryBinding
    private lateinit var viewModel: TransactionHistoryViewModel
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[TransactionHistoryViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()

        // Load transactions
        val userId = UserPreference.getUserId(this)
        viewModel.loadTransactions(userId)
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnRefresh.setOnClickListener {
            val userId = UserPreference.getUserId(this)
            viewModel.loadTransactions(userId)
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(this, emptyList()) { payment ->
            showTransactionDetails(payment)
        }

        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(this@TransactionHistoryActivity)
            adapter = this@TransactionHistoryActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val userId = UserPreference.getUserId(this)
            viewModel.loadTransactions(userId)
        }
    }

    private fun observeViewModel() {
        viewModel.transactions.observe(this) { transactions ->
            binding.swipeRefresh.isRefreshing = false
            adapter.updateTransactions(transactions)

            // Show empty state if no transactions
            if (transactions.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvTransactions.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvTransactions.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Don't show the empty state while loading
            if (isLoading) {
                binding.emptyState.visibility = View.GONE
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            binding.swipeRefresh.isRefreshing = false
            if (errorMessage.isNotEmpty()) {
                // Show error toast or snackbar
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    errorMessage,
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showTransactionDetails(payment: Payment) {
        // You could show a dialog or navigate to a detail screen
        // For now, we'll just show a dialog with basic information
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.transaction_details))

        val message = StringBuilder()
        message.append("${getString(R.string.transaction_id)}: ${payment.transactionId}\n\n")
        message.append("${getString(R.string.payment_method)}: ${payment.paymentMethod}\n\n")
        message.append("${getString(R.string.payment_amount)}: ${payment.amount} ${payment.currency}\n\n")
        message.append("${getString(R.string.payment_date)}: ${payment.paymentDate}\n\n")
        message.append("${getString(R.string.payment_expiry)}: ${payment.expiryDate}")

        builder.setMessage(message.toString())
        builder.setPositiveButton(android.R.string.ok, null)
        builder.show()
    }
}