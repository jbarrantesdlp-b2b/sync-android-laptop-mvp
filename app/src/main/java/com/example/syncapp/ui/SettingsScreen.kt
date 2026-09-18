package com.example.syncapp.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.data.SyncMessage
import com.example.sync.ai.GeminiClient
import com.example.syncapp.SyncApp
import com.example.syncapp.SyncViewModel
import com.example.syncapp.iot.IotSensorHub
import com.example.syncapp.ui.components.FeedbackToast
import com.example.syncapp.ui.components.SyncEngineTopBar
import com.example.syncapp.ui.panes.ActivityPane
import com.example.syncapp.ui.panes.AiPane
import com.example.syncapp.ui.panes.DevicesPane
import com.example.syncapp.ui.panes.HomePane
import com.example.syncapp.ui.panes.RemoteDrivePane
import com.example.syncapp.ui.panes.SettingsBottomSheet
import com.example.syncapp.ui.theme.SyncTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 5 Canonical Destinations matching master design media_1789632468509.jpg:
 * 1. Inicio (Pantalla de inicio animada)
 * 2. Dispositivos (Control de dispositivo / Control remoto)
 * 3. Actividad (Portapapeles / Flujo de clips)
 * 4. Archivos (Explorador del Disco Duro de Laptop / Remote Drive)
 * 5. IA (IA Assist con Gemini AI)
 */
enum class AppTab {
    Home,
    Devices,
    Activity,
    Files,
    Ai
}

@Composable
fun SettingsScreen(
    viewModel: SyncViewModel? = null,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val syncApp = remember { SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE")
    val keptViewModel = viewModel

    // IoT Sensor Hub
    val hub = remember { IotSensorHub(context) }
    DisposableEffect(Unit) {
        hub.start()
        onDispose { hub.stop() }
    }

    // Reactive states from repository & prefs
    var connectionStatus by remember { mutableStateOf("UNKNOWN") }
    var currentTheme by remember { mutableStateOf(ThemePack.NeoObsidian) }
    var technicalLogs by remember { mutableStateOf(listOf<String>()) }
    var serverUrl by remember { mutableStateOf("ws://192.168.1.49:8123") }
    var geminiApiKey by remember { mutableStateOf("") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var remoteIotNode by remember { mutableStateOf<String?>(null) }
    var recentEvents by remember { mutableStateOf(listOf<SyncMessage>()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Secondary UI states
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }

    // Navigation tab - 5 Canonical tabs
    var currentTab by remember {
        mutableStateOf(
            when (initialTab?.lowercase()) {
                "devices", "control", "mando" -> AppTab.Devices
                "activity", "clipboard", "portapapeles" -> AppTab.Activity
                "files", "drive", "disco" -> AppTab.Files
                "ai", "ia", "gemini" -> AppTab.Ai
                else -> AppTab.Home
            }
        )
    }

    val geminiClient = remember(geminiApiKey) {
        GeminiClient { geminiApiKey }
    }

    fun showFeedback(msg: String) {
        toastMessage = msg
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun logTechnical(msg: String) {
        technicalLogs = (technicalLogs + msg).takeLast(60)
    }

    // Collect flows
    LaunchedEffect(Unit) {
        prefs.serverUrlFlow.collect { url ->
            serverUrl = url
            repository.connectToServer(url)
        }
    }

    LaunchedEffect(Unit) {
        prefs.connectionStatusFlow.collect { status ->
            connectionStatus = status
            logTechnical("[STATUS] $status")
        }
    }

    LaunchedEffect(Unit) {
        prefs.themePackFlow.collect { theme ->
            currentTheme = theme
        }
    }

    LaunchedEffect(Unit) {
        prefs.geminiApiKeyFlow.collect { key ->
            geminiApiKey = key
        }
    }

    LaunchedEffect(Unit) {
        repository.latencyMs.collect { ms ->
            latencyMs = ms
        }
    }

    LaunchedEffect(Unit) {
        repository.lastMessage.collect { msg ->
            if (!msg.isNullOrBlank()) {
                logTechnical("[WS] ${msg.take(80)}")
                try {
                    val obj = JSONObject(msg)
                    if (obj.optString("type") == "IOT_TELEMETRY") {
                        val payload = obj.optJSONObject("payload") ?: obj
                        val source = payload.optString("source")
                        if (source != "phone") {
                            remoteIotNode = source + " · " + payload.optString("device", "nodo")
                        }
                    }
                } catch (_: Exception) {}
                recentEvents = repository.getMessageHistory(40)
            }
        }
    }

    LaunchedEffect(Unit) {
        recentEvents = repository.getMessageHistory(40)
    }

    val bgOled = Color(0xFF050811)
    val barSurface = Color(0xFF090F1D)
    val textCyan = Color(0xFF00BFFF)
    val textMuted = Color(0xFF64748B)

    SyncTheme(currentTheme) {
        Scaffold(
            containerColor = bgOled,
            bottomBar = {
                // 5 Canonical Navigation Items (Exact match to all 5 screens in master design)
                NavigationBar(
                    containerColor = barSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.Home,
                        onClick = { currentTab = AppTab.Home },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = "Inicio", modifier = Modifier.size(22.dp)) },
                        label = { Text("Inicio", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textCyan,
                            selectedTextColor = textCyan,
                            unselectedIconColor = textMuted,
                            unselectedTextColor = textMuted,
                            indicatorColor = textCyan.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.Devices,
                        onClick = { currentTab = AppTab.Devices },
                        icon = { Icon(Icons.Outlined.Devices, contentDescription = "Dispositivos", modifier = Modifier.size(22.dp)) },
                        label = { Text("Dispositivos", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textCyan,
                            selectedTextColor = textCyan,
                            unselectedIconColor = textMuted,
                            unselectedTextColor = textMuted,
                            indicatorColor = textCyan.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.Activity,
                        onClick = { currentTab = AppTab.Activity },
                        icon = { Icon(Icons.Outlined.ContentPaste, contentDescription = "Actividad", modifier = Modifier.size(22.dp)) },
                        label = { Text("Actividad", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textCyan,
                            selectedTextColor = textCyan,
                            unselectedIconColor = textMuted,
                            unselectedTextColor = textMuted,
                            indicatorColor = textCyan.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.Files,
                        onClick = { currentTab = AppTab.Files },
                        icon = { Icon(Icons.Outlined.Folder, contentDescription = "Archivos", modifier = Modifier.size(22.dp)) },
                        label = { Text("Archivos", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textCyan,
                            selectedTextColor = textCyan,
                            unselectedIconColor = textMuted,
                            unselectedTextColor = textMuted,
                            indicatorColor = textCyan.copy(alpha = 0.12f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.Ai,
                        onClick = { currentTab = AppTab.Ai },
                        icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "IA", modifier = Modifier.size(22.dp)) },
                        label = { Text("IA", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = textCyan,
                            selectedTextColor = textCyan,
                            unselectedIconColor = textMuted,
                            unselectedTextColor = textMuted,
                            indicatorColor = textCyan.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentTab) {
                    AppTab.Home -> {
                        HomePane(
                            status = connectionStatus,
                            latencyMs = latencyMs,
                            serverUrl = serverUrl,
                            repository = repository,
                            currentTheme = currentTheme,
                            onOpenDevices = { currentTab = AppTab.Devices },
                            onOpenClipboard = { currentTab = AppTab.Activity },
                            onOpenDrive = { currentTab = AppTab.Files },
                            onOpenAi = { currentTab = AppTab.Ai },
                            onOpenSettings = { showSettingsSheet = true },
                            onOpenQrScanner = { showQrScanner = true },
                            onDirectConnect = { targetUrl ->
                                scope.launch {
                                    prefs.setServerUrl(targetUrl)
                                    repository.connectToServer(targetUrl)
                                    showFeedback("Enlazando con $targetUrl...")
                                }
                            },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    AppTab.Devices -> {
                        DevicesPane(
                            status = connectionStatus,
                            repository = repository,
                            onBack = { currentTab = AppTab.Home },
                            onNavigateTab = { tabKey ->
                                when (tabKey) {
                                    "files" -> currentTab = AppTab.Files
                                    "clipboard" -> currentTab = AppTab.Activity
                                }
                            },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    AppTab.Activity -> {
                        ActivityPane(
                            events = recentEvents,
                            repository = repository,
                            onSendToPc = { txt ->
                                scope.launch {
                                    repository.syncClipboard(txt)
                                    showFeedback("Texto enviado a laptop")
                                }
                            },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    AppTab.Files -> {
                        RemoteDrivePane(
                            repository = repository,
                            isConnected = connectionStatus == "CONNECTED",
                            modifier = Modifier.statusBarsPadding()
                        )
                    }

                    AppTab.Ai -> {
                        AiPane(
                            geminiClient = geminiClient,
                            onCopyResult = { txt ->
                                showFeedback("Copiado")
                            },
                            onSendToPc = { txt ->
                                scope.launch {
                                    repository.syncClipboard(txt)
                                    showFeedback("Resultado enviado a la Laptop")
                                }
                            },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                }

                // Temporary Feedback Banner
                FeedbackToast(
                    message = toastMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )

                // Secondary Settings Sheet
                if (showSettingsSheet) {
                    SettingsBottomSheet(
                        currentServerUrl = serverUrl,
                        currentGeminiKey = geminiApiKey,
                        currentTheme = currentTheme,
                        technicalLogs = technicalLogs,
                        onDismiss = { showSettingsSheet = false },
                        onSaveServerUrl = { newUrl ->
                            scope.launch {
                                prefs.setServerUrl(newUrl)
                                repository.connectToServer(newUrl)
                                showFeedback("Conectando a $newUrl...")
                            }
                            showSettingsSheet = false
                        },
                        onSaveGeminiKey = { key ->
                            scope.launch {
                                prefs.setGeminiApiKey(key)
                                showFeedback("Clave Gemini guardada")
                            }
                        },
                        onSelectTheme = { theme ->
                            scope.launch {
                                prefs.setTheme(theme)
                            }
                        },
                        onOpenQrScanner = {
                            showQrScanner = true
                        }
                    )
                }

                // QR Scanner Dialog
                if (showQrScanner) {
                    QrScannerDialog(
                        onDismiss = { showQrScanner = false },
                        onQrScanned = { scannedUrl ->
                            showQrScanner = false
                            showSettingsSheet = false
                            scope.launch {
                                prefs.setServerUrl(scannedUrl)
                                repository.connectToServer(scannedUrl)
                                showFeedback("Vinculado con éxito")
                            }
                        }
                    )
                }
            }
        }
    }
}
