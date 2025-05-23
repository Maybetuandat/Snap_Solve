package com.example.app_music.presentation.feature.notification



import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.repository.NotificationRepositoryImpl
import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.usecase.notification.GetNotificationsUseCase
import com.example.app_music.domain.usecase.notification.MarkNotificationsAsReadUseCase
import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.launch

class NotificationActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationActivityVM"

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _showAdBanner = MutableLiveData<Boolean>(true)
    val showAdBanner: LiveData<Boolean> = _showAdBanner

    private val getNotificationsUseCase: GetNotificationsUseCase
    private val markNotificationsAsReadUseCase: MarkNotificationsAsReadUseCase

    init {
        val notificationRepository = NotificationRepositoryImpl(RetrofitFactory.notificationApi)
        getNotificationsUseCase = GetNotificationsUseCase(notificationRepository)
        markNotificationsAsReadUseCase = MarkNotificationsAsReadUseCase(notificationRepository)

        // Check if user is premium to determine whether to show ad
        checkUserPremiumStatus()
    }

    fun loadNotifications() {
        _isLoading.value = true
        _error.value = ""

        val userId = UserPreference.getUserId(getApplication())
        if (userId == 0L) {
            _error.value = "Người dùng chưa đăng nhập"
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            try {
                val response = getNotificationsUseCase(userId)
                if (response.isSuccessful) {
                    val notifications = response.body() ?: emptyList()
                    _notifications.value = notifications
                } else {
                    _error.value = "Lỗi: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
                Log.e(TAG, "Error loading notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAllAsRead() {
        val userId = UserPreference.getUserId(getApplication())
        if (userId == 0L) return

        viewModelScope.launch {
            try {
                val response = markNotificationsAsReadUseCase(userId)
                if (response.isSuccessful) {
                    Log.d(TAG, "Notifications marked as read successfully")
                    Log.d(TAG, "Response body: ${response.body()}")

                    // Refresh notifications to update UI
                    loadNotifications()
                }
            } catch (e: Exception) {
                _error.value = "Lỗi khi đánh dấu đã đọc: ${e.message}"
                Log.e(TAG, "Error marking all as read", e)
            }
        }
    }

    fun hideAdBanner() {
        _showAdBanner.value = false
    }

    private fun checkUserPremiumStatus() {
        // In a real app, you might have a repository method to check this
        // For now, we'll use a simple approach by checking the UserPreference or another source
        val userRank = UserPreference.getUserRank(getApplication())
        _showAdBanner.value = userRank != "premium"
    }

    fun onNotificationClicked(notification: Notification) {
        // Handle notification click - mark as read, etc.
        if (!notification.isRead) {
            markNotificationAsRead(notification.id)
        }

        // Update the local list
        _notifications.value = _notifications.value?.map {
            if (it.id == notification.id) it.copy(isRead = true) else it
        }
    }

    private fun markNotificationAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                // Assuming there's a use case or repository method for this
                // notificationRepository.markAsRead(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }
}