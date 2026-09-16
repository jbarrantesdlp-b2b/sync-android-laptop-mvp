package com.example.sync

import android.content.Context
import com.example.core.datastore.PreferencesManager
import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncRepository(
    private val context: Context? = null,
    private val prefs: PreferencesManager? = null,
    private var serverUrl: String = DEFAULT_URL
) {
    constructor(serverUrl: String) : this(null, null, serverUrl)

    constructor(context: Context) : this(context, PreferencesManager(context), DEFAULT_URL)

    companion object {
        const val DEFAULT_URL = "ws://10.0.2.2:8123"
        private val LIVE_TYPES = setOf("IOT_TELEMETRY", "HARDWARE_TELEMETRY", "PING", "PONG")
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

    private val _latencyMs = MutableStateFlow<Long?>(null)
    val latencyMs: StateFlow<Long?> = _latencyMs.asStateFlow()

    private var lastSentTimestamp: Long = 0L

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

    fun autoDiscoverAndConnect(onConnected: ((String) -> Unit)? = null) {
        scope.launch {
            // 1. Probar primero la URL guardada si no es el placeholder del emulador
            val savedUrl = try {
                effectivePrefs?.serverUrlFlow?.first()
            } catch (_: Exception) {
                null
            } ?: serverUrl

            if (savedUrl.isNotBlank() && !savedUrl.contains("10.0.2.2")) {
                connectToServer(savedUrl)
                delay(1000)
                if (isConnected()) {
                    onConnected?.invoke(savedUrl)
                    return@launch
                }
            }

            // 2. Escanear automáticamente la subred Wi-Fi local en busca de Sync Engine (puerto 8123)
            val discoveredIp = scanSubnetForSyncEngine()
            if (discoveredIp != null) {
                val fullUrl = "ws://$discoveredIp:8123"
                effectivePrefs?.setServerUrl(fullUrl)
                connectToServer(fullUrl)
                onConnected?.invoke(fullUrl)
            }
        }
    }

    private fun getLocalWifiIp(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private suspend fun scanSubnetForSyncEngine(): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val myIp = getLocalWifiIp() ?: return@withContext null
        val prefix = myIp.substringBeforeLast(".")

        val probeClient = OkHttpClient.Builder()
            .connectTimeout(450, TimeUnit.MILLISECONDS)
            .readTimeout(450, TimeUnit.MILLISECONDS)
            .build()

        val foundIp = kotlinx.coroutines.CompletableDeferred<String?>()

        val jobs = (1..254).map { i ->
            launch {
                if (foundIp.isCompleted) return@launch
                val targetIp = "$prefix.$i"
                try {
                    val req = Request.Builder()
                        .url("http://$targetIp:8123/status")
                        .build()
                    probeClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val body = resp.body?.string() ?: ""
                            if (body.contains("Sync Engine") || body.contains("CONNECTED") || body.contains("hostName")) {
                                foundIp.complete(targetIp)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        kotlinx.coroutines.withTimeoutOrNull(2200) {
            foundIp.await()
        } ?: run {
            jobs.forEach { it.cancel() }
            null
        }
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    fun sendMessage(type: String, payload: String): Boolean {
        lastSentTimestamp = System.currentTimeMillis()
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
        var sent = socketClient.send(formattedJson)

        try {
            val httpUrl = serverUrl.replace("ws://", "http://").replace("wss://", "https://") + "/api/command"
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = formattedJson.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Non-blocking async failure handling
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { resp ->
                        if (resp.isSuccessful) {
                            if (!_connectionStatus.value) {
                                _connectionStatus.value = true
                                _connectionState.value = "CONNECTED"
                                scope.launch { effectivePrefs?.setConnectionStatus("CONNECTED") }
                            }
                            scope.launch { messageQueue?.markSent(message.id) }
                        }
                    }
                }
            })
        } catch (_: Exception) {
        }

        return sent || socketClient.isOnline()
    }

    /**
     * WebSocket-only fire-and-forget. Do not use for commands — it never
     * enqueues Room. Used by the IoT hub so 2.5s telemetry does not flood the DB.
     */
    fun sendLive(type: String, payload: String): Boolean {
        val id = UUID.randomUUID().toString()
        val formattedJson = "{\"id\":\"$id\",\"type\":\"$type\",\"payload\":$payload}"
        return socketClient.send(formattedJson)
    }

    fun sendIotTelemetry(payloadJson: String): Boolean = sendLive("IOT_TELEMETRY", payloadJson)

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

    fun shutdownPc(): Boolean {
        return sendMessage("SHUTDOWN", "{}")
    }

    fun rebootPc(): Boolean {
        return sendMessage("REBOOT", "{}")
    }

    fun abortShutdown(): Boolean {
        return sendMessage("ABORT_SHUTDOWN", "{}")
    }

    fun triggerManualSync() {
        startSync()
        sendMessage("sync_request", "{\"trigger\":\"manual\",\"timestamp\":${System.currentTimeMillis()}}")
    }

    private fun processIncomingData(data: String) {
        if (lastSentTimestamp > 0L) {
            val roundtrip = System.currentTimeMillis() - lastSentTimestamp
            if (roundtrip in 1..30000) {
                _latencyMs.value = roundtrip
            }
        }
        if (LIVE_TYPES.any { data.contains("\"$it\"") }) {
            return
        }
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
