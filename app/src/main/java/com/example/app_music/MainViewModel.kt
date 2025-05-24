package com.example.app_music

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.app_music.data.local.preferences.UserPreference
import com.example.app_music.data.remote.websocket.WebSocket
import com.example.app_music.data.repository.NotificationRepositoryImpl
import com.example.app_music.domain.model.WebSocketMessage
import com.example.app_music.domain.usecase.notification.ConnectWebSocketUseCase
import com.example.app_music.domain.usecase.notification.GetUnreadCountUseCase
import com.example.app_music.domain.utils.RetrofitFactory
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NotificationViewModel"
    private val gson = Gson()

    private val _unreadNotificationCount = MutableLiveData<Long>(0)
    val unreadNotificationCount: LiveData<Long> = _unreadNotificationCount

    private val _newNotification = MutableLiveData<Pair<String, String>>()
    val newNotification: LiveData<Pair<String, String>> = _newNotification

    private val connectWebSocketUseCase: ConnectWebSocketUseCase
    private val getUnreadCountUseCase: GetUnreadCountUseCase

    init {
        val webSocketRepository = WebSocket()
        val notificationRepository = NotificationRepositoryImpl(RetrofitFactory.notificationApi)

        connectWebSocketUseCase = ConnectWebSocketUseCase(webSocketRepository)
        getUnreadCountUseCase = GetUnreadCountUseCase(notificationRepository)


        connectWebSocket()
    }

    fun connectWebSocket() {
        val userId = UserPreference.getUserId(getApplication())
        if (userId == 0L) {
            Log.w(TAG, "User not logged in, skipping WebSocket connection")
            return
        }

        viewModelScope.launch {
            try {
                connectWebSocketUseCase(userId)
                    .catch { e ->
                        Log.e(TAG, "WebSocket error: ${e.message}")
                    }
                    .collect { message ->
                        // Log full message with JSON
                        Log.d(TAG, "Received WebSocket message: ${gson.toJson(message)}")
                        handleWebSocketMessage(message)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect WebSocket: ${e.message}")
            }
        }
    }

    private fun handleWebSocketMessage(message: WebSocketMessage) {
        Log.d(TAG, "Processing message type: ${message.type}")

        when (message.type) {
            "notification" -> {

                message.notification?.let { notification ->
                    Log.d(TAG, "Notification details - Title: ${notification.title}, Content: ${notification.content}")
                    _newNotification.postValue(Pair(notification.title, notification.content))


                    loadUnreadNotificationCount()
                } ?: run {
                    Log.e(TAG, "Received notification message but notification object is null")
                }
            }
            "unread_count" -> {

                message.unreadCount?.let { count ->
                    Log.d(TAG, "Updating unread count to: $count")
                    _unreadNotificationCount.postValue(count)
                } ?: run {
                    Log.e(TAG, "Received unread_count message but count is null")
                }
            }
            else -> {
                Log.w(TAG, "Unknown message type: ${message.type}")
            }
        }
    }

    fun loadUnreadNotificationCount() {
        val userId = UserPreference.getUserId(getApplication())
        if (userId == 0L) return

        viewModelScope.launch {
            try {
                val response = getUnreadCountUseCase(userId)
                if (response.isSuccessful) {
                    val count = response.body() ?: 0L
                    Log.d(TAG, "Loaded unread count from API: $count")
                    _unreadNotificationCount.postValue(count)
                } else {
                    Log.e(TAG, "Failed to load unread count: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load unread count: ${e.message}")
            }
        }
    }

    fun refreshNotifications() {
        loadUnreadNotificationCount()
    }

    fun isWebSocketConnected(): Boolean {
        return connectWebSocketUseCase.isConnected()
    }

    override fun onCleared() {
        super.onCleared()
        connectWebSocketUseCase.disconnect()
    }
}