package com.example.app_music.presentation.feature.textsearch

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.app_music.databinding.ActivityTextSearchBinding
import com.example.app_music.presentation.feature.camera.ResultActivity
import com.example.app_music.presentation.feature.common.BaseActivity

class TextSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityTextSearchBinding
    private lateinit var viewModel: TextSearchViewModel

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
                binding.btnSearch.isEnabled = !s.isNullOrBlank()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Initially disable search button
        binding.btnSearch.isEnabled = false
    }

    private fun setupObservers() {
        viewModel.searchStatus.observe(this) { status ->
            when (status) {
                is TextSearchViewModel.SearchStatus.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSearch.isEnabled = false
                    binding.editTextSearch.isEnabled = false
                }
                is TextSearchViewModel.SearchStatus.Success -> {
                    // Navigate to ResultActivity with the text query
                    val intent = Intent(this, ResultActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", binding.editTextSearch.text.toString())
                        putExtra("IS_TEXT_SEARCH", true)
                    }
                    startActivity(intent)
                    finish()
                }
                is TextSearchViewModel.SearchStatus.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSearch.isEnabled = true
                    binding.editTextSearch.isEnabled = true
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSearch.setOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchByText(query)
            }
        }

        // Handle enter key press
        binding.editTextSearch.setOnEditorActionListener { _, _, _ ->
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchByText(query)
                true
            } else {
                false
            }
        }
    }
}