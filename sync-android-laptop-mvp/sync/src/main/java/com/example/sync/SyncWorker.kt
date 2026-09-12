package com.example.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.core.datastore.PreferencesManager
import kotlinx.coroutines.delay

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val prefs = PreferencesManager(appContext)
    private val repository = SyncRepository(appContext, prefs)

    override suspend fun doWork(): Result {
        return try {
            prefs.setConnectionStatus("CONNECTING")
            
            repository.connectToServer(SyncRepository.DEFAULT_URL)
            
            delay(2000)
            
            repository.sendMessage(
                type = "sync_request",
                payload = "{\"timestamp\":${System.currentTimeMillis()}}"
            )
            
            delay(3000)
            
            prefs.setConnectionStatus("CONNECTED")
            repository.disconnect()
            Result.success()
        } catch (e: Exception) {
            prefs.setConnectionStatus("DISCONNECTED")
            Result.retry()
        }
    }
}
