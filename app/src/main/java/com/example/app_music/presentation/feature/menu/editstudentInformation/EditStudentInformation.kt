
package com.example.app_music.presentation.feature.menu.editstudentInformation

import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.databinding.ActivityEditStudentInformationBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.profile.ProfileEditViewModel

class EditStudentInformation : BaseActivity() {
    private lateinit var binding: ActivityEditStudentInformationBinding
    private lateinit var viewModel: ProfileEditViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditStudentInformationBinding.inflate(layoutInflater)
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

        binding.btnComplete.setOnClickListener {
            val selectedRadioButtonId = binding.rgGrades.checkedRadioButtonId
            if (selectedRadioButtonId != -1) {
                val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
                val gradeInfo = selectedRadioButton.text.toString()

                val userId = UserPreference.getUserId(this)
                viewModel.updateStudentInformation(userId, gradeInfo)
            } else {
                Toast.makeText(this, "Please select a grade", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(this) { user ->
            // Try to find the radio button that matches the current student information
            val currentInfo = user.studentInformation
            if (!currentInfo.isNullOrEmpty()) {
                for (i in 0 until binding.rgGrades.childCount) {
                    val radioButton = binding.rgGrades.getChildAt(i) as RadioButton
                    if (radioButton.text.toString() == currentInfo) {
                        radioButton.isChecked = true
                        break
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnComplete.isEnabled = !isLoading
        }

        viewModel.updateResult.observe(this) { result ->
            if (result.isSuccess) {
                Toast.makeText(this, "Student information updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}