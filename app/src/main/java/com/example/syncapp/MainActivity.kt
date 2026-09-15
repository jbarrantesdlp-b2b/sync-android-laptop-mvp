package com.example.syncapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.syncapp.ui.SettingsScreen
import com.example.syncapp.ui.theme.SyncTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SyncViewModel by viewModels()
    private var openTab by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openTab = intent.getStringExtra("open_tab") ?: intent.getStringExtra("open_sheet")
        setContent {
            SyncTheme {
                SettingsScreen(viewModel = viewModel, initialTab = openTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTab = intent.getStringExtra("open_tab") ?: intent.getStringExtra("open_sheet")
        recreate()
    }
}
