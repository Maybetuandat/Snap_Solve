package com.example.app_music.presentation.feature.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app_music.R
import com.example.app_music.databinding.ActivityUpdateProfileBinding

class UpdateProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateProfileBinding
//    private lateinit var viewModel: AuthViewModel
//    private val calendar = Calendar.getInstance()
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    //    binding = ActivityProfileCompletionBinding.inflate(layoutInflater)
        setContentView(binding.root)

     //   viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

     //   setupSpinner()
//        setupDatePicker()
//        setupListeners()
//        observeViewModel()
    }

//    private fun setupSpinner() {
//        // Setup grade spinner with values
//        val grades = resources.getStringArray(R.array.grades_array)
//     //   val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spinnerGrade.adapter = adapter
//    }

//    private fun setupDatePicker() {
//        // Set up date picker dialog
//        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
//            calendar.set(Calendar.YEAR, year)
//            calendar.set(Calendar.MONTH, month)
//            calendar.set(Calendar.DAY_OF_MONTH, day)
//            updateDateInView()
//        }
//
//        binding.etDob.setOnClickListener {
//            DatePickerDialog(
//                this, dateSetListener,
//                calendar.get(Calendar.YEAR),
//                calendar.get(Calendar.MONTH),
//                calendar.get(Calendar.DAY_OF_MONTH)
//            ).show()
//        }
//    }

//    private fun updateDateInView() {
//        binding.etDob.setText(dateFormat.format(calendar.time))
//    }
//
//    private fun setupListeners() {
//        binding.btnContinue.setOnClickListener {
//            if (validateInputs()) {
//                val firstName = binding.etFirstName.text.toString().trim()
//                val lastName = binding.etLastName.text.toString().trim()
//                val dob = binding.etDob.text.toString().trim()
//
//                viewModel.completeProfile(firstName, lastName, dob)
//            }
//        }
//    }
//
//    private fun observeViewModel() {
//        viewModel.profileUpdateResult.observe(this) { result ->
//            binding.progressBar.visibility = View.GONE
//
//            if (result.isSuccess) {
//                // Save auth token and user info to SharedPreferences
//                viewModel.saveUserSession(this, result.token, result.user)
//
//                // Navigate to MainActivity
//                val intent = Intent(this, MainActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(intent)
//                finish()
//            } else {
//                Toast.makeText(this, result.errorMessage, Toast.LENGTH_LONG).show()
//            }
//        }
//
//        viewModel.isLoading.observe(this) { isLoading ->
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//            binding.btnContinue.isEnabled = !isLoading
//        }
//    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val dob = binding.etDob.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.etFirstName.error = "First name is required"
            isValid = false
        }

        if (lastName.isEmpty()) {
            binding.etLastName.error = "Last name is required"
            isValid = false
        }

        if (dob.isEmpty()) {
            binding.etDob.error = "Date of birth is required"
            isValid = false
        }

        return isValid
    }
}