package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.SyncDirection
import com.example.data.SyncStatus

@Entity(tableName = "sync_messages")
data class SyncMessageEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val payload: String,
    val timestamp: Long,
    val direction: String, // OUTBOUND | INBOUND
    val status: String,    // PENDING | SENT | RECEIVED | FAILED | ACKNOWLEDGED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
