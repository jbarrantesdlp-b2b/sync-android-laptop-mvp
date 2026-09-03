package com.example.sync

import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class BidirectionalSyncTest {

    @Test
    fun testOutboundMessageFlow() = runTest {
        // Arrange
        val msg = SyncMessage(
            type = "sync_request",
            payload = "{\"data\": \"test\"}",
            direction = SyncDirection.OUTBOUND,
            status = SyncStatus.PENDING
        )

        // Assert
        assertEquals("sync_request", msg.type)
        assertEquals(SyncDirection.OUTBOUND, msg.direction)
        assertEquals(SyncStatus.PENDING, msg.status)
    }

    @Test
    fun testInboundMessageFlow() = runTest {
        // Arrange
        val msg = SyncMessage(
            type = "sync_response",
            payload = "{\"data\": \"response\"}",
            direction = SyncDirection.INBOUND,
            status = SyncStatus.RECEIVED
        )

        // Assert
        assertEquals("sync_response", msg.type)
        assertEquals(SyncDirection.INBOUND, msg.direction)
        assertEquals(SyncStatus.RECEIVED, msg.status)
    }

    @Test
    fun testMessageStateTransitions() = runTest {
        // Arrange
        val msg = SyncMessage(
            type = "request",
            payload = "{}",
            status = SyncStatus.PENDING
        )

        // Act & Assert
        assertEquals(SyncStatus.PENDING, msg.status)

        val sent = msg.copy(status = SyncStatus.SENT)
        assertEquals(SyncStatus.SENT, sent.status)

        val acked = sent.copy(status = SyncStatus.ACKNOWLEDGED)
        assertEquals(SyncStatus.ACKNOWLEDGED, acked.status)
    }

    @Test
    fun testBidirectionalMessageTypes() = runTest {
        val outbound = SyncMessage(
            type = "sync_request",
            payload = "{}",
            direction = SyncDirection.OUTBOUND
        )

        val inbound = SyncMessage(
            type = "sync_response",
            payload = "{}",
            direction = SyncDirection.INBOUND
        )

        val ack = SyncMessage(
            type = "ack",
            payload = "{}",
            direction = SyncDirection.OUTBOUND
        )

        assertEquals(SyncDirection.OUTBOUND, outbound.direction)
        assertEquals(SyncDirection.INBOUND, inbound.direction)
        assertEquals(SyncDirection.OUTBOUND, ack.direction)
    }

    @Test
    fun testMessageIdUniqueness() = runTest {
        val msg1 = SyncMessage(type = "test", payload = "{}")
        val msg2 = SyncMessage(type = "test", payload = "{}")

        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun testMessageTimestamps() = runTest {
        val msg1 = SyncMessage(type = "test", payload = "{}")
        Thread.sleep(10)
        val msg2 = SyncMessage(type = "test", payload = "{}")

        assertTrue(msg2.timestamp >= msg1.timestamp)
    }

    @Test
    fun testMessagePayloadPreservation() = runTest {
        val payload = "{\"key\": \"value\", \"number\": 42}"
        val msg = SyncMessage(
            type = "data",
            payload = payload
        )

        assertEquals(payload, msg.payload)
    }
}
