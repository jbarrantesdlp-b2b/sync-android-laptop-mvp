package com.example.syncapp

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SyncViewModel(context: Context) : ViewModel() {
    private val prefs = PreferencesManager(context)

    val connectionStatusFlow = prefs.connectionStatusFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "UNKNOWN")

    val themePackFlow = prefs.themePackFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, com.example.core.model.ThemePack.Minimal)

    fun triggerManualSync() {
        // Enqueue immediate sync work
    }
}
