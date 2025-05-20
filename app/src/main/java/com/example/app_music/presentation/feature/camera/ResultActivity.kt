package com.example.app_music.presentation.feature.camera

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.R
import com.example.app_music.databinding.ActivityResultBinding
import com.example.app_music.domain.model.Assignment
import com.example.app_music.presentation.feature.common.BaseActivity
import java.io.File


class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: ResultViewModel
    private var imagePath: String? = null
    private var searchQuery: String? = null
    private var isTextSearch: Boolean = false
    private lateinit var pageIndicators: List<TextView>

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

        // Initialize page indicators
        pageIndicators = listOf(
            binding.pageIndicator1,
            binding.pageIndicator2,
            binding.pageIndicator3,
            binding.pageIndicator4,
            binding.pageIndicator5
        )

        // Set up click listeners for indicators
        pageIndicators.forEachIndexed { index, indicator ->
            indicator.setOnClickListener {
                viewModel.goToAssignment(index)
            }
        }

        // Get data from intent
        imagePath = intent.getStringExtra("IMAGE_PATH")
        searchQuery = intent.getStringExtra("SEARCH_QUERY")
        isTextSearch = intent.getBooleanExtra("IS_TEXT_SEARCH", false)

        if (isTextSearch && !searchQuery.isNullOrEmpty()) {
            // Handle text search
            setupForTextSearch()
        } else if (imagePath != null) {
            // Handle image search
            setupForImageSearch()
        } else {
            // Handle error case
            Toast.makeText(this, "No image or query found", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup button click listeners
        setupClickListeners()
    }

    private fun setupForTextSearch() {
        // Hide image card for text search
        binding.imageCropped.visibility = View.GONE

        // Setup observers and search
        setupObservers()
        showLoadingState()
        searchByText()
    }

    private fun setupForImageSearch() {
        // Show and load image
        displayCroppedImage()

        // Setup observers and upload
        setupObservers()
        showLoadingState()
        uploadImageToServer()
    }

    private fun searchByText() {
        searchQuery?.let { query ->
            viewModel.searchByText(query)
        }
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
                    // If we have assignments, they'll be shown by the assignments observer
                    // Otherwise, keep showing loading
                    if (binding.linearResults.visibility != View.VISIBLE) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is ResultViewModel.UploadStatus.Error -> {
                    // Show error
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                    // Still show the image
                    binding.linearResults.visibility = View.VISIBLE
                }
            }
        }

        // Observe assignments
        viewModel.assignments.observe(this) { assignments ->
            if (!assignments.isNullOrEmpty()) {
                // Update pagination visibility
                updatePaginationVisibility(assignments.size)

                // Current assignment will be shown by the currentAssignmentIndex observer
                binding.progressBar.visibility = View.GONE
                binding.linearResults.visibility = View.VISIBLE
            } else {
                // Hide pagination if no assignments
                updatePaginationVisibility(0)

                if (viewModel.uploadStatus.value is ResultViewModel.UploadStatus.Success) {
                    // Show empty state if upload was successful
                    binding.progressBar.visibility = View.GONE
                    binding.linearResults.visibility = View.VISIBLE
                    binding.webViewResult.loadData(
                        "<html><body><h3>No matching assignments found</h3></body></html>",
                        "text/html",
                        "UTF-8"
                    )
                    binding.webViewResult.visibility = View.VISIBLE
                }
            }
        }

        // Observe current assignment index
        viewModel.currentAssignmentIndex.observe(this) { index ->
            val assignments = viewModel.assignments.value ?: emptyList()
            if (assignments.isNotEmpty() && index < assignments.size) {
                // Update pagination indicators
                updatePaginationIndicators(index, assignments.size)

                // Display the current assignment
                displayAssignment(assignments[index])
            }
        }
    }

    private fun updatePaginationVisibility(count: Int) {
        // Show/hide pagination based on assignment count
        val paginationContainer = binding.paginationContainer
        paginationContainer.visibility = if (count > 0) View.VISIBLE else View.GONE

        // Update which indicators are visible
        pageIndicators.forEachIndexed { i, indicator ->
            indicator.visibility = if (i < count) View.VISIBLE else View.GONE
        }
    }

    private fun updatePaginationIndicators(currentIndex: Int, totalCount: Int) {
        // Update styling of indicators
        pageIndicators.forEachIndexed { index, indicator ->
            if (index < totalCount) {
                if (index == currentIndex) {
                    indicator.background = ContextCompat.getDrawable(this, R.drawable.circle_selected)
                    indicator.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    indicator.background = ContextCompat.getDrawable(this, R.drawable.circle_unselected)
                    indicator.setTextColor(ContextCompat.getColor(this, R.color.black))
                }
            }
        }
    }

    private fun displayAssignment(assignment: Assignment) {
        // Configure WebView
        binding.webViewResult.visibility = View.VISIBLE
        binding.webViewResult.settings.javaScriptEnabled = true

        // Preprocess question and answer to handle LaTeX syntax
        // Note: The server response has double backslashes which we need to handle
        val processedQuestion = preprocessLatex(assignment.question)
        val processedAnswer = preprocessLatex(assignment.answer)

        // Create HTML content with MathJax for rendering LaTeX
        val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script type="text/javascript" async src="https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML"></script>
            <script type="text/x-mathjax-config">
                MathJax.Hub.Config({
                    tex2jax: {
                        inlineMath: [['\\(','\\)']],
                        displayMath: [['\\[','\\]']]
                    }
                });
            </script>
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    line-height: 1.4;
                    font-size: 14px;
                    color: #333;
                    padding: 8px;
                }
                h3 { 
                    margin-top: 8px; 
                    margin-bottom: 12px; 
                    color: #000;
                    font-size: 16px;
                }
                p { margin-bottom: 16px; }
                br { 
                    display: block;
                    margin-top: 8px; 
                }
            </style>
        </head>
        <body>
            <h3>Question:</h3>
            <p>${processedQuestion}</p>
            
            <h3>Answer:</h3>
            <p>${processedAnswer}</p>
        </body>
        </html>
        """.trimIndent()

        // Load content into WebView
        binding.webViewResult.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    // Function to handle LaTeX formatting
    private fun preprocessLatex(text: String): String {

        // Thử nhiều phương pháp thay thế khác nhau
        var processed = text

        // 1. Thay thế ký tự xuống dòng thực sự (ASCII 10)
        processed = processed.replace("\n", "<br>")

        // 2. Thay thế chuỗi "\n" (gồm 2 ký tự)
        processed = processed.replace("\\n", "<br>")

        // 3. Xử lý các dấu gạch chéo ngược của LaTeX
        processed = processed.replace("\\\\", "\\")

        return processed
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
    }
}