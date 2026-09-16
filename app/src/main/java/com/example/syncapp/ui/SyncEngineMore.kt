package com.example.syncapp.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.SyncApp
import com.example.syncapp.ui.brand.SyncEngineLockup
import kotlinx.coroutines.launch

@Composable
internal fun ActivityPane(theme: ThemePack, logs: List<String>) {
    val context = LocalContext.current
    val syncApp = remember { SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    var url by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val discoverStatus by syncApp.discovery.status.collectAsState()
    val discovered by syncApp.discovery.peer.collectAsState()

    val blePerms = remember {
        if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) syncApp.restartDiscovery()
    }
    LaunchedEffect(Unit) { permLauncher.launch(blePerms) }
    LaunchedEffect(Unit) { prefs.serverUrlFlow.collect { url = it } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Actividad", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ENLACE AUTOMATICO", color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    discoverStatus,
                    color = Color(0xFF00BFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    discovered?.let { "${it.name} · ${it.via} · ${it.url}" }
                        ?: "Wi-Fi (UDP + mDNS) y Bluetooth LE. El QR ya no hace falta.",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            permLauncher.launch(blePerms)
                            syncApp.restartDiscovery()
                            Toast.makeText(context, "Buscando laptop…", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF), contentColor = Color(0xFF021018))
                    ) {
                        Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buscar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { permLauncher.launch(blePerms); syncApp.restartDiscovery() },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceAlt, contentColor = theme.textPrimary)
                    ) {
                        Icon(Icons.Outlined.Bluetooth, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bluetooth", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = { showManual = !showManual }) {
                    Text(if (showManual) "Ocultar manual" else "Manual / QR (respaldo)", color = theme.textSecondary, fontSize = 12.sp)
                }
                if (showManual) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("ws://IP:8123", fontSize = 12.sp) },
                        colors = fieldColors(theme),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showQr = true },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceAlt, contentColor = theme.textPrimary)
                        ) {
                            Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("QR")
                        }
                        Button(
                            onClick = {
                                scope.launch { prefs.setServerUrl(url) }
                                repository.connectToServer(url)
                                Toast.makeText(context, "Conectando...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceAlt, contentColor = theme.textPrimary)
                        ) { Text("Conectar") }
                    }
                }
                Text("Tema", color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePack.values().forEach { pack ->
                        val selected = pack == theme
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color(0xFF00BFFF) else theme.surfaceAlt,
                            modifier = Modifier.clickable { scope.launch { prefs.setTheme(pack) } }
                        ) {
                            Text(
                                pack.displayName,
                                color = if (selected) Color(0xFF021018) else theme.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        Text("Registro", color = theme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (logs.isEmpty()) {
                    Text("Sin eventos todavia.", color = theme.textSecondary, fontSize = 13.sp)
                } else {
                    logs.takeLast(18).reversed().forEach { line ->
                        Text(line, color = Color(0xFF7DD3FC), fontSize = 11.sp)
                    }
                }
            }
        }
        if (showQr) {
            QrScannerDialog(
                onDismiss = { showQr = false },
                onQrScanned = {
                    showQr = false
                    url = it
                    scope.launch { prefs.setServerUrl(it) }
                    repository.connectToServer(it)
                }
            )
        }
    }
}

@Composable
internal fun FilesPane(
    theme: ThemePack,
    clipboardText: String,
    onClipboardChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenUrl: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Portapapeles", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Tu contexto, en movimiento.", color = theme.textSecondary, fontSize = 13.sp)
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clipboardText,
                    onValueChange = onClipboardChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Pega texto o una URL...") },
                    colors = fieldColors(theme),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = onSend,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF), contentColor = Color(0xFF021018))
                ) { Text("Enviar al dispositivo", fontWeight = FontWeight.Bold) }
                TextButton(onClick = onOpenUrl, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir URL en el PC", color = Color(0xFF00BFFF))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiPane(
    theme: ThemePack,
    prompt: String,
    onPrompt: (String) -> Unit,
    onAsk: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("IA Assist", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Tu contexto, mas inteligente.", color = theme.textSecondary, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SyncEngineLockup(size = 180.dp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlTile("Resumir", Icons.Outlined.AutoAwesome, theme) { onAsk("resumir") }
            ControlTile("Traducir", Icons.Outlined.NorthEast, theme) { onAsk("traducir") }
            ControlTile("Acciones", Icons.Outlined.AutoAwesome, theme) { onAsk("acciones") }
            ControlTile("Mejorar", Icons.Outlined.Refresh, theme) { onAsk("mejorar") }
        }
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPrompt,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pregunta algo...") },
                    colors = fieldColors(theme),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = { onAsk("prompt") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF), contentColor = Color(0xFF021018))
                ) { Text("Enviar", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
