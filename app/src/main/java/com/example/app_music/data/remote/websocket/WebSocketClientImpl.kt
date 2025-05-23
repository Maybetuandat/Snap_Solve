package com.example.app_music.data.remote.websocket

import android.util.Log
import com.example.app_music.domain.model.WebSocketMessage
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class WebSocketClientImpl : WebSocketClient {

    companion object {
        private const val TAG = "WebSocketClient"
    }

    private var webSocketClient: WebSocketClient? = null
    private val gson = Gson()
    private var isConnected = false

    constructor(serverUri: URI) : super(serverUri)

    override fun onOpen(handshake: ServerHandshake?) {
        Log.d(TAG, "WebSocket Connected")
        isConnected = true
    }

    override fun onMessage(message: String?) {
        Log.d(TAG, "Received message: $message")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d(TAG, "WebSocket Closed: $code - $reason")
        isConnected = false
    }

    override fun onError(ex: Exception?) {
        Log.e(TAG, "WebSocket Error: ${ex?.message}")
        isConnected = false
    }

    fun connectAndListen(userId: Long, serverUrl: String): Flow<WebSocketMessage> = callbackFlow {
        try {
            val uri = URI("$serverUrl/ws?userId=$userId")
            webSocketClient = object : WebSocketClient(uri) {
                override fun onOpen(handshake: ServerHandshake?) {
                    Log.d(TAG, "WebSocket Connected for user: $userId")
                    isConnected = true
                }

                override fun onMessage(message: String?) {
                    Log.d(TAG, "Received message: $message")
                    message?.let {
                        try {
                            val webSocketMessage = gson.fromJson(it, WebSocketMessage::class.java)
                            trySend(webSocketMessage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing WebSocket message: ${e.message}")
                        }
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d(TAG, "WebSocket Closed: $code - $reason")
                    isConnected = false
                }

                override fun onError(ex: Exception?) {
                    Log.e(TAG, "WebSocket Error: ${ex?.message}")
                    isConnected = false
                }
            }

            webSocketClient?.connect()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect WebSocket: ${e.message}")
        }

        awaitClose {
            disconnect()
        }
    }

    fun disconnect() {
        webSocketClient?.close()
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected

    override fun send(message: String) {
        if (isConnected && webSocketClient != null) {
            webSocketClient?.send(message)
        } else {
            Log.w(TAG, "WebSocket not connected, cannot send message")
        }
    }
}