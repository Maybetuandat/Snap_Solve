package com.example.app_music.presentation.feature.camera

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.app_music.MainActivity
import com.example.app_music.R
import com.example.app_music.databinding.ActivityResultBinding
import com.example.app_music.domain.model.Assignment
import com.example.app_music.presentation.feature.common.BaseActivity
import com.example.app_music.presentation.feature.menu.premiumuser.PremiumUser
import java.io.File


class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var viewModel: ResultViewModel
    private var imagePath: String? = null
    private var searchQuery: String? = null
    private var isTextSearch: Boolean = false
    private var fromHistory: Boolean = false
    private lateinit var pageIndicators: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ResultViewModel::class.java]
        setupToolbarNavigation()
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
        fromHistory = intent.getBooleanExtra("FROM_HISTORY", false)

        if (isTextSearch || fromHistory) {
            searchQuery?.let {
                binding.tvSearchQuery.text = it
                binding.tvSearchQuery.visibility = View.VISIBLE
            }
        } else {
            binding.tvSearchQuery.visibility = View.GONE
        }

        // Setup observers
        setupObservers()

        if (fromHistory) {
            // Handle opening from search history
            setupFromHistory()
        } else if (isTextSearch && !searchQuery.isNullOrEmpty()) {
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

    private fun setupToolbarNavigation() {
        // Set back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Set home button
        binding.btnHome.setOnClickListener {
            // Tạo Intent để mở MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                // Xóa tất cả các activities trước đó
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            // Đóng ResultActivity
            finish()
        }
    }


    private fun setupFromHistory() {
        // Show loading state
        showLoadingState()

        // Get image URL if exists
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        if (!imageUrl.isNullOrEmpty()) {
            // Show image from URL
            binding.imageCropped.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(binding.imageCropped)
        } else {
            // Hide image for text searches
            binding.imageCropped.visibility = View.GONE
        }
        searchQuery?.let {
            binding.tvSearchQuery.text = it
            binding.tvSearchQuery.visibility = View.VISIBLE
        }

        // Get assignment IDs
        val assignmentIds = mutableListOf<Long>()
        val id1 = intent.getLongExtra("ASSIGNMENT_ID_1", 0)
        val id2 = intent.getLongExtra("ASSIGNMENT_ID_2", 0)
        val id3 = intent.getLongExtra("ASSIGNMENT_ID_3", 0)
        val id4 = intent.getLongExtra("ASSIGNMENT_ID_4", 0)
        val id5 = intent.getLongExtra("ASSIGNMENT_ID_5", 0)

        // Add valid IDs to the list
        if (id1 > 0) assignmentIds.add(id1)
        if (id2 > 0) assignmentIds.add(id2)
        if (id3 > 0) assignmentIds.add(id3)
        if (id4 > 0) assignmentIds.add(id4)
        if (id5 > 0) assignmentIds.add(id5)

        // Load assignments if we have IDs
        if (assignmentIds.isNotEmpty()) {
            viewModel.loadAssignmentsByIds(assignmentIds)
        } else {
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

    private fun setupForTextSearch() {
        // Hide image card for text search
        binding.imageCropped.visibility = View.GONE

        searchQuery?.let {
            binding.tvSearchQuery.text = it
            binding.tvSearchQuery.visibility = View.VISIBLE
        }

        showLoadingState()
        searchByText()
    }

    private fun setupForImageSearch() {
        // Show and load image
        displayCroppedImage()

        // Show loading and upload
        showLoadingState()
        uploadImageToServer()
    }

    private fun searchByText() {
        searchQuery?.let { query ->
            viewModel.searchByText(query, this)
        }
    }

    private fun displayCroppedImage() {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.imageCropped.setImageBitmap(bitmap)
            binding.imageCropped.visibility = View.VISIBLE
        } catch (e: Exception) {
            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.imageCropped.visibility = View.GONE
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

        // Observe AI solution status
        viewModel.aiSolutionStatus.observe(this) { status ->
            when (status) {
                is ResultViewModel.AISolutionStatus.Loading -> {
                    binding.aiResponseSection.visibility = View.VISIBLE
                    binding.aiProgressBar.visibility = View.VISIBLE
                    binding.webViewAIResult.visibility = View.GONE
                }
                is ResultViewModel.AISolutionStatus.Success -> {
                    binding.aiProgressBar.visibility = View.GONE
                    binding.webViewAIResult.visibility = View.VISIBLE
                    displayAISolution(status.solution)
                }
                is ResultViewModel.AISolutionStatus.Error -> {
                    binding.aiProgressBar.visibility = View.GONE
                    binding.webViewAIResult.visibility = View.VISIBLE
                    displayAIError(status.message)
                }
                is ResultViewModel.AISolutionStatus.PremiumRequired -> {
                    binding.aiProgressBar.visibility = View.GONE
                    showPremiumRequiredDialog()
                }
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

    private fun displayAISolution(solution: String) {
        // Configure AI WebView
        binding.webViewAIResult.settings.javaScriptEnabled = true

        // Process the AI solution to handle LaTeX syntax
        val processedSolution = preprocessLatex(solution)

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
                    line-height: 1.5;
                    font-size: 14px;
                    color: #333;
                    padding: 8px;
                    background-color: #f8f9fa;
                    border-radius: 8px;
                }
                h1, h2, h3, h4 { 
                    color: #C67C4E;
                    margin-top: 16px; 
                    margin-bottom: 8px; 
                }
                p { margin-bottom: 12px; }
                ol, ul { margin-left: 20px; margin-bottom: 12px; }
                li { margin-bottom: 8px; }
                .step { 
                    background-color: #fff; 
                    padding: 12px; 
                    margin: 8px 0; 
                    border-left: 4px solid #C67C4E; 
                    border-radius: 4px;
                }
                .formula { 
                    background-color: #f0f7ff; 
                    padding: 8px; 
                    margin: 8px 0; 
                    border-radius: 4px; 
                    text-align: center;
                }
            </style>
        </head>
        <body>
            ${processedSolution}
        </body>
        </html>
        """.trimIndent()

        // Load content into AI WebView
        binding.webViewAIResult.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun displayAIError(message: String) {
        val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { 
                    font-family: Arial, sans-serif; 
                    line-height: 1.4;
                    font-size: 14px;
                    color: #dc3545;
                    padding: 16px;
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <h3>Error</h3>
            <p>$message</p>
            <p>Please try again later.</p>
        </body>
        </html>
        """.trimIndent()

        binding.webViewAIResult.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun showPremiumRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.premium_required))
            .setMessage(getString(R.string.premium_required_message))
            .setPositiveButton(getString(R.string.upgrade_now)) { _, _ ->
                // Navigate to Premium User Activity
                val intent = Intent(this, PremiumUser::class.java)
                startActivity(intent)
            }
            .setNegativeButton(getString(R.string.maybe_later)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
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
            viewModel.uploadImage(File(path), this)
        }
    }

    private fun setupClickListeners() {
        // Setup community post button
        binding.btnPostCommunity.setOnClickListener {
            // Tạo Intent để mở MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                // Thêm thông tin để chuyển hướng đến CommunityPostingFragment
                putExtra("NAVIGATE_TO_POSTING", true)

                // Thêm đường dẫn hình ảnh (từ máy hoặc URL)
                if (imagePath != null) {
                    putExtra("IMAGE_PATH", imagePath)
                } else {
                    // Nếu không có imagePath, sử dụng URL của hình ảnh đã tải lên (nếu có)
                    val uploadStatus = viewModel.uploadStatus.value
                    if (uploadStatus is ResultViewModel.UploadStatus.Success && !uploadStatus.imageUrl.isNullOrEmpty()) {
                        putExtra("IMAGE_URL", uploadStatus.imageUrl)
                    }
                }

                // Thêm nội dung câu hỏi nếu có
                if (!searchQuery.isNullOrEmpty()) {
                    putExtra("QUESTION_TEXT", searchQuery)
                } else {
                    // Nếu không có searchQuery, sử dụng nội dung từ assignment hiện tại nếu có
                    val assignments = viewModel.assignments.value
                    val currentIndex = viewModel.currentAssignmentIndex.value ?: 0
                    if (assignments != null && assignments.isNotEmpty() && currentIndex < assignments.size) {
                        putExtra("QUESTION_TEXT", assignments[currentIndex].question)
                    }
                }
            }

            startActivity(intent)
            // Không đóng ResultActivity để người dùng có thể quay lại nếu cần
        }

        // Setup Ask AI button
        binding.btnAskAI.setOnClickListener {
            val query = searchQuery ?: binding.tvSearchQuery.text.toString()
            if (query.isNotEmpty()) {
                viewModel.getAISolution(query, this)
            } else {
                Toast.makeText(this, "No question available for AI analysis", Toast.LENGTH_SHORT).show()
            }
        }
    }
}