package com.example.app_music.presentation.feature.camera

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.databinding.ActivityResultBinding
import com.example.app_music.presentation.feature.common.BaseActivity
import java.io.File

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: ResultViewModel
    private var imagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ResultViewModel::class.java]

        // Set toolbar back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Get the cropped image path from intent
        imagePath = intent.getStringExtra("IMAGE_PATH")

        if (imagePath != null) {
            // Load and display the cropped image
            displayCroppedImage()

            // Setup observers
            setupObservers()

            // Show loading state
            showLoadingState()

            // Upload to server automatically
            uploadImageToServer()

        } else {
            // Handle error case - no image
            Toast.makeText(this, "No image found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup button click listeners
        setupClickListeners()
    }

    private fun displayCroppedImage() {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.imageCropped.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        // Observe upload status
        viewModel.uploadStatus.observe(this) { status ->
            when (status) {
                is ResultViewModel.UploadStatus.Loading -> {
                    showLoadingState()
                }
                is ResultViewModel.UploadStatus.Success -> {
                    // If we get a success but no analysis results yet, keep showing loading
                    // The analysis results observer will handle showing results
                    if (binding.linearResults.visibility != View.VISIBLE) {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    // Log success
                    val message = "Image uploaded successfully" +
                            if (status.imageUrl != null) ", URL: ${status.imageUrl}" else ""
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                is ResultViewModel.UploadStatus.Error -> {
                    // Show error but still display the image
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Upload error: ${status.message}", Toast.LENGTH_LONG).show()

                    // Show an empty result with only the cropped image
                    binding.linearResults.visibility = View.VISIBLE
                    binding.tvQuestion.text = "Unable to analyze image"
                    binding.tvAnswer.text = "There was a problem uploading the image to our servers. You can try again or post to the community for help."
                }
            }
        }

        // Observe analysis results
        viewModel.analyzeResult.observe(this) { result ->
            when (result) {
                is ResultViewModel.AnalyzeResult.Loading -> {
                    // Just keep the loading indicator visible
                    binding.progressBar.visibility = View.VISIBLE
                    binding.linearResults.visibility = View.GONE
                }
                is ResultViewModel.AnalyzeResult.Success -> {
                    // Hide loading, show results
                    binding.progressBar.visibility = View.GONE
                    binding.linearResults.visibility = View.VISIBLE

                    // Set the text fields
                    binding.tvQuestion.text = result.question
                    binding.tvAnswer.text = result.answer
                }
                is ResultViewModel.AnalyzeResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.linearResults.visibility = View.VISIBLE

                    // Show error message but still display the image
                    binding.tvQuestion.text = "Analysis failed"
                    binding.tvAnswer.text = "Error: ${result.message}\n\nYou can post to the community for help."
                    Toast.makeText(this, "Analysis error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.linearResults.visibility = View.GONE
    }

    private fun uploadImageToServer() {
        imagePath?.let { path ->
            viewModel.uploadImage(File(path))
        }
    }

    private fun setupClickListeners() {
        // Setup community post button
        binding.btnPostCommunity.setOnClickListener {
            // Toggle button state
            if (binding.btnPostCommunity.text.contains("Post")) {
                binding.btnPostCommunity.text = "Posted to community"
                binding.btnPostCommunity.isEnabled = false
                Toast.makeText(this, "Your question has been posted to the community", Toast.LENGTH_SHORT).show()
            }
        }

        // If we need to manually retry upload
//        binding.btnRetry.setOnClickListener {
//            // Get the file and manually upload
//            imagePath?.let { path ->
//                viewModel.manuallyUploadImage(File(path))
//            }
//        }
    }
}