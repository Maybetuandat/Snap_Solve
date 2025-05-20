package com.example.app_music.presentation.feature.menu.setting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge

import com.example.app_music.databinding.ActivitySettingBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.setting.multilanguage.LanguageSettingsActivity


import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.R
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.presentation.feature.auth.AuthViewModel
import com.example.app_music.presentation.feature.auth.LoginActivity

class SettingActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySettingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        setContentView(binding.root)

        setupListeners()
        setUpObserver()

    }

    private fun setUpObserver() {
        viewModel.deleteAccountResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

                UserPreference.clearUserData(this)


                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
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
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout_confirmation_title))
            .setMessage(getString(R.string.logout_confirmation_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                performLogout()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
    private fun performLogout() {

        viewModel.logoutUser()


        UserPreference.clearUserData(this)


        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }



}



