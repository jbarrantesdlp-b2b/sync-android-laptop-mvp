package com.example.syncapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.datastore.PreferencesManager
import com.example.sync.ConnectivityObserver
import com.example.sync.NetworkState
import kotlinx.coroutines.flow.first

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = PreferencesManager(context)
    val connectivityObserver = remember { ConnectivityObserver(context) }
    
    var connectionStatus by remember { mutableStateOf("UNKNOWN") }
    var theme by remember { mutableStateOf("Minimal") }
    var networkState by remember { mutableStateOf<NetworkState>(NetworkState.Unknown) }
    var syncLog by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        connectivityObserver.startObserving()
        connectionStatus = prefs.connectionStatusFlow.first().toString()
        theme = prefs.themePackFlow.first().name
        syncLog = listOf(
            "[${System.currentTimeMillis()}] App initialized",
            "[${System.currentTimeMillis()}] Connectivity observer started",
            "[${System.currentTimeMillis()}] WorkManager scheduled (15 min interval)"
        )
    }

    LaunchedEffect(Unit) {
        connectivityObserver.networkState.collect { state ->
            networkState = state
            val networkType = when (state) {
                is NetworkState.Connected -> state.type
                NetworkState.Disconnected -> "Disconnected"
                NetworkState.Connecting -> "Connecting..."
                NetworkState.Unknown -> "Unknown"
            }
            syncLog = syncLog + "[${System.currentTimeMillis()}] Network state: $networkType"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF121212)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Sync Status",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Connection Status Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = when (connectionStatus) {
                        "CONNECTED" -> Color(0xFF4CAF50)
                        "CONNECTING" -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = connectionStatus,
                color = Color.White,
                fontSize = 18.sp
            )
        }

        // Network Type Display
        Text(
            text = "Network: " + when (networkState) {
                is NetworkState.Connected -> networkState.type
                NetworkState.Disconnected -> "No connection"
                NetworkState.Connecting -> "Connecting..."
                NetworkState.Unknown -> "Unknown"
            },
            color = when (networkState) {
                is NetworkState.Connected -> Color(0xFF4CAF50)
                NetworkState.Disconnected -> Color(0xFFF44336)
                NetworkState.Connecting -> Color(0xFFFFC107)
                NetworkState.Unknown -> Color(0xFFBB86FC)
            },
            fontSize = 14.sp
        )

        // Theme Display
        Text(
            text = "Current Theme: $theme",
            color = Color(0xFFBB86FC),
            fontSize = 14.sp
        )

        Button(
            onClick = { /* open theme picker */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Change Theme")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sync Log
        Text(
            text = "Sync Log (last 10)",
            color = Color.White,
            fontSize = 12.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1E1E1E), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                syncLog.takeLast(10).forEach { logEntry ->
                    Text(
                        text = logEntry,
                        color = Color(0xFF80DEEA),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Button(
            onClick = { syncLog = syncLog + "[${System.currentTimeMillis()}] Manual sync triggered" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trigger Sync Now")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            connectivityObserver.stopObserving()
        }
    }
}
