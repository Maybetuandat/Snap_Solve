package com.example.app_music.presentation.feature.community.communitySearchPost

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.domain.model.Post
import com.example.app_music.domain.model.Topic
import com.example.app_music.data.repository.PostRepositoryImpl
import com.example.app_music.data.repository.TopicRepositoryImpl
import kotlinx.coroutines.launch

class SearchResultViewModel : ViewModel() {
    private val postRepository = PostRepositoryImpl()
    private val topicRepository = TopicRepositoryImpl()

    private val _searchResults = MutableLiveData<List<Post>>()
    val searchResults: LiveData<List<Post>> = _searchResults

    private val _originalResults = MutableLiveData<List<Post>>()

    private val _topics = MutableLiveData<List<Topic>>()
    val topics: LiveData<List<Topic>> = _topics

    private val _currentTopicFilter = MutableLiveData<Long?>(null)
    val currentTopicFilter: LiveData<Long?> = _currentTopicFilter

    private val _selectedTopicName = MutableLiveData<String>("")
    val selectedTopicName: LiveData<String> = _selectedTopicName

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Loại sắp xếp
    enum class SortType {
        NEWEST, POPULAR
    }

    private var currentSortType = SortType.NEWEST

    init {
        loadTopics()
    }

    // Tải các chủ đề từ API
    private fun loadTopics() {
        viewModelScope.launch {
            try {
                val response = topicRepository.getAllTopics()
                if (response.isSuccessful) {
                    _topics.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Lỗi tải chủ đề: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                Log.e("SearchResultViewModel", "Lỗi khi tải chủ đề", e)
                _error.value = "Lỗi kết nối: ${e.message}"
            }
        }
    }

    // Tìm kiếm bài viết theo từ khóa
    fun searchPosts(keyword: String) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            _originalResults.value = emptyList()
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = postRepository.searchPosts(keyword)
                if (response.isSuccessful) {
                    val results = response.body() ?: emptyList()

                    // Lưu kết quả gốc
                    _originalResults.value = results

                    // Áp dụng sắp xếp
                    val sortedResults = sortResults(results)

                    // Cập nhật LiveData
                    _searchResults.postValue(sortedResults) // Sử dụng postValue thay vì value để đảm bảo cập nhật trên thread chính
                } else {
                    _error.postValue("Lỗi: ${response.code()} - ${response.message()}")
                    _searchResults.postValue(emptyList())
                }
            } catch (e: Exception) {
                _error.postValue("Lỗi kết nối: ${e.message}")
                _searchResults.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Sắp xếp kết quả theo loại đã chọn
    private fun sortResults(posts: List<Post>): List<Post> {
        return when (currentSortType) {
            SortType.NEWEST -> posts.sortedByDescending { it.createDate }
            SortType.POPULAR -> posts.sortedByDescending { it.reactCount }
        }
    }

    // Lọc theo topic
    private fun filterByTopic(posts: List<Post>, topicId: Long): List<Post> {
        return posts.filter { post ->
            post.topics.any { it.id == topicId }
        }
    }

    // Thay đổi loại sắp xếp
    fun changeSortType(sortType: SortType) {
        if (currentSortType != sortType) {
            currentSortType = sortType

            // Áp dụng lại sắp xếp
            _originalResults.value?.let { results ->
                // Sắp xếp lại kết quả
                val sortedResults = sortResults(results)

                // Áp dụng bộ lọc nếu có
                val filteredResults = if (_currentTopicFilter.value != null) {
                    filterByTopic(sortedResults, _currentTopicFilter.value!!)
                } else {
                    sortedResults
                }

                _searchResults.value = filteredResults
            }
        }
    }

    // Cập nhật bộ lọc topic
    fun setTopicFilter(topicId: Long?) {
        // Lưu giá trị topicId
        _currentTopicFilter.value = topicId

        // Cập nhật tên topic đã chọn
        if (topicId == null) {
            _selectedTopicName.value = ""
        } else {
            val selectedTopic = _topics.value?.find { it.id == topicId }
            _selectedTopicName.value = selectedTopic?.name ?: ""
        }

        // Áp dụng lại bộ lọc
        _originalResults.value?.let { results ->
            // Sắp xếp kết quả
            val sortedResults = sortResults(results)

            // Áp dụng bộ lọc nếu có
            val filteredResults = if (topicId != null) {
                filterByTopic(sortedResults, topicId)
            } else {
                sortedResults
            }

            _searchResults.value = filteredResults
        }
    }
}