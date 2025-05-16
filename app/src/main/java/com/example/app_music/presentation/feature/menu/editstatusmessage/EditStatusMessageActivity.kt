
package com.example.app_music.presentation.feature.menu.editstatusmessage

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityEditStatusMessageBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.profile.ProfileEditViewModel

class EditStatusMessageActivity : BaseActivity() {
    private lateinit var binding: ActivityEditStatusMessageBinding
    private lateinit var viewModel: ProfileEditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditStatusMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProfileEditViewModel::class.java]

        setupListeners()
        setupObservers()

        val userId = UserPreference.getUserId(this)
        viewModel.fetchUserData(userId)
    }

    private fun setupListeners() {
        binding.ivBackArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvSave.setOnClickListener {
            val newStatusMessage = binding.etStatusMessage.text.toString().trim()
            val userId = UserPreference.getUserId(this)
            viewModel.updateStatusMessage(userId, newStatusMessage)
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(this) { user ->
            binding.etStatusMessage.setText(user.statusMessage)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.tvSave.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Status message updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}