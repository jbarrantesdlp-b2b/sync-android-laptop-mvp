package com.example.syncwidget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.datastore.PreferencesManager
import com.example.sync.SyncRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class GlanceWidgetViewModel(
    context: Context,
    private val prefs: PreferencesManager,
    private val repository: SyncRepository
) : ViewModel() {

    val connectionStatusFlow = prefs.connectionStatusFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "UNKNOWN")

    val themePackFlow = prefs.themePackFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, com.example.core.model.ThemePack.Minimal)

    val messageFlow = repository.messageFlow

    suspend fun getMessageCount(): Int {
        val history = repository.getMessageHistory(100)
        return history.size
    }

    suspend fun getLastMessageTime(): String {
        val history = repository.getMessageHistory(1)
        return if (history.isNotEmpty()) {
            val timestamp = history[0].timestamp
            val diff = System.currentTimeMillis() - timestamp
            when {
                diff < 60_000 -> "now"
                diff < 3_600_000 -> "${diff / 60_000}m ago"
                diff < 86_400_000 -> "${diff / 3_600_000}h ago"
                else -> "${diff / 86_400_000}d ago"
            }
        } else {
            "never"
        }
    }

    suspend fun getPendingCount(): Int {
        // Would require accessing repository's queue
        return 0
    }
}

class GlanceWidgetViewModelFactory(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val repository: SyncRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GlanceWidgetViewModel(context, prefs, repository) as T
    }
}
