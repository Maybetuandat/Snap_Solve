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


    private val getNotificationsUseCase: GetNotificationsUseCase
    private val markNotificationsAsReadUseCase: MarkNotificationsAsReadUseCase

    init {
        val notificationRepository = NotificationRepositoryImpl(RetrofitFactory.notificationApi)
        getNotificationsUseCase = GetNotificationsUseCase(notificationRepository)
        markNotificationsAsReadUseCase = MarkNotificationsAsReadUseCase(notificationRepository)


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



    private fun checkUserPremiumStatus() {

        val userRank = UserPreference.getUserRank(getApplication())
        _showAdBanner.value = userRank != "premium"
    }

    fun onNotificationClicked(notification: Notification) {




        _notifications.value = _notifications.value?.map {
            if (it.id == notification.id) it.copy(isRead = true) else it
        }
    }

}