package com.example.app_music.presentation.feature.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.SearchHistoryRepository
import com.example.app_music.domain.model.SearchHistory
import com.example.app_music.domain.model.User
import com.example.app_music.domain.usecase.GetUserUseCase
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val searchHistoryRepository = SearchHistoryRepository()
    private val getUserUseCase = GetUserUseCase()

    private val _searchHistory = MutableLiveData<List<SearchHistory>>()
    val searchHistory: LiveData<List<SearchHistory>> = _searchHistory

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _currentUser = MutableLiveData<User>()
    val currentUser: LiveData<User> = _currentUser

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

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

    fun loadUserInfo(context: Context) {
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _errorMessage.value = "User not logged in"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = getUserUseCase(userId)

                if (response.isSuccessful && response.body() != null) {
                    _currentUser.value = response.body()
                } else {
                    _errorMessage.value = "Failed to load user info: ${response.message()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}