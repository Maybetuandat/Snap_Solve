package com.example.app_music.presentation.feature.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge

import com.example.app_music.databinding.ActivitySettingBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.setting.multilanguage.LanguageSettingsActivity


import android.content.Intent


class SettingActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.tvLanguage.setOnClickListener {
            val intent = Intent(this, LanguageSettingsActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()

        MultiLanguage.applyLanguage(this, MultiLanguage.getSelectedLanguage(this))
    }
}



