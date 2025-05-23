package com.example.app_music.data.remote.websocket

import android.util.Log
import com.example.app_music.domain.model.NotificationDTO
import com.example.app_music.domain.model.WebSocketMessage
import com.example.app_music.domain.utils.ApiInstance
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompMessage

class WebSocket  {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()
    private var mIsConnected = false

    companion object {
        private const val TAG = "WebSocket"
    }

    fun connect(userId: Long): Flow<WebSocketMessage> = callbackFlow {

        if (stompClient?.isConnected == true) {
            Log.d(TAG, "Already connected. Reusing existing connection.")

            val userTopic = "/user/$userId/queue/notifications"
            compositeDisposable.add(stompClient!!.topic(userTopic)
                .subscribe({ stompMessage: StompMessage ->
                    Log.d(TAG, "Received from $userTopic: ${stompMessage.payload}")
                    try {
                        // Server sends a NotificationDTO directly, not a WebSocketMessage
                        val notificationDTO = gson.fromJson(stompMessage.payload, NotificationDTO::class.java)

                        // Create a WebSocketMessage with the parsed NotificationDTO
                        val webSocketMessage = WebSocketMessage(
                            type = "notification",
                            notification = notificationDTO,
                            unreadCount = null
                        )

                        Log.d(TAG, "Parsed notification: $notificationDTO")
                        Log.d(TAG, "Created message: $webSocketMessage")

                        trySend(webSocketMessage).isSuccess
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing STOMP message from $userTopic: ${e.message}", e)
                        e.printStackTrace()
                    }
                },
                    { throwable ->
                        Log.e(TAG, "Error on $userTopic subscription: ${throwable.message}")
                    })
            )
            Log.d(TAG, "Subscribed to $userTopic with existing connection")


            awaitClose {
                Log.d(TAG, "Flow closing, but keeping STOMP connection as it may be used elsewhere.")

            }
            return@callbackFlow
        }


        val serverUrl = ApiInstance.baseUrl.replace("http:", "ws:").replace("https:", "wss:") + "/ws/websocket"
        Log.d(TAG, "Attempting to connect to STOMP server: $serverUrl")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, serverUrl)

        val lifecycleDisposable = stompClient!!.lifecycle().subscribe { lifecycleEvent: LifecycleEvent ->

            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "STOMP Connection Opened")
                    mIsConnected = true
                    val userTopic = "/user/$userId/queue/notifications"
                    compositeDisposable.add(stompClient!!.topic(userTopic)
                        .subscribe({ stompMessage: StompMessage ->
                            Log.d(TAG, "Received from $userTopic: ${stompMessage.payload}")
                            try {
                                // Server sends a NotificationDTO directly, not a WebSocketMessage
                                val notificationDTO = gson.fromJson(stompMessage.payload, NotificationDTO::class.java)

                                // Create a WebSocketMessage with the parsed NotificationDTO
                                val webSocketMessage = WebSocketMessage(
                                    type = "notification",
                                    notification = notificationDTO,
                                    unreadCount = null
                                )

                                Log.d(TAG, "Parsed notification: $notificationDTO")
                                Log.d(TAG, "Created message: $webSocketMessage")

                                trySend(webSocketMessage).isSuccess
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing STOMP message from $userTopic: ${e.message}", e)
                                e.printStackTrace()
                            }
                        },
                            { throwable ->
                                Log.e(TAG, "Error on $userTopic subscription: ${throwable.message}")
                            })
                    )
                    Log.d(TAG, "Subscribed to $userTopic")
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "STOMP Connection Error: " + lifecycleEvent.exception.message)
                    mIsConnected = false
                    close(lifecycleEvent.exception)
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "STOMP Connection Closed")
                    mIsConnected = false
                    close() // Close the flow
                }
                LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                    Log.w(TAG, "STOMP Failed Server Heartbeat")
                    mIsConnected = false
                }
            }
        }
        compositeDisposable.add(lifecycleDisposable)

        stompClient!!.connect()

        awaitClose {
            Log.d(TAG, "Flow closing, disconnecting STOMP.")
            disconnect()
        }
    }
    fun disconnect() {
        Log.d(TAG, "Disconnecting STOMP client.")
        compositeDisposable.clear()
        stompClient?.disconnect()
        stompClient = null
        mIsConnected = false
    }

    fun isConnected(): Boolean {
        return mIsConnected
    }
}