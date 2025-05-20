package com.example.app_music

import android.content.Context
import android.os.Bundle
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


    }
    // Thêm vào phương thức onDestroy
    override fun onDestroy() {
        super.onDestroy()
        // Hủy tất cả coroutine đang chạy
        lifecycleScope.coroutineContext.cancelChildren()
    }

}