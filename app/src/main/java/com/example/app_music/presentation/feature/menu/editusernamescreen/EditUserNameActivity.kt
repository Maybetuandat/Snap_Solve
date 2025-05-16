
package com.example.app_music.presentation.feature.menu.editusernamescreen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityEditUserNameBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.profile.ProfileEditViewModel

class EditUserNameActivity : BaseActivity() {
    private lateinit var binding: ActivityEditUserNameBinding
    private lateinit var viewModel: ProfileEditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditUserNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ProfileEditViewModel::class.java]

        setupListeners()
        setupObservers()

        // Get current user id from preferences
        val userId = UserPreference.getUserId(this)
        viewModel.fetchUserData(userId)
    }

    private fun setupListeners() {
        binding.ivBackArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvSave.setOnClickListener {
            val newUsername = binding.etUsername.text.toString().trim()
            if (validateUsername(newUsername)) {
                val userId = UserPreference.getUserId(this)
                viewModel.updateUsername(userId, newUsername)
            }
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(this) { user ->
            binding.etUsername.setText(user.username)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.tvSave.isEnabled = !isLoading

        }

        viewModel.updateResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show()
                // Return to the previous screen
                finish()
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateUsername(username: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = "Username cannot be empty"
            return false
        }

        if (username.length > 20) {
            binding.etUsername.error = "Username cannot exceed 20 characters"
            return false
        }


        return true
    }
}