package com.example.app_music.presentation.feature.notification


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.remote.api.NotificationApi
import com.example.app_music.data.repository.NotificationRepositoryImpl
import com.example.app_music.data.repository.WebSocketRepositoryImpl
import com.example.app_music.domain.model.Notification
import com.example.app_music.domain.model.WebSocketMessage
import com.example.app_music.domain.model.WebSocketMessageType
import com.example.app_music.domain.usecase.notification.ConnectWebSocketUseCase
import com.example.app_music.domain.usecase.notification.GetNotificationsUseCase
import com.example.app_music.domain.usecase.notification.GetUnreadCountUseCase
import com.example.app_music.domain.usecase.notification.MarkNotificationsAsReadUseCase
import com.example.app_music.domain.utils.RetrofitFactory
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {

    // Repositories
    private val notificationRepository = NotificationRepositoryImpl(RetrofitFactory.notificationApi)
    private val webSocketRepository = WebSocketRepositoryImpl()

    // Use Cases
    private val getNotificationsUseCase = GetNotificationsUseCase(notificationRepository)
    private val getUnreadCountUseCase = GetUnreadCountUseCase(notificationRepository)
    private val markNotificationsAsReadUseCase = MarkNotificationsAsReadUseCase(notificationRepository)
    private val connectWebSocketUseCase = ConnectWebSocketUseCase(webSocketRepository)

    // LiveData
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _unreadCount = MutableLiveData<Long>(0)
    val unreadCount: LiveData<Long> = _unreadCount

    private val _incomingNotification = MutableLiveData<Notification?>()
    val incomingNotification: LiveData<Notification?> = _incomingNotification

    private val _isWebSocketConnected = MutableLiveData<Boolean>(false)
    val isWebSocketConnected: LiveData<Boolean> = _isWebSocketConnected

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun connectWebSocket(userId: Long) {
        viewModelScope.launch {
            try {
                _isWebSocketConnected.value = true

                connectWebSocketUseCase(userId)
                    .catch { e ->
                        _errorMessage.value = "WebSocket connection error: ${e.message}"
                        _isWebSocketConnected.value = false
                    }
                    .collect { message ->
                        handleWebSocketMessage(message)
                    }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to connect WebSocket: ${e.message}"
                _isWebSocketConnected.value = false
            }
        }
    }

    private fun handleWebSocketMessage(message: WebSocketMessage) {
        when (message.type) {
            WebSocketMessageType.NOTIFICATION.name -> {
                message.notification?.let {
                    _incomingNotification.value = it
                    // Update unread count
                    _unreadCount.value = (_unreadCount.value ?: 0) + 1
                }
            }
            WebSocketMessageType.UNREAD_COUNT.name -> {
                _unreadCount.value = message.unreadCount
            }
            WebSocketMessageType.CONNECTION_STATUS.name -> {
                // Handle connection status if needed
            }
        }
    }

    fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val response = getNotificationsUseCase(userId)

                if (response.isSuccessful && response.body() != null) {
                    _notifications.value = response.body()
                } else {
                    _errorMessage.value = "Failed to load notifications: ${response.message()}"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUnreadCount(userId: Long) {
        viewModelScope.launch {
            try {
                val response = getUnreadCountUseCase(userId)

                if (response.isSuccessful && response.body() != null) {
                    _unreadCount.value = response.body()
                } else {
                    _unreadCount.value = 0
                }

            } catch (e: Exception) {
                _unreadCount.value = 0
                _errorMessage.value = "Error loading unread count: ${e.message}"
            }
        }
    }

    fun markNotificationsAsRead(userId: Long) {
        viewModelScope.launch {
            try {
                val response = markNotificationsAsReadUseCase(userId)

                if (response.isSuccessful) {
                    _unreadCount.value = 0
                    // Reload notifications to update read status
                    loadNotifications(userId)
                } else {
                    _errorMessage.value = "Failed to mark notifications as read: ${response.message()}"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error marking notifications as read: ${e.message}"
            }
        }
    }

    fun disconnectWebSocket() {
        connectWebSocketUseCase.disconnect()
        _isWebSocketConnected.value = false
    }

    fun clearIncomingNotification() {
        _incomingNotification.value = null
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}