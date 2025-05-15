package com.example.app_music.presentation.feature.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.MainActivity
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityLoginBinding
import com.example.app_music.domain.utils.MultiLanguage
import com.example.app_music.presentation.feature.common.BaseActivity

class LoginActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        binding.formContainer.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        setupLanguageSpinner()
        setupListeners()
        observeViewModel()

        viewModel.checkSavedCredentials()
    }

    private fun setupLanguageSpinner() {
        try {

            val languages = MultiLanguage.getSupportedLanguages()
            Log.d(TAG, "Available languages: ${languages.map { it.code }}")

            // Create and set adapter
            val adapter = LanguageSpinnerAdapter(this, languages)
            binding.spinnerLanguage.adapter = adapter

            // Set current selected language
            val isUsingSystemLanguage = MultiLanguage.isUsingSystemLanguage(this)
            val selectedLanguageCode = if (isUsingSystemLanguage) "system" else MultiLanguage.getSelectedLanguage(this)
            Log.d(TAG, "Current language: $selectedLanguageCode, isSystem: $isUsingSystemLanguage")

            val currentPosition = languages.indexOfFirst { it.code == selectedLanguageCode }
            Log.d(TAG, "Current position in spinner: $currentPosition")

            if (currentPosition != -1) {
                binding.spinnerLanguage.setSelection(currentPosition)
            }

            // Flag to ignore initial selection event - Using class property for reliability
            var isFirstSelection = true

            // Set selection listener
            binding.spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Log.d(TAG, "onItemSelected called, position: $position, isFirstSelection: $isFirstSelection")

                    // Skip the initial selection event that happens when the spinner is first populated
                    if (isFirstSelection) {
                        isFirstSelection = false
                        Log.d(TAG, "Skipping initial selection")
                        return
                    }

                    val selectedLanguage = languages[position]
                    val newLanguageCode = selectedLanguage.code

                    val isUsingSystemLanguage = MultiLanguage.isUsingSystemLanguage(this@LoginActivity)
                    val currentLanguageCode = if (isUsingSystemLanguage) "system" else MultiLanguage.getSelectedLanguage(this@LoginActivity)

                    Log.d(TAG, "Selected language: $newLanguageCode, Current language: $currentLanguageCode")

                    if (newLanguageCode != currentLanguageCode) {
                        Log.d(TAG, "Language changed! Setting new language: $newLanguageCode")

                        val result = MultiLanguage.setSelectedLanguage(this@LoginActivity, newLanguageCode)
                        Log.d(TAG, "setSelectedLanguage result: $result")


                        try {
                            Log.d(TAG, "Attempting to restart app...")
                            // Thay đổi cách restart app
                            val packageManager = applicationContext.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                            val componentName = intent?.component
                            val mainIntent = Intent.makeRestartActivityTask(componentName)
                            applicationContext.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to restart app: ${e.message}")
                            Toast.makeText(
                                this@LoginActivity,
                                "Không thể khởi động lại ứng dụng. Vui lòng khởi động lại thủ công.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Log.d(TAG, "No language change needed")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                    Log.d(TAG, "onNothingSelected called")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up language spinner: ${e.message}")
            e.printStackTrace()
        }
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
                UserPreference.saveUserId(this, result.user?.id!!)
                Log.d("loginactivity", UserPreference.getUserId(this).toString() + " " + result.user.toString())

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