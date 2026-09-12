package com.example.sync

import android.content.Context
import com.example.core.datastore.PreferencesManager
import com.example.data.SyncDirection
import com.example.data.SyncMessage
import com.example.data.SyncStatus
import com.example.data.db.SyncDatabase
import com.example.data.db.SyncMessageEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SyncMessageQueue(
    private val context: Context,
    private val prefs: PreferencesManager
) {
    private val db = SyncDatabase.getDatabase(context)
    private val dao = db.syncMessageDao()

    private val _messageFlow = MutableSharedFlow<SyncMessage>(replay = 50)
    val messageFlow: SharedFlow<SyncMessage> = _messageFlow.asSharedFlow()

    suspend fun enqueue(message: SyncMessage) {
        val msg = message.copy(status = SyncStatus.PENDING)
        val entity = toEntity(msg)
        dao.insert(entity)
        _messageFlow.emit(msg)
    }

    suspend fun markSent(messageId: String) {
        val entity = dao.getById(messageId)
        if (entity != null) {
            val updated = entity.copy(
                status = SyncStatus.SENT.name,
                updatedAt = System.currentTimeMillis()
            )
            dao.update(updated)
            _messageFlow.emit(toMessage(updated))
        }
    }

    suspend fun markAcknowledged(messageId: String) {
        val entity = dao.getById(messageId)
        if (entity != null) {
            val updated = entity.copy(
                status = SyncStatus.ACKNOWLEDGED.name,
                updatedAt = System.currentTimeMillis()
            )
            dao.update(updated)
            _messageFlow.emit(toMessage(updated))
        }
    }

    suspend fun addIncoming(message: SyncMessage) {
        val incoming = message.copy(
            direction = SyncDirection.INBOUND,
            status = SyncStatus.RECEIVED
        )
        val entity = toEntity(incoming)
        dao.insert(entity)
        _messageFlow.emit(incoming)
    }

    suspend fun getPending(): List<SyncMessage> {
        return dao.getByStatus(SyncStatus.PENDING.name)
            .map { toMessage(it) }
    }

    suspend fun getAll(): List<SyncMessage> {
        return dao.getLatest(500)
            .map { toMessage(it) }
    }

    suspend fun getHistory(limit: Int = 100): List<SyncMessage> {
        return dao.getLatest(limit)
            .map { toMessage(it) }
    }

    suspend fun clear() {
        dao.deleteAll()
    }

    suspend fun cleanupOlderThan(days: Int = 7) {
        val timestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        dao.deleteOlderThan(timestamp)
    }

    private fun toEntity(message: SyncMessage): SyncMessageEntity {
        return SyncMessageEntity(
            id = message.id,
            type = message.type,
            payload = message.payload,
            timestamp = message.timestamp,
            direction = message.direction.name,
            status = message.status.name
        )
    }

    private fun toMessage(entity: SyncMessageEntity): SyncMessage {
        return SyncMessage(
            id = entity.id,
            type = entity.type,
            payload = entity.payload,
            timestamp = entity.timestamp,
            direction = SyncDirection.valueOf(entity.direction),
            status = SyncStatus.valueOf(entity.status)
        )
    }
}
