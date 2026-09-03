package com.example.sync

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SyncNotificationHelper(private val context: Context) {
    fun notifySyncStarted() {
        // Log sync start
    }

    fun notifySyncCompleted(success: Boolean) {
        // Log sync completion
    }

    fun notifyConnectionStatus(status: String) {
        // Broadcast or log status change
    }
}
