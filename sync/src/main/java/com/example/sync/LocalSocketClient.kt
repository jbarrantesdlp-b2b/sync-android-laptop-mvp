package com.example.sync

import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

class LocalSocketClient(
    private val url: String,
    private val onMessage: suspend (String) -> Unit,
    private val onConnected: suspend () -> Unit = {},
    private val onDisconnected: suspend () -> Unit = {}
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var reconnectAttempts = 0L
    private val maxBackoffMs = 60_000L
    private var isConnected = false

    fun connect() {
        scope.launch {
            ensureConnect()
        }
    }

    private suspend fun ensureConnect() {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                scope.launch {
                    onConnected()
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch {
                    onMessage(text)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                scope.launch {
                    onDisconnected()
                    scheduleReconnect()
                }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                scope.launch {
                    onDisconnected()
                    if (code != 1000) scheduleReconnect()
                }
            }
        })
    }

    private suspend fun scheduleReconnect() {
        if (!scope.isActive) return
        reconnectAttempts++
        val backoff = (Math.pow(2.0, reconnectAttempts.toDouble()) * 1500).toLong().coerceIn(3000L, maxBackoffMs)
        delay(backoff)
        if (scope.isActive) {
            ensureConnect()
        }
    }

    fun send(text: String): Boolean {
        return if (isConnected && webSocket != null) {
            webSocket!!.send(text)
            true
        } else {
            false
        }
    }

    fun isOnline(): Boolean = isConnected

    fun close() {
        webSocket?.close(1000, "client close")
        scope.cancel()
    }
}
