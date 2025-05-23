package com.example.app_music.presentation.feature.detail_notification


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.repository.NotificationRepositoryImpl
import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.usecase.notification.DeleteNotificationUseCase
import com.example.app_music.domain.usecase.notification.GetNotificationByIdUseCase
import com.example.app_music.domain.usecase.notification.MarkNotificationAsReadUseCase


import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.launch

class NotificationDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationDetailVM"

    private val _notification = MutableLiveData<Notification>()
    val notification: LiveData<Notification> = _notification

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    private val notificationRepository = NotificationRepositoryImpl(RetrofitFactory.notificationApi)
    private val getNotificationByIdUseCase = GetNotificationByIdUseCase(notificationRepository)
    private val markNotificationAsReadUseCase = MarkNotificationAsReadUseCase(notificationRepository)
    private val deleteNotificationUseCase = DeleteNotificationUseCase(notificationRepository)

    fun loadNotificationDetails(notificationId: Long) {
        _isLoading.value = true
        _error.value = ""

        viewModelScope.launch {
            try {
                val response = getNotificationByIdUseCase(notificationId)
                if (response.isSuccessful) {
                    val notification = response.body()
                    if (notification != null) {
                        _notification.value = notification!!

                        // Mark as read if not already read
                        if (!notification.isRead) {
                            markAsRead(notificationId)
                        }
                    } else {
                        _error.value = "Không tìm thấy thông báo"
                    }
                } else {
                    _error.value = "Lỗi: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
                Log.e(TAG, "Error loading notification details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                markNotificationAsReadUseCase(notificationId)
                // Update local notification object
                _notification.value = _notification.value?.copy(isRead = true)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }

    fun deleteNotification(notificationId: Long) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = deleteNotificationUseCase(notificationId)
                _deleteResult.value = response.isSuccessful
                if (!response.isSuccessful) {
                    _error.value = "Không thể xóa thông báo: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
                _deleteResult.value = false
                Log.e(TAG, "Error deleting notification", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}