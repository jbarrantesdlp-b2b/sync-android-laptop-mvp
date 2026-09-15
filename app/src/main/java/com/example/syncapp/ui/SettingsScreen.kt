package com.example.syncapp.ui

import androidx.compose.runtime.Composable
import com.example.syncapp.SyncViewModel

@Composable
fun SettingsScreen(
    viewModel: SyncViewModel? = null,
    initialTab: String? = null
) {
    SyncAppShell(viewModel = viewModel, initialTab = initialTab)
}
