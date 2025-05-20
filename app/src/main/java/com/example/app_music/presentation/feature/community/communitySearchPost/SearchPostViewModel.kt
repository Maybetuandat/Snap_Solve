package com.example.app_music.presentation.feature.community.communitySearchPost

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchPostViewModel : ViewModel() {
    private val gson = Gson()
    private val PREFS_NAME = "search_history_prefs"
    private val SEARCH_HISTORY_KEY = "search_history"
    private val MAX_HISTORY_SIZE = 10

    private val _recentSearches = MutableLiveData<List<RecentSearch>>()
    val recentSearches: LiveData<List<RecentSearch>> = _recentSearches

    // Đọc lịch sử tìm kiếm từ SharedPreferences
    fun loadRecentSearches(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val searchHistoryJson = prefs.getString(SEARCH_HISTORY_KEY, null)

        if (searchHistoryJson != null) {
            try {
                val type = object : TypeToken<List<RecentSearch>>() {}.type
                val searches = gson.fromJson<List<RecentSearch>>(searchHistoryJson, type)
                _recentSearches.value = searches
            } catch (e: Exception) {
                Log.e("SearchPostViewModel", "Lỗi khi đọc lịch sử tìm kiếm", e)
                _recentSearches.value = emptyList()
            }
        } else {
            _recentSearches.value = emptyList()
        }
    }

    // Lưu từ khóa vào lịch sử tìm kiếm
    fun saveToRecentSearches(query: String, context: Context) {
        val currentSearches = _recentSearches.value?.toMutableList() ?: mutableListOf()

        // Kiểm tra xem từ khóa đã tồn tại trong lịch sử chưa
        val existingIndex = currentSearches.indexOfFirst { it.query.equals(query, ignoreCase = true) }
        if (existingIndex != -1) {
            // Nếu đã tồn tại, xóa và thêm lại vào đầu danh sách (để cập nhật thời gian)
            currentSearches.removeAt(existingIndex)
        }

        // Thêm từ khóa mới vào đầu danh sách
        currentSearches.add(0, RecentSearch(query))

        // Giới hạn số lượng lịch sử
        while (currentSearches.size > MAX_HISTORY_SIZE) {
            currentSearches.removeAt(currentSearches.size - 1)
        }

        // Cập nhật LiveData
        _recentSearches.value = currentSearches

        // Lưu vào SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(SEARCH_HISTORY_KEY, gson.toJson(currentSearches))
        editor.apply()
    }

    // Xóa tất cả lịch sử tìm kiếm
    fun clearRecentSearches(context: Context) {
        _recentSearches.value = emptyList()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(SEARCH_HISTORY_KEY)
        editor.apply()
    }


}