package com.example.sync

import android.content.Context
import com.example.core.datastore.PreferencesManager
import com.example.data.SyncMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class SyncRepository(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val messageQueue = SyncMessageQueue(context, prefs)
    private var client: LocalSocketClient? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    val messageFlow: SharedFlow<SyncMessage> = messageQueue.messageFlow

    fun connectToServer(serverUrl: String) {
        client = LocalSocketClient(
            url = serverUrl,
            onMessage = { message ->
                scope.launch {
                    handleIncomingMessage(message)
                }
            },
            onConnected = {
                prefs.setConnectionStatus("CONNECTED")
                processPendingMessages()
            },
            onDisconnected = {
                prefs.setConnectionStatus("DISCONNECTED")
            }
        )
        client?.connect()
    }

    suspend fun sendMessage(type: String, payload: String) {
        val message = SyncMessage(type = type, payload = payload)
        messageQueue.enqueue(message)
        
        val json = JSONObject().apply {
            put("id", message.id)
            put("type", type)
            put("payload", payload)
            put("timestamp", message.timestamp)
        }

        if (client?.send(json.toString()) == true) {
            messageQueue.markSent(message.id)
        }
    }

    private suspend fun handleIncomingMessage(rawMessage: String) {
        try {
            val json = JSONObject(rawMessage)
            val message = SyncMessage(
                id = json.optString("id", ""),
                type = json.optString("type", "unknown"),
                payload = json.optString("payload", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
            
            messageQueue.addIncoming(message)
            
            // Auto-acknowledge receipt
            if (message.type != "ack") {
                sendAcknowledgment(message.id)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    private suspend fun sendAcknowledgment(messageId: String) {
        val ackJson = JSONObject().apply {
            put("type", "ack")
            put("ackId", messageId)
            put("timestamp", System.currentTimeMillis())
        }
        client?.send(ackJson.toString())
    }

    private fun processPendingMessages() {
        scope.launch {
            messageQueue.getPending().forEach { msg ->
                val json = JSONObject().apply {
                    put("id", msg.id)
                    put("type", msg.type)
                    put("payload", msg.payload)
                    put("timestamp", msg.timestamp)
                }

                if (client?.send(json.toString()) == true) {
                    messageQueue.markSent(msg.id)
                }
            }
        }
    }

    fun getMessageHistory(limit: Int = 100): List<SyncMessage> = messageQueue.getHistory(limit)

    suspend fun cleanupOldMessages(days: Int = 7) {
        messageQueue.cleanupOlderThan(days)
    }

    fun isConnected(): Boolean = client?.isOnline() ?: false

    fun disconnect() {
        client?.close()
    }
}
