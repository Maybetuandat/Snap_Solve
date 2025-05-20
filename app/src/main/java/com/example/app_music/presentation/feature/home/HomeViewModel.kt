package com.example.app_music.presentation.feature.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.SearchHistoryRepository
import com.example.app_music.domain.model.SearchHistory
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val searchHistoryRepository = SearchHistoryRepository()

    private val _searchHistory = MutableLiveData<List<SearchHistory>>()
    val searchHistory: LiveData<List<SearchHistory>> = _searchHistory

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadSearchHistory(context: Context) {
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            return
        }

        viewModelScope.launch {
            try {
                val response = searchHistoryRepository.getSearchHistory(userId, 3)

                if (response.isSuccessful && response.body() != null) {
                    _searchHistory.value = response.body()
                } else {
                    _errorMessage.value = "Failed to load search history: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            }
        }
    }
}