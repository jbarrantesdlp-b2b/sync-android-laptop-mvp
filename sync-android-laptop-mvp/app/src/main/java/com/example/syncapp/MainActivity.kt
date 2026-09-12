package com.example.syncapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.syncapp.ui.SettingsScreen
import com.example.syncapp.ui.theme.SyncTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyncTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
