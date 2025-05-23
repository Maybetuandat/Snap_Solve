package com.example.app_music

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.app_music.databinding.ActivityMainBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import kotlinx.coroutines.cancelChildren

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.let {
            it as? NavHostFragment
        }?.navController

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

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.coroutineContext.cancelChildren()
    }
}