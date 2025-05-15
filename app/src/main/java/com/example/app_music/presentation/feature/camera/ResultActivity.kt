package com.example.app_music.presentation.feature.camera

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.app_music.databinding.ActivityResultBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set toolbar back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Get the cropped image path from intent
        val imagePath = intent.getStringExtra("IMAGE_PATH")

        if (imagePath != null) {
            // Load and display the cropped image
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.imageCropped.setImageBitmap(bitmap)

            // Show loading state
            binding.progressBar.visibility = View.VISIBLE
            binding.linearResults.visibility = View.GONE

            // Simulate API call to backend
            CoroutineScope(Dispatchers.IO).launch {
                // Simulate network delay
                delay(2000)

                // Get mock response
                val mockResponse = getMockResponse()

                withContext(Dispatchers.Main) {
                    // Hide progress and show results
                    binding.progressBar.visibility = View.GONE
                    binding.linearResults.visibility = View.VISIBLE

                    // Display results
                    binding.tvQuestion.text = mockResponse.question
                    binding.tvAnswer.text = mockResponse.answer

                    // Setup community post button
                    binding.btnPostCommunity.setOnClickListener {
                        // Simulate posting to community
                        binding.btnPostCommunity.text = "Posted to community"
                        binding.btnPostCommunity.isEnabled = false
                    }
                }
            }
        } else {
            // Handle error case
            finish()
        }
    }

    // Simulate backend response
    private fun getMockResponse(): MockResponse {
        return MockResponse(
            question = "Tìm hệ số a,b,c để y sau có cực trị: y = a·x² + b·x + c·y + x²",
            answer = "Để có cực trị, y' = 0:\n2a·x + b + 2x = 0\nSắp xếp: (2a+2)x + b = 0\nVì phương trình này có nghiệm với mọi x, nên:\n2a+2 = 0 → a = -1\nb = 0\nDo đó (a,b,c) = (-1, 0, c) với c bất kỳ."
        )
    }

    // Mock data class to represent the response
    data class MockResponse(
        val question: String,
        val answer: String
    )
}