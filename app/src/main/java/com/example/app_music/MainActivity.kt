package com.example.app_music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.app_music.databinding.ActivityMainBinding
import com.example.app_music.presentation.feature.common.BaseActivity


class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var mainViewModel: MainViewModel

    companion object {
        private const val TAG = "MainActivity"
        private const val CHANNEL_ID = "notification_channel"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Create notification channel
        createNotificationChannel()

        // Request notification permission
        requestNotificationPermission()

        // Observe new notifications for push notification display
        observeNotifications()
    }

    private fun observeNotifications() {
        mainViewModel.newNotification.observe(this) { (title, content) ->
            showPushNotification(title, content)
        if (navController != null) {
            binding.bottomNavigationView.setupWithNavController(navController)

            // Kiểm tra intent từ ResultActivity
            if (intent.getBooleanExtra("NAVIGATE_TO_POSTING", false)) {
                // Đặt Community tab được chọn
                binding.bottomNavigationView.selectedItemId = R.id.communityFragment

                // Đợi cho navigation hoàn tất
                navController.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
                        if (destination.id == R.id.communityFragment) {
                            // Xóa listener để tránh gọi nhiều lần
                            navController.removeOnDestinationChangedListener(this)

                            // Chuyển đến trang posting với dữ liệu từ ResultActivity
                            val bundle = Bundle().apply {
                                putString("IMAGE_PATH", intent.getStringExtra("IMAGE_PATH"))
                                putString("IMAGE_URL", intent.getStringExtra("IMAGE_URL"))
                                putString("QUESTION_TEXT", intent.getStringExtra("QUESTION_TEXT"))
                            }

                            // Navigate to posting fragment
                            navController.navigate(R.id.action_communityFragment_to_communityPostingFragment, bundle)
                        }
                    }
                })
            }
        } else {
            Log.e("MainActivity", "NavController is null!")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)    // dang ky thong bao voi system, ten la Snap ..
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun showPushNotification(title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when showing notification: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconnect WebSocket if needed
        if (!mainViewModel.isWebSocketConnected()) {
            mainViewModel.connectWebSocket()
        }
        // Refresh notification count
        mainViewModel.refreshNotifications()
    }
}