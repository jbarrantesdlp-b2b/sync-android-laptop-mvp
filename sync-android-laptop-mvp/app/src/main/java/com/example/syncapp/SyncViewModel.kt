package com.example.syncapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ThemePack
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncViewModel : ViewModel() {
    private val syncApp = SyncApp.instance
    private val prefs = syncApp.prefs
    private val repository = syncApp.repository

    val connectionStatusFlow: StateFlow<String> = prefs.connectionStatusFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "DISCONNECTED")

    val themePackFlow: StateFlow<ThemePack> = prefs.themePackFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePack.SyncEngine)

    val lastMessageFlow: StateFlow<String?> = repository.lastMessage

    val latencyMsFlow: StateFlow<Long?> = repository.latencyMs

    fun triggerManualSync() {
        repository.triggerManualSync()
    }

    fun sendPing(): Boolean {
        return repository.sendPing()
    }

    fun lockScreen(): Boolean {
        return repository.lockScreen()
    }

    fun syncClipboard(text: String): Boolean {
        return repository.syncClipboard(text)
    }

    fun openUrlOnPc(url: String): Boolean {
        return repository.openUrlOnPc(url)
    }

    fun adjustVolume(action: String): Boolean {
        return repository.adjustVolume(action)
    }

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            prefs.setServerUrl(url)
            repository.connectToServer(url)
        }
    }

    fun setTheme(theme: ThemePack) {
        viewModelScope.launch {
            prefs.setTheme(theme)
        }
    }
}
