package com.example.app_music.presentation.feature.textsearch

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.TextSearchRepository
import kotlinx.coroutines.launch

class TextSearchViewModel : ViewModel() {
    private val textSearchRepository = TextSearchRepository()

    private val _searchStatus = MutableLiveData<SearchStatus>()
    val searchStatus: LiveData<SearchStatus> = _searchStatus

    fun searchByText(query: String, context: Context) {
        _searchStatus.value = SearchStatus.Loading

        // Get userId from preferences
        val userId = UserPreference.getUserId(context)

        if (userId <= 0) {
            _searchStatus.value = SearchStatus.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val response = textSearchRepository.searchByText(query, userId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.success) {
                        _searchStatus.value = SearchStatus.Success(
                            assignments = result.assignments
                        )
                    } else {
                        _searchStatus.value = SearchStatus.Error(result.message)
                    }
                } else {
                    _searchStatus.value = SearchStatus.Error(
                        "Search failed: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching by text", e)
                _searchStatus.value = SearchStatus.Error(
                    "Search error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    sealed class SearchStatus {
        object Loading : SearchStatus()
        data class Success(val assignments: List<com.example.app_music.domain.model.Assignment>?) : SearchStatus()
        data class Error(val message: String) : SearchStatus()
    }

    companion object {
        private const val TAG = "TextSearchViewModel"
    }
}