package com.example.app_music.presentation.feature.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.databinding.ActivityRegisterBinding
import com.example.app_music.presentation.feature.common.BaseActivity

class RegisterActivity : BaseActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnRegister.setOnClickListener {
            if (validateInputs()) {
                val username = binding.etUsername.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val phoneNumber = binding.etPhone.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()

                viewModel.register(username, email, phoneNumber, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish() // Return to login screen
        }
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                // Save the newly registered user
                viewModel.setRegisteredUser(result.user)

                // Navigate to profile completion activity
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra(LoginActivity.EXTRA_REGISTRATION_SUCCESS, true)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, result.errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.etUsername.error = "Username is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email address"
            isValid = false
        }

        if (phoneNumber.isEmpty()) {
            binding.etPhone.error = "Phone number is required"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "Confirm password is required"
            isValid = false
        } else if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }
}