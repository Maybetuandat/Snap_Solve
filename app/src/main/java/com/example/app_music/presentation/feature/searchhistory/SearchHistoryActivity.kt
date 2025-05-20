package com.example.app_music.presentation.feature.searchhistory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app_music.databinding.ActivitySearchHistoryBinding
import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.utils.UrlUtils
import com.example.app_music.presentation.feature.camera.ResultActivity
import com.example.app_music.presentation.feature.common.BaseActivity

class SearchHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchHistoryBinding
    private lateinit var viewModel: SearchHistoryViewModel
    private lateinit var adapter: SearchHistoryAdapter
    private var isLoadingMore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[SearchHistoryViewModel::class.java]

        // Initialize RecyclerView and adapter
        setupRecyclerView()

        // Setup observers
        setupObservers()

        // Setup listeners
        setupListeners()

        // Load initial data
        viewModel.initializeSearchHistory(this)
    }

    private fun setupRecyclerView() {
        adapter = SearchHistoryAdapter { searchHistory ->
            openSearchResult(searchHistory)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(this@SearchHistoryActivity)
            adapter = this@SearchHistoryActivity.adapter

            // Add scroll listener for pagination
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // Check if we've scrolled to near the end of the list
                    if (!isLoadingMore && viewModel.hasMoreData.value == true) {
                        if ((visibleItemCount + firstVisibleItemPosition + 5) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                            // Load more data
                            viewModel.loadNextPage(this@SearchHistoryActivity)
                        }
                    }
                }
            })
        }
    }

    private fun setupObservers() {
        // Observe search history
        viewModel.searchHistory.observe(this) { history ->
            adapter.submitList(history.toMutableList())

            // Show/hide empty state
            updateEmptyState(history.isEmpty())

            // Stop refresh animation if active
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Observe loading state for initial load
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            if (isLoading) {
                // Hide empty state while loading
                binding.emptyStateView.visibility = View.GONE
            }
        }

        // Observe loading state for pagination
        viewModel.isLoadingMore.observe(this) { isLoadingMore ->
            this.isLoadingMore = isLoadingMore
            // You could show a footer loading indicator in the RecyclerView if desired
        }

        // Observe errors
        viewModel.errorMessage.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            binding.swipeRefreshLayout.isRefreshing = false
        }

        // Observe has more data
        viewModel.hasMoreData.observe(this) { hasMore ->
            // Could show "end of list" indicator if hasMore is false
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.recyclerViewHistory.visibility = View.GONE
        } else {
            binding.emptyStateView.visibility = View.GONE
            binding.recyclerViewHistory.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Search text changed
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchHistory(this@SearchHistoryActivity, s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Pull to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshSearchHistory(this)
        }
    }

    private fun openSearchResult(searchHistory: SearchHistory) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            // Determine if it's a text or image search
            if (searchHistory.image.isNullOrEmpty()) {
                putExtra("SEARCH_QUERY", searchHistory.question)
                putExtra("IS_TEXT_SEARCH", true)
            } else {
                putExtra("SEARCH_QUERY", searchHistory.question)
                putExtra("IMAGE_URL", UrlUtils.getAbsoluteUrl(searchHistory.image))
                putExtra("FROM_HISTORY", true)
                putExtra("IS_TEXT_SEARCH", false)
            }

            // Add available assignment IDs
            putExtra("ASSIGNMENT_ID_1", searchHistory.assignmentId1)
            putExtra("ASSIGNMENT_ID_2", searchHistory.assignmentId2)
            putExtra("ASSIGNMENT_ID_3", searchHistory.assignmentId3)
            putExtra("ASSIGNMENT_ID_4", searchHistory.assignmentId4)
            putExtra("ASSIGNMENT_ID_5", searchHistory.assignmentId5)
        }
        startActivity(intent)
    }
}