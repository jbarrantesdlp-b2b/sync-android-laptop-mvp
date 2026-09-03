package com.example.sync

import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import org.junit.Test
import org.junit.Assert.*

class SyncMessageTest {

    @Test
    fun testSyncMessageCreation() {
        val message = SyncMessage(
            type = "sync_request",
            payload = "{\"data\": \"test\"}",
            direction = SyncDirection.OUTBOUND,
            status = SyncStatus.PENDING
        )

        assertEquals("sync_request", message.type)
        assertEquals(SyncDirection.OUTBOUND, message.direction)
        assertEquals(SyncStatus.PENDING, message.status)
        assertNotNull(message.id)
        assertTrue(message.timestamp > 0)
    }

    @Test
    fun testSyncMessageWithCustomId() {
        val customId = "custom-msg-id"
        val message = SyncMessage(
            id = customId,
            type = "test",
            payload = "{}",
            timestamp = 1234567890L,
            direction = SyncDirection.INBOUND,
            status = SyncStatus.RECEIVED
        )

        assertEquals(customId, message.id)
        assertEquals(1234567890L, message.timestamp)
    }

    @Test
    fun testSyncMessageDefaults() {
        val message = SyncMessage(
            type = "ping",
            payload = "{}"
        )

        assertEquals(SyncDirection.OUTBOUND, message.direction)
        assertEquals(SyncStatus.PENDING, message.status)
        assertEquals("ping", message.type)
    }

    @Test
    fun testSyncMessageCopy() {
        val original = SyncMessage(
            type = "request",
            payload = "{}",
            status = SyncStatus.PENDING
        )

        val updated = original.copy(status = SyncStatus.SENT)

        assertEquals(original.id, updated.id)
        assertEquals(original.type, updated.type)
        assertEquals(SyncStatus.PENDING, original.status)
        assertEquals(SyncStatus.SENT, updated.status)
    }

    @Test
    fun testSyncStatusEnum() {
        val statuses = listOf(
            SyncStatus.PENDING,
            SyncStatus.SENT,
            SyncStatus.RECEIVED,
            SyncStatus.FAILED,
            SyncStatus.ACKNOWLEDGED
        )

        assertEquals(5, statuses.size)
        assertTrue(statuses.contains(SyncStatus.SENT))
    }

    @Test
    fun testSyncDirectionEnum() {
        assertEquals(SyncDirection.OUTBOUND, SyncDirection.OUTBOUND)
        assertEquals(SyncDirection.INBOUND, SyncDirection.INBOUND)
        assertNotEquals(SyncDirection.OUTBOUND, SyncDirection.INBOUND)
    }
}
