package com.example.syncapp.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.datastore.PreferencesManager
import com.example.core.model.ThemePack
import com.example.sync.ConnectivityObserver
import com.example.sync.NetworkState
import com.example.sync.SyncRepository
import kotlinx.coroutines.launch

@Composable
fun WaveOverlay(modifier: Modifier = Modifier, color: Color = Color.White.copy(alpha = 0.3f)) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            moveTo(0f, height * 0.65f)
            cubicTo(
                width * 0.25f, height * 0.25f,
                width * 0.5f, height * 0.85f,
                width * 0.75f, height * 0.35f
            )
            cubicTo(
                width * 0.88f, height * 0.15f,
                width * 0.95f, height * 0.45f,
                width, height * 0.3f
            )
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        val path2 = Path().apply {
            moveTo(0f, height * 0.85f)
            cubicTo(
                width * 0.3f, height * 0.55f,
                width * 0.6f, height * 0.92f,
                width, height * 0.68f
            )
        }

        drawPath(
            path = path2,
            color = color.copy(alpha = 0.18f),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val syncApp = remember { com.example.syncapp.SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    val connectivityObserver = remember { ConnectivityObserver(context) }
    val scope = rememberCoroutineScope()

    var connectionStatus by remember { mutableStateOf("UNKNOWN") }
    var currentTheme by remember { mutableStateOf(ThemePack.Minimal) }
    var networkState by remember { mutableStateOf<NetworkState>(NetworkState.Unknown) }
    var syncLog by remember { mutableStateOf(listOf<String>()) }
    var lastMessageText by remember { mutableStateOf<String?>(null) }

    var serverUrlInput by remember { mutableStateOf("ws://10.0.2.2:8123") }
    var clipboardText by remember { mutableStateOf("") }
    var showQrScanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        connectivityObserver.startObserving()
        syncLog = listOf(
            "[INIT] App Engine iniciado correctamente",
            "[INIT] Connectivity Observer activo",
            "[INIT] WorkManager programado (15 min)"
        )
    }

    LaunchedEffect(Unit) {
        prefs.serverUrlFlow.collect { url ->
            if (serverUrlInput != url) {
                serverUrlInput = url
            }
            repository.connectToServer(url)
        }
    }

    LaunchedEffect(Unit) {
        prefs.connectionStatusFlow.collect { status ->
            if (connectionStatus != status) {
                connectionStatus = status
                syncLog = (syncLog + "[STATUS] Conexión -> $status").takeLast(50)
            }
        }
    }

    LaunchedEffect(Unit) {
        prefs.themePackFlow.collect { theme ->
            if (currentTheme != theme) {
                currentTheme = theme
                syncLog = (syncLog + "[THEME] Tema activo -> ${theme.displayName}").takeLast(50)
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.lastMessage.collect { msg ->
            if (!msg.isNullOrEmpty() && lastMessageText != msg) {
                lastMessageText = msg
                syncLog = (syncLog + "[DATA] Recibido -> $msg").takeLast(50)
            }
        }
    }

    LaunchedEffect(Unit) {
        connectivityObserver.networkState.collect { state ->
            if (networkState != state) {
                networkState = state
                val networkType = when (state) {
                    is NetworkState.Connected -> state.type
                    NetworkState.Disconnected -> "Desconectado"
                    NetworkState.Connecting -> "Conectando..."
                    NetworkState.Unknown -> "Desconocido"
                }
                syncLog = (syncLog + "[NET] Estado de red -> $networkType").takeLast(50)
            }
        }
    }

    // Animated theme transitions
    val animatedBg by animateColorAsState(targetValue = currentTheme.background, animationSpec = tween(500), label = "bg")
    val animatedSurface by animateColorAsState(targetValue = currentTheme.surface, animationSpec = tween(500), label = "surface")
    val animatedPrimary by animateColorAsState(targetValue = currentTheme.primary, animationSpec = tween(500), label = "primary")
    val animatedBorder by animateColorAsState(targetValue = currentTheme.border, animationSpec = tween(500), label = "border")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = animatedBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COUTES",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Sync Engine",
                        color = currentTheme.textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = animatedSurface,
                    border = BorderStroke(1.dp, animatedBorder)
                ) {
                    Text(
                        text = "v1.0 MVP",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Hero Landscape Wave Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, animatedBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (currentTheme == ThemePack.Minimal) {
                                    listOf(Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF1E293B))
                                } else {
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
                                }
                            )
                        )
                ) {
                    WaveOverlay(modifier = Modifier.fillMaxSize())

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYSTEM BRIDGE",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            // Status Indicator Pill
                            val dotColor = when (connectionStatus) {
                                "CONNECTED" -> Color(0xFF10B981)
                                "CONNECTING" -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Text(
                                        text = connectionStatus,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (connectionStatus == "CONNECTED") "097 .26" else "OFFLINE",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = serverUrlInput,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Server Address Connection Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = animatedSurface),
                border = BorderStroke(1.dp, animatedBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CONEXIÓN AL SERVIDOR (LAPTOP)",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("URL WebSocket (ej. ws://192.168.1.50:8123)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = currentTheme.primary,
                            unfocusedBorderColor = animatedBorder,
                            focusedLabelColor = currentTheme.primary,
                            unfocusedLabelColor = currentTheme.textSecondary,
                            focusedTextColor = currentTheme.textPrimary,
                            unfocusedTextColor = currentTheme.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showQrScanner = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentTheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("📷 Escanear QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    prefs.setServerUrl(serverUrlInput)
                                    repository.connectToServer(serverUrlInput)
                                    syncLog = (syncLog + "[WS] Conectando a $serverUrlInput...").takeLast(50)
                                    Toast.makeText(context, "Guardado. Conectando a $serverUrlInput", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = animatedBg,
                                contentColor = currentTheme.textPrimary
                            ),
                            border = BorderStroke(1.dp, animatedBorder)
                        ) {
                            Text("🔌 Conectar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (showQrScanner) {
                QrScannerDialog(
                    onDismiss = { showQrScanner = false },
                    onQrScanned = { scannedUrl ->
                        showQrScanner = false
                        serverUrlInput = scannedUrl
                        scope.launch {
                            prefs.setServerUrl(scannedUrl)
                            repository.connectToServer(scannedUrl)
                            syncLog = (syncLog + "[QR] Vinculado a $scannedUrl").takeLast(50)
                            Toast.makeText(context, "QR Vinculado: $scannedUrl", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Interactive Dashboard Actions Card (Graphic 2 Dashboard)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = animatedSurface),
                border = BorderStroke(1.dp, animatedBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "DASHBOARD INTERACTIVO (CONTROL TOTAL)",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // A. Hardware & Estado
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "A. HARDWARE & CONTROL DE AUDIO",
                            color = currentTheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val sent = repository.lockScreen()
                                    syncLog = (syncLog + if (sent) "[CMD] Bloquear Laptop enviado" else "[FAIL] No conectado").takeLast(50)
                                    Toast.makeText(context, if (sent) "Bloqueando laptop..." else "Sin conexión", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🔒 Bloquear PC", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val sent = repository.sendPing()
                                    syncLog = (syncLog + if (sent) "[PING] Enviado" else "[FAIL] No conectado").takeLast(50)
                                    Toast.makeText(context, if (sent) "Ping enviado" else "Sin conexión", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🚀 Test Ping", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Volume and Media Controls
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { repository.adjustVolume("VOLUME_MUTE") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🔇 Mute", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { repository.adjustVolume("VOLUME_DOWN") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🔉 Vol-", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { repository.adjustVolume("VOLUME_UP") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🔊 Vol+", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { repository.adjustVolume("MEDIA_PLAY_PAUSE") },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("⏯️ Play", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = animatedBorder, thickness = 1.dp)

                    // B. Software & Sesión (Presentation Clicker)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "B. SOFTWARE & SESIÓN (CLICKER PRESENTACIÓN)",
                            color = currentTheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val sent = repository.presentationPrev()
                                    syncLog = (syncLog + if (sent) "[CLICKER] Diapositiva Anterior" else "[FAIL] No conectado").takeLast(50)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("◀️ Diapositiva Anterior", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val sent = repository.presentationNext()
                                    syncLog = (syncLog + if (sent) "[CLICKER] Diapositiva Siguiente" else "[FAIL] No conectado").takeLast(50)
                                },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = animatedBg, contentColor = currentTheme.textPrimary),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("▶️ Siguiente", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Divider(color = animatedBorder, thickness = 1.dp)

                    // C. Transferencia Fluida (Portapapeles Universal & Smart URLs)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "C. TRANSFERENCIA FLUIDA & ENLACES",
                            color = currentTheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = clipboardText,
                            onValueChange = { clipboardText = it },
                            placeholder = { Text("Escribe texto o pega URL (ej. https://...)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentTheme.primary,
                                unfocusedBorderColor = animatedBorder,
                                focusedTextColor = currentTheme.textPrimary,
                                unfocusedTextColor = currentTheme.textPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (clipboardText.isNotBlank()) {
                                        val sent = repository.syncClipboard(clipboardText)
                                        syncLog = (syncLog + if (sent) "[CLIPBOARD] Copiado a Laptop" else "[FAIL] No conectado").takeLast(50)
                                        Toast.makeText(context, if (sent) "Copiado en laptop" else "Sin conexión", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentTheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("📋 Copiar en Laptop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val urlToOpen = if (clipboardText.startsWith("http://") || clipboardText.startsWith("https://")) {
                                        clipboardText
                                    } else if (clipboardText.isNotBlank()) {
                                        "https://$clipboardText"
                                    } else ""

                                    if (urlToOpen.isNotBlank()) {
                                        val sent = repository.openUrlOnPc(urlToOpen)
                                        syncLog = (syncLog + if (sent) "[URL] Abriendo en PC -> $urlToOpen" else "[FAIL] No conectado").takeLast(50)
                                        Toast.makeText(context, if (sent) "Abriendo en navegador de PC" else "Sin conexión", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = animatedBg,
                                    contentColor = currentTheme.textPrimary
                                ),
                                border = BorderStroke(1.dp, animatedBorder)
                            ) {
                                Text("🌐 Abrir Enlace en PC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stats Grid Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = animatedSurface),
                border = BorderStroke(1.dp, animatedBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ENGINE", color = currentTheme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("100%", color = currentTheme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("Salud OK", color = currentTheme.textSecondary, fontSize = 9.sp)
                        }

                        Divider(
                            modifier = Modifier.height(40.dp).width(1.dp),
                            color = animatedBorder
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("USO COLA", color = currentTheme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("0%", color = currentTheme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("0 Pendientes", color = currentTheme.textSecondary, fontSize = 9.sp)
                        }

                        Divider(
                            modifier = Modifier.height(40.dp).width(1.dp),
                            color = animatedBorder
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PING", color = currentTheme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (connectionStatus == "CONNECTED") "<10ms" else "--",
                                color = if (connectionStatus == "CONNECTED") Color(0xFF10B981) else currentTheme.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Latencia", color = currentTheme.textSecondary, fontSize = 9.sp)
                        }
                    }

                    Divider(color = animatedBorder, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentNetworkState = networkState
                        val netLabel = when (currentNetworkState) {
                            is NetworkState.Connected -> "📶 ${currentNetworkState.type}"
                            NetworkState.Disconnected -> "❌ Sin Red"
                            NetworkState.Connecting -> "⏳ Conectando"
                            NetworkState.Unknown -> "❓ Red"
                        }
                        Text(
                            text = "Red: $netLabel",
                            color = currentTheme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "WorkManager: 15m",
                            color = currentTheme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!lastMessageText.isNullOrEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = animatedBg,
                            border = BorderStroke(1.dp, animatedBorder)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Último mensaje recibido:",
                                    color = currentTheme.textSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = lastMessageText ?: "",
                                    color = currentTheme.primary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Theme Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ESTILOS VISUALES & TEMA",
                    color = currentTheme.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemePack.values().forEach { pack ->
                        val isSelected = currentTheme == pack
                        val cardBg = if (isSelected) currentTheme.primary else animatedSurface
                        val textColor = if (isSelected) Color.White else currentTheme.textSecondary

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    scope.launch { prefs.setTheme(pack) }
                                    Toast.makeText(context, "Estilo: ${pack.displayName}", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, if (isSelected) currentTheme.primary else animatedBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pack.primary))
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pack.border))
                                }
                                Text(
                                    text = pack.displayName.split(" ")[0],
                                    color = textColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Event Terminal Log Card
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMINAL LOG",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Limpiar Log",
                        color = currentTheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            syncLog = listOf("[CLEARED] Terminal reiniciada")
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = animatedSurface),
                    border = BorderStroke(1.dp, animatedBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            syncLog.takeLast(15).forEach { log ->
                                Text(
                                    text = log,
                                    color = currentTheme.textSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            connectivityObserver.stopObserving()
        }
    }
}
