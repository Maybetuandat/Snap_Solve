package com.example.app_music.presentation.feature.searchhistory

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.SearchHistoryRepository
import com.example.app_music.domain.model.SearchHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchHistoryViewModel : ViewModel() {

    private companion object {
        private const val TAG = "SearchHistoryViewModel"
    }

    private val searchHistoryRepository = SearchHistoryRepository()

    private val _searchHistory = MutableLiveData<List<SearchHistory>>()
    val searchHistory: LiveData<List<SearchHistory>> = _searchHistory

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _hasMoreData = MutableLiveData(true)
    val hasMoreData: LiveData<Boolean> = _hasMoreData

    private var searchJob: Job? = null
    private var currentPage = 0
    private var currentQuery: String? = null
    private val pageSize = 10

    // Initialize load with real data
    fun initializeSearchHistory(context: Context) {
        Log.d(TAG, "initializeSearchHistory called")
        _isLoading.value = true
        currentPage = 0

        val userId = UserPreference.getUserId(context)
        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = searchHistoryRepository.getSearchHistoryPaginated(userId, pageSize, 0)

                if (response.isSuccessful && response.body() != null) {
                    val historyList = response.body() ?: emptyList()
                    _searchHistory.value = historyList
                    _hasMoreData.value = historyList.size >= pageSize
                    Log.d(TAG, "Initial data loaded: ${historyList.size} items")
                } else {
                    _errorMessage.value = "Failed to load search history: ${response.message()}"
                    Log.e(TAG, "Error loading search history: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e(TAG, "Exception during data load", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load next page of data
    fun loadNextPage(context: Context) {
        Log.d(TAG, "loadNextPage called")
        if (_isLoadingMore.value == true || _hasMoreData.value != true) {
            return
        }

        _isLoadingMore.value = true
        currentPage++

        val userId = UserPreference.getUserId(context)
        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            _isLoadingMore.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = if (currentQuery.isNullOrBlank()) {
                    searchHistoryRepository.getSearchHistoryPaginated(userId, pageSize, currentPage)
                } else {
                    searchHistoryRepository.searchHistoryByQueryPaginated(userId, currentQuery!!, pageSize, currentPage)
                }

                if (response.isSuccessful && response.body() != null) {
                    val newItems = response.body() ?: emptyList()

                    // Combine with existing items
                    val currentItems = _searchHistory.value ?: emptyList()
                    val combinedItems = currentItems + newItems

                    _searchHistory.value = combinedItems
                    _hasMoreData.value = newItems.size >= pageSize

                    Log.d(TAG, "Loaded page $currentPage with ${newItems.size} items")
                } else {
                    _errorMessage.value = "Failed to load more data: ${response.message()}"
                    Log.e(TAG, "Error loading more data: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e(TAG, "Exception during pagination", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // Refresh - reload first page of data
    fun refreshSearchHistory(context: Context) {
        Log.d(TAG, "refreshSearchHistory called")
        _isLoading.value = true
        currentPage = 0

        val userId = UserPreference.getUserId(context)
        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = if (currentQuery.isNullOrBlank()) {
                    searchHistoryRepository.getSearchHistoryPaginated(userId, pageSize, 0)
                } else {
                    searchHistoryRepository.searchHistoryByQueryPaginated(userId, currentQuery!!, pageSize, 0)
                }

                if (response.isSuccessful && response.body() != null) {
                    val historyList = response.body() ?: emptyList()
                    _searchHistory.value = historyList
                    _hasMoreData.value = historyList.size >= pageSize
                    Log.d(TAG, "Data refreshed: ${historyList.size} items")
                } else {
                    _errorMessage.value = "Failed to refresh search history: ${response.message()}"
                    Log.e(TAG, "Error refreshing data: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                Log.e(TAG, "Exception during refresh", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Search with query
    fun searchHistory(context: Context, query: String) {
        Log.d(TAG, "searchHistory called with query: '$query'")

        // Cancel previous search job if any
        searchJob?.cancel()

        // Update current query
        currentQuery = query.trim()
        currentPage = 0

        // Show loading
        _isLoading.value = true

        val userId = UserPreference.getUserId(context)
        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            _isLoading.value = false
            return
        }

        // Start new search
        searchJob = viewModelScope.launch {
            // Add a small delay to avoid too many API calls while typing
            delay(300)

            try {
                val response = if (currentQuery.isNullOrBlank()) {
                    searchHistoryRepository.getSearchHistoryPaginated(userId, pageSize, 0)
                } else {
                    searchHistoryRepository.searchHistoryByQueryPaginated(userId, currentQuery!!, pageSize, 0)
                }

                if (response.isSuccessful && response.body() != null) {
                    val searchResults = response.body() ?: emptyList()
                    _searchHistory.value = searchResults
                    _hasMoreData.value = searchResults.size >= pageSize
                    Log.d(TAG, "Search completed with ${searchResults.size} results")
                } else {
                    _errorMessage.value = "Search failed: ${response.message()}"
                    Log.e(TAG, "Error during search: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error during search: ${e.message}"
                Log.e(TAG, "Exception during search", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}