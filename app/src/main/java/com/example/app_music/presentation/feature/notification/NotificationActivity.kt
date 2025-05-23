package com.example.app_music.presentation.feature.notification

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.app_music.R
import com.example.app_music.databinding.ActivityNotificationBinding
import com.example.app_music.domain.model.Notification
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.detail_notification.NotificationDetailActivity
import com.example.app_music.presentation.feature.menu.premiumuser.PremiumUser

class NotificationActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var adapter: NotificationAdapter

    private val viewModel: NotificationActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        setupObservers()

        // Load notifications
        viewModel.loadNotifications()
    }

    private fun setupUI() {
        // Setup RecyclerView
        adapter = NotificationAdapter(this) { notification ->
            onNotificationClicked(notification)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = this@NotificationActivity.adapter
            setHasFixedSize(true)
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setColorSchemeResources(
            R.color.selectedicon,
            R.color.red
        )
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Mark all as read
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
        }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications()
        }

        // Close ad button
        binding.btnCloseAd.setOnClickListener {
            viewModel.hideAdBanner()
        }

        // Upgrade button
        binding.btnUpgrade.setOnClickListener {
            val intent = Intent(this, PremiumUser::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        // Observe notifications
        viewModel.notifications.observe(this) { notifications ->
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(notifications)

            // Show/hide empty state
            if (notifications.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading && adapter.currentList.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Don't show refresh indicator if progress bar is visible
            if (isLoading && adapter.currentList.isEmpty()) {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Observe error messages
        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // Observe ad banner visibility
        viewModel.showAdBanner.observe(this) { showAd ->
            binding.adBanner.visibility = if (showAd) View.VISIBLE else View.GONE
        }
    }

    private fun onNotificationClicked(notification: Notification) {
        // Handle notification click
        viewModel.onNotificationClicked(notification)

        // Navigate to notification detail screen
        NotificationDetailActivity.start(this, notification.id)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this screen
        viewModel.loadNotifications()
    }
}