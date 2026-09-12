package com.example.sync

import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import org.junit.Test
import org.junit.Assert.*

class LocalSocketClientTest {

    @Test
    fun testClientInitialization() {
        var messageReceived = ""
        var connected = false
        var disconnected = false

        val client = LocalSocketClient(
            url = "ws://localhost:8123",
            onMessage = { msg -> messageReceived = msg },
            onConnected = { connected = true },
            onDisconnected = { disconnected = true }
        )

        assertNotNull(client)
        assertFalse(client.isOnline())
    }

    @Test
    fun testClientIsOfflineByDefault() {
        val client = LocalSocketClient(
            url = "ws://localhost:8123",
            onMessage = {}
        )

        assertFalse(client.isOnline())
    }

    @Test
    fun testSendReturnsFalseWhenNotConnected() {
        val client = LocalSocketClient(
            url = "ws://localhost:8123",
            onMessage = {}
        )

        val result = client.send("test message")
        assertFalse(result)
    }

    @Test
    fun testClientCreationWithDifferentUrls() {
        val urls = listOf(
            "ws://localhost:8123",
            "ws://192.168.1.100:8123",
            "wss://example.com/sync"
        )

        urls.forEach { url ->
            val client = LocalSocketClient(url, {})
            assertNotNull(client)
        }
    }

    @Test
    fun testCallbacksNotNull() {
        val client = LocalSocketClient(
            url = "ws://localhost:8123",
            onMessage = { },
            onConnected = { },
            onDisconnected = { }
        )

        assertNotNull(client)
    }
}
