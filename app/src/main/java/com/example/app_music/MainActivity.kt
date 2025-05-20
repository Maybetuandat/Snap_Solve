package com.example.app_music

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.app_music.databinding.ActivityMainBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.setting.multilanguage.RestartAppDialog
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
        } else {
            Log.e("MainActivity", "NavController is null!")
        }

        handleIntentExtras(intent)
    }
    private fun handleIntentExtras(intent: Intent?) {
        if (intent?.getIntExtra("SELECT_HOME_TAB", 99) == 100) {
            // Chuyển đến HomeFragment
            binding.bottomNavigationView.selectedItemId = R.id.homeFragment
        }
    }
    override fun onResume() {
        super.onResume()

        // Kiểm tra lại trong onResume để đảm bảo UI đã được khởi tạo hoàn toàn
        Log.d("MainActivity", "onResume - checking extras")
        if(intent == null)
            Log.d("MainActivity","Intent is null")
        if(intent != null)
        {
            Log.d("MainActivity", intent.getIntExtra("SELECT_HOME_TAB", 99).toString());
        }
        if (intent != null && intent.getIntExtra("SELECT_HOME_TAB", 99) == 100) {
            Log.d("MainActivity", "Should select Home tab in onResume")
            selectHomeTab()
        }
    }
    // Thêm vào phương thức onDestroy
    override fun onDestroy() {
        super.onDestroy()
        // Hủy tất cả coroutine đang chạy
        lifecycleScope.coroutineContext.cancelChildren()
    }
    private fun selectHomeTab() {
        // Sử dụng Handler để delay việc chọn tab đảm bảo BottomNavigationView đã được khởi tạo
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("MainActivity", "Selecting Home tab now")
            binding.bottomNavigationView.selectedItemId = R.id.homeFragment

            // Đặt lại flag để không chọn lại tab khi xoay màn hình
            intent.removeExtra("SELECT_HOME_TAB")
        }, 100)
    }

}