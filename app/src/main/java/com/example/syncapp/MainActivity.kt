package com.example.syncapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.syncapp.ui.SettingsScreen
import com.example.syncapp.ui.theme.SyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SyncTheme {
                SettingsScreen()
            }
        }
    }
}
