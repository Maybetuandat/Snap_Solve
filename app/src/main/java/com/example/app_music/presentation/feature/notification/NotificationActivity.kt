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


        viewModel.loadNotifications()
    }

    private fun setupUI() {

        adapter = NotificationAdapter(this) { notification ->
            onNotificationClicked(notification)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = this@NotificationActivity.adapter
            setHasFixedSize(true)
        }


        binding.swipeRefresh.setColorSchemeResources(
            R.color.selectedicon,
            R.color.red
        )
    }

    private fun setupListeners() {

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }




        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications()
        }




    }

    private fun setupObservers() {

        viewModel.notifications.observe(this) { notifications ->
            binding.swipeRefresh.isRefreshing = false
            adapter.submitList(notifications)


            if (notifications.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }


        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading && adapter.currentList.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }


            if (isLoading && adapter.currentList.isEmpty()) {
                binding.swipeRefresh.isRefreshing = false
            }
        }


        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }


    }

    private fun onNotificationClicked(notification: Notification) {

        viewModel.onNotificationClicked(notification)


        NotificationDetailActivity.start(this, notification.id)
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadNotifications()
    }
}