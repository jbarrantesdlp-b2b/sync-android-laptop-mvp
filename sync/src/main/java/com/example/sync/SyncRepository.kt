package com.example.sync

import android.content.Context
import com.example.core.datastore.PreferencesManager
import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class SyncRepository(
    private val context: Context? = null,
    private val prefs: PreferencesManager? = null,
    private var serverUrl: String = DEFAULT_URL
) {
    constructor(serverUrl: String) : this(null, null, serverUrl)

    constructor(context: Context) : this(context, PreferencesManager(context), DEFAULT_URL)

    companion object {
        const val DEFAULT_URL = "ws://10.0.2.2:8123"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val effectivePrefs: PreferencesManager? = prefs ?: if (context != null) PreferencesManager(context) else null
    private val messageQueue: SyncMessageQueue? = context?.let {
        SyncMessageQueue(it, effectivePrefs ?: PreferencesManager(it))
    }

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _lastMessage = MutableStateFlow<String?>(null)
    val lastMessage: StateFlow<String?> = _lastMessage.asStateFlow()

    val messageFlow: kotlinx.coroutines.flow.SharedFlow<SyncMessage> =
        messageQueue?.messageFlow ?: kotlinx.coroutines.flow.MutableSharedFlow()

    suspend fun getMessageHistory(limit: Int = 100): List<SyncMessage> {
        return messageQueue?.getHistory(limit) ?: emptyList()
    }

    private var socketClient: LocalSocketClient = createSocketClient(serverUrl)

    private fun createSocketClient(url: String): LocalSocketClient {
        return LocalSocketClient(
            url = url,
            onMessage = { messageText ->
                _lastMessage.value = messageText
                processIncomingData(messageText)
            },
            onConnected = {
                if (!_connectionStatus.value) {
                    _connectionStatus.value = true
                    _connectionState.value = "CONNECTED"
                    scope.launch { effectivePrefs?.setConnectionStatus("CONNECTED") }
                }
            },
            onDisconnected = {
                if (_connectionStatus.value || _connectionState.value != "DISCONNECTED") {
                    _connectionStatus.value = false
                    _connectionState.value = "DISCONNECTED"
                    scope.launch { effectivePrefs?.setConnectionStatus("DISCONNECTED") }
                }
            }
        )
    }

    fun startSync() {
        socketClient.connect()
    }

    fun connectToServer(url: String = serverUrl) {
        if (this.serverUrl != url || !socketClient.isOnline()) {
            socketClient.close()
            this.serverUrl = url
            socketClient = createSocketClient(url)
        }
        socketClient.connect()
    }

    fun isConnected(): Boolean {
        return socketClient.isOnline()
    }

    fun sendMessage(type: String, payload: String): Boolean {
        val message = SyncMessage(
            id = UUID.randomUUID().toString(),
            type = type,
            payload = payload,
            timestamp = System.currentTimeMillis(),
            direction = SyncDirection.OUTBOUND,
            status = SyncStatus.PENDING
        )

        scope.launch {
            messageQueue?.enqueue(message)
        }

        val formattedJson = "{\"id\":\"${message.id}\",\"type\":\"$type\",\"payload\":$payload}"
        val sent = socketClient.send(formattedJson)

        if (sent) {
            scope.launch {
                messageQueue?.markSent(message.id)
            }
        }
        return sent
    }

    fun sendData(data: String): Boolean {
        return sendMessage("data", if (data.startsWith("{")) data else "\"$data\"")
    }

    fun sendPing(): Boolean {
        return sendMessage("PING", "\"PING\"")
    }

    fun lockScreen(): Boolean {
        return sendMessage("LOCK_SCREEN", "{}")
    }

    fun presentationNext(): Boolean {
        return sendMessage("PRESENTATION_NEXT", "{}")
    }

    fun presentationPrev(): Boolean {
        return sendMessage("PRESENTATION_PREV", "{}")
    }

    fun syncClipboard(text: String): Boolean {
        val cleanText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        return sendMessage("SYNC_CLIPBOARD", "{\"text\":\"$cleanText\"}")
    }

    fun openUrlOnPc(url: String): Boolean {
        val cleanUrl = url.replace("\\", "\\\\").replace("\"", "\\\"")
        return sendMessage("OPEN_URL", "{\"url\":\"$cleanUrl\"}")
    }

    fun adjustVolume(action: String): Boolean {
        return sendMessage(action, "{}")
    }

    fun triggerManualSync() {
        startSync()
        sendMessage("sync_request", "{\"trigger\":\"manual\",\"timestamp\":${System.currentTimeMillis()}}")
    }

    private fun processIncomingData(data: String) {
        scope.launch {
            val message = SyncMessage(
                id = UUID.randomUUID().toString(),
                type = "incoming",
                payload = data,
                timestamp = System.currentTimeMillis(),
                direction = SyncDirection.INBOUND,
                status = SyncStatus.RECEIVED
            )
            messageQueue?.addIncoming(message)
        }
    }

    fun stopSync() {
        socketClient.close()
    }

    fun disconnect() {
        stopSync()
    }
}
