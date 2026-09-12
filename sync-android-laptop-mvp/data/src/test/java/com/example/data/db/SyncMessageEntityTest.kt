package com.example.data.db

import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

class SyncMessageEntityTest {

    @Test
    fun testSyncMessageEntityCreation() {
        val entity = SyncMessageEntity(
            id = "test-id",
            type = "sync_request",
            payload = "{\"data\": \"test\"}",
            timestamp = 1234567890L,
            direction = "OUTBOUND",
            status = "PENDING"
        )

        assertEquals("test-id", entity.id)
        assertEquals("sync_request", entity.type)
        assertEquals("OUTBOUND", entity.direction)
        assertEquals("PENDING", entity.status)
    }

    @Test
    fun testSyncMessageEntityWithDefaults() {
        val entity = SyncMessageEntity(
            id = "id-1",
            type = "ack",
            payload = "{}",
            timestamp = System.currentTimeMillis(),
            direction = "INBOUND",
            status = "RECEIVED"
        )

        assertNotNull(entity.createdAt)
        assertNotNull(entity.updatedAt)
        assertTrue(entity.createdAt > 0)
    }

    @Test
    fun testSyncMessageEntityStatusValues() {
        val statuses = listOf("PENDING", "SENT", "RECEIVED", "FAILED", "ACKNOWLEDGED")
        
        statuses.forEach { status ->
            val entity = SyncMessageEntity(
                id = "id-${status}",
                type = "test",
                payload = "{}",
                timestamp = System.currentTimeMillis(),
                direction = "OUTBOUND",
                status = status
            )
            assertEquals(status, entity.status)
        }
    }

    @Test
    fun testSyncMessageEntityDirections() {
        val outbound = SyncMessageEntity(
            id = "out",
            type = "test",
            payload = "{}",
            timestamp = System.currentTimeMillis(),
            direction = "OUTBOUND",
            status = "SENT"
        )

        val inbound = SyncMessageEntity(
            id = "in",
            type = "test",
            payload = "{}",
            timestamp = System.currentTimeMillis(),
            direction = "INBOUND",
            status = "RECEIVED"
        )

        assertEquals("OUTBOUND", outbound.direction)
        assertEquals("INBOUND", inbound.direction)
    }
}
