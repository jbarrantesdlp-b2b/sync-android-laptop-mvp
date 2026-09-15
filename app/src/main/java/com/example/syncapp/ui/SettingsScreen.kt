package com.example.syncapp.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.SyncApp
import com.example.syncapp.SyncViewModel
import com.example.syncapp.iot.IotSensorHub
import com.example.syncapp.ui.theme.SyncTheme
import kotlinx.coroutines.delay
import org.json.JSONObject

private enum class AppTab { Home, Device, Activity, Files, Sensors }

@Composable
fun SettingsScreen(
    viewModel: SyncViewModel? = null,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val syncApp = remember { SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    @Suppress("UNUSED_VARIABLE")
    val keptViewModel = viewModel

    val hub = remember { IotSensorHub(context) }
    val iot by hub.snapshot.collectAsState()
    DisposableEffect(Unit) {
        hub.start()
        onDispose { hub.stop() }
    }

    var connectionStatus by remember { mutableStateOf("UNKNOWN") }
    var currentTheme by remember { mutableStateOf(ThemePack.SyncEngine) }
    var syncLog by remember { mutableStateOf(listOf<String>()) }
    var lastMessageText by remember { mutableStateOf<String?>(null) }
    var serverUrlInput by remember { mutableStateOf("ws://10.0.2.2:8123") }
    var clipboardText by remember { mutableStateOf("") }
    var showSyncing by remember { mutableStateOf(false) }
    var streaming by remember { mutableStateOf(true) }
    var remoteIot by remember { mutableStateOf<String?>(null) }
    var tab by remember {
        mutableStateOf(
            when (initialTab) {
                "device" -> AppTab.Device
                "clipboard", "files" -> AppTab.Files
                "ai", "sensors" -> AppTab.Sensors
                "activity" -> AppTab.Activity
                else -> AppTab.Home
            }
        )
    }
    var latencyLabel by remember { mutableStateOf("-") }

    fun log(line: String) { syncLog = (syncLog + line).takeLast(60) }
    fun toast(msg: String) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    LaunchedEffect(Unit) {
        prefs.serverUrlFlow.collect { url ->
            if (serverUrlInput != url) serverUrlInput = url
            repository.connectToServer(url)
        }
    }
    LaunchedEffect(Unit) {
        prefs.connectionStatusFlow.collect { status ->
            if (connectionStatus != status) {
                connectionStatus = status
                log("[STATUS] $status")
                if (status == "CONNECTED") showSyncing = false
            }
        }
    }
    LaunchedEffect(Unit) { prefs.themePackFlow.collect { theme -> if (currentTheme != theme) { currentTheme = theme; log("[THEME] ${theme.displayName}") } } }
    LaunchedEffect(Unit) {
        repository.lastMessage.collect { msg ->
            if (!msg.isNullOrEmpty() && lastMessageText != msg) {
                lastMessageText = msg
                log("[DATA] ${msg.take(80)}")
                try {
                    val obj = JSONObject(msg)
                    if (obj.optString("type") == "IOT_TELEMETRY") {
                        val payload = obj.optJSONObject("payload") ?: obj
                        val source = payload.optString("source")
                        if (source != "phone") {
                            remoteIot = source + " · " + payload.optString("device", "nodo")
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }
    LaunchedEffect(Unit) { repository.latencyMs.collect { ms -> if (ms != null) latencyLabel = "$ms ms" } }
    LaunchedEffect(streaming, connectionStatus) {
        while (streaming && connectionStatus == "CONNECTED") {
            repository.sendIotTelemetry(hub.snapshot.value.toPayloadJson(IotSensorHub.deviceName()))
            delay(2500)
        }
    }

    val health = when (connectionStatus) {
        "CONNECTED" -> 0.998f
        "CONNECTING" -> 0.68f
        else -> 0.12f
    }

    SyncTheme(currentTheme) {
        Scaffold(
            containerColor = currentTheme.background,
            bottomBar = {
                NavigationBar(containerColor = currentTheme.surface, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                    NavItem(AppTab.Home, tab, Icons.Outlined.Home, "Inicio") { tab = it }
                    NavItem(AppTab.Device, tab, Icons.Outlined.Smartphone, "Dispositivo") { tab = it }
                    NavItem(AppTab.Sensors, tab, Icons.Outlined.Sensors, "IoT") { tab = it }
                    NavItem(AppTab.Files, tab, Icons.Outlined.Folder, "Archivos") { tab = it }
                    NavItem(AppTab.Activity, tab, Icons.Outlined.History, "Actividad") { tab = it }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
                when (tab) {
                    AppTab.Home -> HomePane(
                        theme = currentTheme, status = connectionStatus, health = health,
                        latency = latencyLabel, lastMessage = lastMessageText,
                        onClipboard = { tab = AppTab.Files }, onSend = { tab = AppTab.Files },
                        onAi = { tab = AppTab.Sensors }, onMore = { tab = AppTab.Device },
                        onSync = { showSyncing = true; repository.triggerManualSync(); toast("Sincronizando...") }
                    )
                    AppTab.Device -> DevicePane(
                        theme = currentTheme, status = connectionStatus, serverUrl = serverUrlInput,
                        onLock = { toast(if (repository.lockScreen()) "Bloqueando laptop..." else "Sin conexion") },
                        onVolume = { repository.adjustVolume("VOLUME_UP") },
                        onNext = { repository.presentationNext() },
                        onPrev = { repository.presentationPrev() },
                        onPower = { repository.shutdownPc(); toast("Apagado en 60s. Usa Abortar para cancelar.") },
                        onReboot = { repository.rebootPc(); toast("Reinicio en 60s. Usa Abortar para cancelar.") },
                        onAbort = { repository.abortShutdown(); toast("Apagado cancelado") }
                    )
                    AppTab.Sensors -> SensorsPane(
                        theme = currentTheme, snapshot = iot, streaming = streaming,
                        connected = connectionStatus == "CONNECTED", remoteLabel = remoteIot,
                        onToggleStream = { streaming = !streaming; toast(if (streaming) "IoT al PC" else "Transmision pausada") }
                    )
                    AppTab.Activity -> ActivityPane(currentTheme, syncLog)
                    AppTab.Files -> FilesPane(
                        theme = currentTheme, clipboardText = clipboardText,
                        onClipboardChange = { clipboardText = it },
                        onSend = {
                            val ok = repository.syncClipboard(clipboardText)
                            toast(if (ok) "Enviado al PC" else "Sin conexion")
                            if (ok) clipboardText = ""
                        },
                        onOpenUrl = { toast(if (repository.openUrlOnPc(clipboardText)) "Abriendo URL" else "Sin conexion") }
                    )
                }
                if (showSyncing) {
                    SyncingOverlay(
                        theme = currentTheme,
                        progress = if (connectionStatus == "CONNECTED") 1f else 0.68f,
                        onCancel = { showSyncing = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(value: AppTab, current: AppTab, icon: ImageVector, label: String, onClick: (AppTab) -> Unit) {
    NavigationBarItem(
        selected = current == value,
        onClick = { onClick(value) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 10.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFF00BFFF),
            selectedTextColor = Color(0xFF00BFFF),
            unselectedIconColor = Color(0xFF64748B),
            unselectedTextColor = Color(0xFF64748B),
            indicatorColor = Color(0xFF123044)
        )
    )
}
