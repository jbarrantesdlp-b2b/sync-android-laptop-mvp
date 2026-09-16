package com.example.syncapp.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.core.model.ThemePack
import com.example.data.SyncMessage
import com.example.sync.ai.GeminiClient
import com.example.syncapp.SyncApp
import com.example.syncapp.SyncViewModel
import com.example.syncapp.iot.IotSensorHub
import com.example.syncapp.ui.components.FeedbackToast
import com.example.syncapp.ui.components.SyncEngineTopBar
import com.example.syncapp.ui.components.SyncNavItem
import com.example.syncapp.ui.panes.ActivityPane
import com.example.syncapp.ui.panes.AiPane
import com.example.syncapp.ui.panes.BridgePane
import com.example.syncapp.ui.panes.DevicesPane
import com.example.syncapp.ui.panes.HomePane
import com.example.syncapp.ui.panes.SettingsBottomSheet
import com.example.syncapp.ui.theme.DesignTokens
import com.example.syncapp.ui.theme.SyncTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Definitively exactly 5 main areas:
 * 1. Inicio (Home)
 * 2. Dispositivos (Devices)
 * 3. Actividad (Activity)
 * 4. Bridge (Bridge)
 * 5. IA (IA Assist)
 *
 * Ajustes is secondary (accessed via top bar).
 */
enum class AppTab {
    Home,
    Devices,
    Activity,
    Bridge,
    Ai
}

@Composable
fun SettingsScreen(
    viewModel: SyncViewModel? = null,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val syncApp = remember { SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE")
    val keptViewModel = viewModel

    // IoT Sensor Hub
    val hub = remember { IotSensorHub(context) }
    val iotSnapshot by hub.snapshot.collectAsState()
    DisposableEffect(Unit) {
        hub.start()
        onDispose { hub.stop() }
    }

    // Reactive states from repository & prefs
    var connectionStatus by remember { mutableStateOf("UNKNOWN") }
    var currentTheme by remember { mutableStateOf(ThemePack.SyncEngine) }
    var technicalLogs by remember { mutableStateOf(listOf<String>()) }
    var serverUrl by remember { mutableStateOf("ws://10.0.2.2:8123") }
    var geminiApiKey by remember { mutableStateOf("") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var remoteIotNode by remember { mutableStateOf<String?>(null) }
    var recentEvents by remember { mutableStateOf(listOf<SyncMessage>()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Controlled IoT streaming (on-demand only, not unconditional)
    var isStreamingIot by remember { mutableStateOf(false) }

    // Secondary UI states
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var aiContextBridgeText by remember { mutableStateOf("") }

    // Navigation tab
    var currentTab by remember {
        mutableStateOf(
            when (initialTab?.lowercase()) {
                "device", "devices" -> AppTab.Devices
                "activity" -> AppTab.Activity
                "bridge", "clipboard", "files" -> AppTab.Bridge
                "ai" -> AppTab.Ai
                else -> AppTab.Home
            }
        )
    }

    fun showFeedback(msg: String) {
        toastMessage = msg
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun logTechnical(msg: String) {
        technicalLogs = (technicalLogs + msg).takeLast(60)
    }

    // Gemini Client initialized with stored key
    val geminiClient = remember(geminiApiKey) {
        GeminiClient(apiKeyProvider = {
            if (geminiApiKey.isNotBlank()) geminiApiKey else "AQ.Ab8RN6Iqx7tg9BpiMO46e7v7EdKSoj8q3G72AeTWbN8DH0IP1w"
        })
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

    // Periodic check for history
    LaunchedEffect(Unit) {
        while (true) {
            recentEvents = repository.getMessageHistory(40)
            delay(3000)
        }
    }

    // Controlled IoT streaming: only when user has enabled it explicitly
    LaunchedEffect(isStreamingIot, connectionStatus) {
        while (isStreamingIot && connectionStatus == "CONNECTED") {
            repository.sendIotTelemetry(hub.snapshot.value.toPayloadJson(IotSensorHub.deviceName()))
            delay(2500)
        }
    }

    SyncTheme(currentTheme) {
        Scaffold(
            containerColor = DesignTokens.CanvasBackground,
            topBar = {
                if (currentTab != AppTab.Home && currentTab != AppTab.Devices) {
                    SyncEngineTopBar(
                        status = connectionStatus,
                        onOpenSettings = { showSettingsSheet = true },
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DesignTokens.NavigationDarkBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    SyncNavItem(
                        selected = currentTab == AppTab.Home,
                        onClick = { currentTab = AppTab.Home },
                        icon = Icons.Outlined.Home,
                        label = "Inicio"
                    )
                    SyncNavItem(
                        selected = currentTab == AppTab.Devices,
                        onClick = { currentTab = AppTab.Devices },
                        icon = Icons.Outlined.Smartphone,
                        label = "Dispositivos"
                    )
                    SyncNavItem(
                        selected = currentTab == AppTab.Activity,
                        onClick = { currentTab = AppTab.Activity },
                        icon = Icons.Outlined.History,
                        label = "Actividad"
                    )
                    SyncNavItem(
                        selected = currentTab == AppTab.Bridge,
                        onClick = { currentTab = AppTab.Bridge },
                        icon = Icons.Outlined.Folder,
                        label = "Bridge"
                    )
                    SyncNavItem(
                        selected = currentTab == AppTab.Ai,
                        onClick = { currentTab = AppTab.Ai },
                        icon = Icons.Outlined.AutoAwesome,
                        label = "IA"
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
                            recentEvents = recentEvents,
                            batteryPct = iotSnapshot.batteryPct,
                            onSync = {
                                repository.triggerManualSync()
                                showFeedback("Sincronizando...")
                            },
                            onQuickSend = { currentTab = AppTab.Bridge },
                            onQuickAi = { currentTab = AppTab.Ai },
                            onQuickControl = { currentTab = AppTab.Devices },
                            onViewAllActivity = { currentTab = AppTab.Activity },
                            onOpenDeviceDetail = { currentTab = AppTab.Devices }
                        )
                    }

                    AppTab.Devices -> {
                        DevicesPane(
                            status = connectionStatus,
                            serverUrl = serverUrl,
                            latencyMs = latencyMs,
                            remoteNodeLabel = remoteIotNode,
                            iotSnapshot = iotSnapshot,
                            isStreamingIot = isStreamingIot,
                            onToggleStreamIot = {
                                isStreamingIot = !isStreamingIot
                                showFeedback(if (isStreamingIot) "Telemetría activada hacia el PC" else "Telemetría pausada")
                            },
                            onLock = {
                                val ok = repository.lockScreen()
                                showFeedback(if (ok) "Bloqueando laptop Windows..." else "Sin conexión con la laptop")
                            },
                            onVolumeUp = {
                                repository.adjustVolume("VOLUME_UP")
                                showFeedback("Volumen +")
                            },
                            onVolumeDown = {
                                repository.adjustVolume("VOLUME_DOWN")
                                showFeedback("Volumen -")
                            },
                            onVolumeMute = {
                                repository.adjustVolume("VOLUME_MUTE")
                                showFeedback("Audio silenciado")
                            },
                            onMediaPlayPause = {
                                repository.adjustVolume("MEDIA_PLAY_PAUSE")
                                showFeedback("Play / Pause")
                            },
                            onPresentationNext = {
                                repository.presentationNext()
                                showFeedback("Diapositiva siguiente")
                            },
                            onPresentationPrev = {
                                repository.presentationPrev()
                                showFeedback("Diapositiva anterior")
                            },
                            onPing = {
                                val ok = repository.sendPing()
                                showFeedback(if (ok) "Ping enviado. Esperando respuesta..." else "Sin conexión")
                            },
                            onPower = {
                                repository.shutdownPc()
                                showFeedback("Apagado en 60s. Usa 'Abortar' para cancelar.")
                            },
                            onReboot = {
                                repository.rebootPc()
                                showFeedback("Reinicio en 60s. Usa 'Abortar' para cancelar.")
                            },
                            onAbort = {
                                repository.abortShutdown()
                                showFeedback("Apagado/Reinicio cancelado")
                            }
                        )
                    }

                    AppTab.Activity -> {
                        ActivityPane(events = recentEvents)
                    }

                    AppTab.Bridge -> {
                        BridgePane(
                            clipboardHistory = recentEvents,
                            onSendClipboard = { text ->
                                val ok = repository.syncClipboard(text)
                                showFeedback(if (ok) "Texto enviado a la laptop" else "Sin conexión")
                            },
                            onCopyLocal = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                showFeedback("Copiado al portapapeles")
                            },
                            onOpenUrlOnPc = { url ->
                                val ok = repository.openUrlOnPc(url)
                                showFeedback(if (ok) "Abriendo URL en la laptop..." else "Sin conexión")
                            },
                            onSendToAi = { text ->
                                aiContextBridgeText = text
                                currentTab = AppTab.Ai
                            }
                        )
                    }

                    AppTab.Ai -> {
                        AiPane(
                            geminiClient = geminiClient,
                            initialContextText = aiContextBridgeText,
                            onCopyResult = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                showFeedback("Resultado copiado")
                            },
                            onSendToPc = { text ->
                                val ok = repository.syncClipboard(text)
                                showFeedback(if (ok) "Resultado enviado a la laptop" else "Sin conexión")
                            }
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
