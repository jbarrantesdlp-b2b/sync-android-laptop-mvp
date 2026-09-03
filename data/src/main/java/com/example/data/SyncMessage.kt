package com.example.data

import java.util.*

data class SyncMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val direction: SyncDirection = SyncDirection.OUTBOUND,
    val status: SyncStatus = SyncStatus.PENDING
)

enum class SyncDirection {
    OUTBOUND,
    INBOUND
}

enum class SyncStatus {
    PENDING,
    SENT,
    RECEIVED,
    FAILED,
    ACKNOWLEDGED
}
