package com.example.syncapp.ui

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.SyncApp
import com.example.syncapp.ui.brand.SyncEngineMark
import kotlinx.coroutines.launch

@Composable
internal fun ActivityPane(theme: ThemePack, logs: List<String>) {
    val context = LocalContext.current
    val syncApp = remember { SyncApp.instance }
    val prefs = syncApp.prefs
    val repository = syncApp.repository
    var url by remember { mutableStateOf("") }
    var showQr by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        prefs.serverUrlFlow.collect { url = it }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Actividad", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("VINCULACION", color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF), contentColor = Color(0xFF021018))
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("QR", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            scope.launch { prefs.setServerUrl(url) }
                            repository.connectToServer(url)
                            Toast.makeText(context, "Conectando...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.surfaceAlt, contentColor = theme.textPrimary)
                    ) { Text("Conectar", fontWeight = FontWeight.Bold) }
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
            SyncEngineMark(size = 96.dp)
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
