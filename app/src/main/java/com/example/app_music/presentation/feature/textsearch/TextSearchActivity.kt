package com.example.app_music.presentation.feature.textsearch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.databinding.ActivityTextSearchBinding
import com.example.app_music.presentation.feature.camera.ResultActivity
import com.example.app_music.presentation.feature.common.BaseActivity

class TextSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityTextSearchBinding
    private lateinit var viewModel: TextSearchViewModel
    private val MIN_QUERY_LENGTH = 20
    private val CLICK_DELAY = 1000L // 1 giây
    private var isButtonClickable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[TextSearchViewModel::class.java]

        setupViews()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViews() {
        // Focus on search input
        binding.editTextSearch.requestFocus()

        // Add text watcher to enable/disable search button
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputLength = s?.length ?: 0
                binding.btnSearch.isEnabled = inputLength >= MIN_QUERY_LENGTH

                // Hiển thị thông báo về độ dài
                if (inputLength > 0 && inputLength < MIN_QUERY_LENGTH) {
                    binding.tvInputError.visibility = View.VISIBLE
                    binding.tvInputError.text = "Please enter at least $MIN_QUERY_LENGTH characters. Current: $inputLength"
                } else {
                    binding.tvInputError.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Initially disable search button
        binding.btnSearch.isEnabled = false
    }

    private fun setupObservers() {

    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSearch.setOnClickListener {
            handleSearchClick()
        }

        // Handle enter key press
        binding.editTextSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (isButtonClickable) {
                    handleSearchClick()
                    return@setOnEditorActionListener true
                }
            }
            return@setOnEditorActionListener false
        }
    }

    private fun handleSearchClick() {
        val query = binding.editTextSearch.text.toString().trim()
        if (validateQuery(query)) {
            // Vô hiệu hóa nút search
            disableSearchButton()

            // KHÔNG gọi viewModel.searchByText, mà chuyển thẳng đến ResultActivity
            val intent = Intent(this, ResultActivity::class.java).apply {
                putExtra("SEARCH_QUERY", query)
                putExtra("IS_TEXT_SEARCH", true)
            }
            startActivity(intent)

            // Không finish() ở đây để người dùng có thể quay lại màn hình search

            // Kích hoạt lại nút search sau 1 giây
            Handler(Looper.getMainLooper()).postDelayed({
                enableSearchButton()
            }, CLICK_DELAY)
        }
    }

    private fun disableSearchButton() {
        isButtonClickable = false
        binding.btnSearch.isEnabled = false
        binding.btnSearch.alpha = 0.5f  // Làm mờ nút để thể hiện trạng thái vô hiệu hóa
    }

    private fun enableSearchButton() {
        isButtonClickable = true
        // Chỉ kích hoạt nút nếu độ dài đủ
        val inputLength = binding.editTextSearch.text?.length ?: 0
        binding.btnSearch.isEnabled = inputLength >= MIN_QUERY_LENGTH
        binding.btnSearch.alpha = 1.0f  // Khôi phục độ trong suốt
    }

    private fun validateQuery(query: String): Boolean {
        if (query.length < MIN_QUERY_LENGTH) {
            Toast.makeText(
                this,
                "Please enter at least $MIN_QUERY_LENGTH characters. Current: ${query.length}",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }
}