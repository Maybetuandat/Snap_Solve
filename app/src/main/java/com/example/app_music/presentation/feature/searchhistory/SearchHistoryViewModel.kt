package com.example.app_music.presentation.feature.searchhistory

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.SearchHistory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchHistoryViewModel : ViewModel() {

    private companion object {
        private const val TAG = "SearchHistoryViewModel"
    }

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

    // Dữ liệu mẫu cố định
    private val dummyData = listOf(
        SearchHistory(
            id = 1,
            question = "What is photosynthesis?",
            image = null,
            createDate = "2025-05-20",
            assignmentId1 = 101,
            assignmentId2 = null,
            assignmentId3 = null,
            assignmentId4 = null,
            assignmentId5 = null
        ),
        SearchHistory(
            id = 2,
            question = "Calculate the area of a circle",
            image = "/images/circle.jpg",
            createDate = "2025-05-19",
            assignmentId1 = 102,
            assignmentId2 = 103,
            assignmentId3 = null,
            assignmentId4 = null,
            assignmentId5 = null
        ),
        SearchHistory(
            id = 3,
            question = "How to solve quadratic equations?",
            image = null,
            createDate = "2025-05-18",
            assignmentId1 = 104,
            assignmentId2 = null,
            assignmentId3 = null,
            assignmentId4 = null,
            assignmentId5 = null
        )
    )

    // Initialize load - đặt dữ liệu mẫu ngay từ đầu
    fun initializeSearchHistory(context: Context) {
        Log.d(TAG, "initializeSearchHistory called")
        _isLoading.value = true

        // Đặt dữ liệu mẫu và ẩn loading
        viewModelScope.launch {
            delay(500) // Giả lập delay tải
            _searchHistory.value = dummyData
            Log.d(TAG, "Initial data loaded: ${dummyData.size} items")
            _isLoading.value = false
        }
    }

    // Load next page - để trống vì chúng ta chỉ dùng dữ liệu mẫu
    fun loadNextPage(context: Context) {
        Log.d(TAG, "loadNextPage called - no additional data in test mode")
    }

    // Refresh - chỉ tải lại dữ liệu mẫu
    fun refreshSearchHistory(context: Context) {
        Log.d(TAG, "refreshSearchHistory called")
        _isLoading.value = true

        viewModelScope.launch {
            delay(500) // Giả lập delay làm mới
            _searchHistory.value = dummyData
            Log.d(TAG, "Data refreshed: ${dummyData.size} items")
            _isLoading.value = false
        }
    }

    // Hàm tìm kiếm đơn giản - lọc dữ liệu mẫu sẵn có
    fun searchHistory(context: Context, query: String) {
        Log.d(TAG, "searchHistory called with query: '$query'")

        // Hủy job tìm kiếm trước đó nếu có
        searchJob?.cancel()

        // Cập nhật query hiện tại
        currentQuery = query

        // Hiển thị loading
        _isLoading.value = true

        // Khởi tạo tìm kiếm mới
        searchJob = viewModelScope.launch {
            delay(300) // Giả lập độ trễ mạng

            try {
                // Lọc dữ liệu
                val filteredData = if (query.isBlank()) {
                    dummyData // Nếu query trống, hiển thị tất cả dữ liệu mẫu
                } else {
                    dummyData.filter {
                        it.question.contains(query, ignoreCase = true)
                    }
                }

                Log.d(TAG, "Search completed with ${filteredData.size} results")

                // Cập nhật dữ liệu
                _searchHistory.value = filteredData
            } catch (e: Exception) {
                Log.e(TAG, "Error during search", e)
                _errorMessage.value = "Error during search: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}