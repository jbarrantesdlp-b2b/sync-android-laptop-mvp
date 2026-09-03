package com.example.sync

import android.content.Context
import com.example.core.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SyncTrigger(
    private val context: Context,
    private val repository: SyncRepository,
    private val prefs: PreferencesManager
) {
    private val connectivityObserver = ConnectivityObserver(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    fun start() {
        connectivityObserver.startObserving()
        
        scope.launch {
            connectivityObserver.networkState.collect { state ->
                when (state) {
                    is NetworkState.Connected -> {
                        // Network recovered: trigger sync
                        if (!repository.isConnected()) {
                            triggerManualSync()
                        }
                    }
                    NetworkState.Disconnected -> {
                        prefs.setConnectionStatus("DISCONNECTED")
                    }
                    NetworkState.Connecting -> {
                        prefs.setConnectionStatus("CONNECTING")
                    }
                    NetworkState.Unknown -> {
                        // No action
                    }
                }
            }
        }
    }

    fun stop() {
        connectivityObserver.stopObserving()
    }

    private suspend fun triggerManualSync() {
        repository.connectToServer("ws://localhost:8123")
        repository.sendMessage(
            type = "sync_request",
            payload = "{\"timestamp\":${System.currentTimeMillis()},\"trigger\":\"connectivity_change\"}"
        )
    }
}
