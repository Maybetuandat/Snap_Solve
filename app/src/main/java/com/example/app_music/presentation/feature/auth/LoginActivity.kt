package com.example.app_music.presentation.feature.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.MainActivity
import com.example.app_music.databinding.ActivityLoginBinding
import com.example.app_music.presentation.feature.common.BaseActivity

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]


        binding.formContainer.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        setupListeners()
        observeViewModel()


        viewModel.checkSavedCredentials()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                val username = binding.etUsername.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                val rememberMe = binding.cbRememberMe.isChecked

                // Hiển thị loading và ẩn form
                binding.progressBar.visibility = View.VISIBLE
                binding.formContainer.visibility = View.GONE

                viewModel.login(username, password, rememberMe)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Chức năng quên mật khẩu sẽ có trong tương lai", Toast.LENGTH_SHORT).show()
        }

        binding.cvGoogleLogin.setOnClickListener {
            Toast.makeText(this, "Đăng nhập bằng Google sẽ có trong tương lai", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            binding.progressBar.visibility = View.GONE

            if (result.isSuccess) {
                // Chuyển đến MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                // Hiển thị form đăng nhập nếu đăng nhập thất bại
                binding.formContainer.visibility = View.VISIBLE
                if (result.errorMessage.isNotEmpty()) {
                    Toast.makeText(this, result.errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
        }

        // Observer khong co thong tin trong room db
        viewModel.autoLoginCheckComplete.observe(this) { checkComplete ->
            //cai nay dung cho case 1 -> khi login check auto login duoc goi nhung khong co doi tuong nao trong room db

            if (checkComplete && viewModel.loginResult.value == null) {

                binding.formContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty()) {
            binding.etUsername.error = "Vui lòng nhập tên đăng nhập hoặc email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Vui lòng nhập mật khẩu"
            isValid = false
        }

        return isValid
    }
}