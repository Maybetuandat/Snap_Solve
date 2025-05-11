package com.example.app_music.presentation.feature.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_music.R
import com.example.app_music.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private  lateinit var binding : ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding= ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val btnBack = binding.btnBack
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }




}