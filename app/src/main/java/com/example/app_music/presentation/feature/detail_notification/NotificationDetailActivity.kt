package com.example.app_music.presentation.feature.detail_notification


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.app_music.R
import com.example.app_music.databinding.ActivityNotificationDetailBinding
import com.example.app_music.domain.model.Notification
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.premiumuser.PremiumUser
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationDetailBinding
    private val viewModel: NotificationDetailViewModel by viewModels()

    private var notificationId: Long = 0

    companion object {
        private const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun start(context: Context, notificationId: Long) {
            val intent = Intent(context, NotificationDetailActivity::class.java).apply {
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get notification ID from intent
        notificationId = intent.getLongExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId == 0L) {
            Toast.makeText(this, "Thông báo không tồn tại", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        observeViewModel()

        // Load notification details
        viewModel.loadNotificationDetails(notificationId)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSecondaryAction.setOnClickListener {
            viewModel.deleteNotification(notificationId)
        }
    }

    private fun observeViewModel() {
        viewModel.notification.observe(this, Observer { notification ->
            displayNotificationDetails(notification)
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Handle loading state
        })

        viewModel.error.observe(this, Observer { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        })

        viewModel.deleteResult.observe(this, Observer { isDeleted ->
            if (isDeleted) {
                Toast.makeText(this, "Đã xóa thông báo", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun displayNotificationDetails(notification: Notification) {
        // Set notification title
        binding.tvNotificationTitle.text = notification.title

        // Set notification content
        binding.tvNotificationContent.text = notification.content

        // Set notification time
        binding.tvNotificationTime.text = formatDateTime(notification.notiDate)

        // Set notification details
        binding.tvNotificationId.text = notification.id.toString()
        binding.tvNotificationType.text = notification.type
        binding.tvNotificationDate.text = notification.notiDate
        binding.tvNotificationStatus.text = if (notification.isRead) "Đã đọc" else "Chưa đọc"

        // Set icon based on notification type or content
        when {
            notification.title.contains("Premium", ignoreCase = true) -> {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_premium)
                setupPremiumActions()
            }
            notification.title.contains("thanh toán", ignoreCase = true) -> {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_payment)
                setupPaymentActions()
            }
            else -> {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification)
                binding.actionsContainer.visibility = View.GONE
            }
        }
    }

    private fun setupPremiumActions() {
        binding.actionsContainer.visibility = View.VISIBLE
        binding.btnPrimaryAction.text = "Xem chi tiết gói Premium"
        binding.btnPrimaryAction.setOnClickListener {
            val intent = Intent(this, PremiumUser::class.java)
            startActivity(intent)
        }
    }

    private fun setupPaymentActions() {
        binding.actionsContainer.visibility = View.VISIBLE
        binding.btnPrimaryAction.text = "Xem lịch sử giao dịch"
        binding.btnPrimaryAction.setOnClickListener {
            // Navigate to transaction history
            // You might need to create an intent to navigate to your transaction history screen
            Toast.makeText(this, "Chuyển đến lịch sử giao dịch", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDateTime(dateString: String): String {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputFormatter = DateTimeFormatter.ofPattern("dd 'tháng' MM, yyyy")

            val date = LocalDate.parse(dateString, inputFormatter)
            return date.format(outputFormatter)
        } catch (e: Exception) {
            return dateString
        }
    }
}