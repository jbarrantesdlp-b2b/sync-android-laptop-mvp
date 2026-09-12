package com.example.sync

import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

class WebSocketClientManager(
    private val serverUrl: String,
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnectionStatusChanged(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessageReceived(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleDisconnect()
                scheduleReconnect()
            }
        })
    }

    private fun handleDisconnect() {
        if (isConnected) {
            isConnected = false
            onConnectionStatusChanged(false)
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000)
            if (!isConnected) {
                connect()
            }
        }
    }

    fun sendMessage(text: String) {
        webSocket?.send(text)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        scope.cancel()
    }
}