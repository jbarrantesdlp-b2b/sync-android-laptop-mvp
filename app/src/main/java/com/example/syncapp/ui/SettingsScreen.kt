package com.example.syncapp.ui

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.SyncApp
import com.example.syncapp.SyncViewModel
import com.example.syncapp.ui.brand.SyncEngineMark
import com.example.syncapp.ui.theme.SyncTheme
import kotlinx.coroutines.launch
import java.util.Calendar

private enum class AppTab { Home, Device, Activity, Files, Ai }

@Composable
fun SettingsScreen(viewModel: SyncViewModel? = null, initialTab: String? = null) {
    val context = LocalContext.current
    val app = remember { SyncApp.instance }
    val prefs = app.prefs
    val repo = app.repository
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("UNKNOWN") }
    var theme by remember { mutableStateOf(ThemePack.SyncEngine) }
    var url by remember { mutableStateOf("ws://10.0.2.2:8123") }
    var clip by remember { mutableStateOf("") }
    var log by remember { mutableStateOf(listOf<String>()) }
    var showQr by remember { mutableStateOf(false) }
    var tab by remember {
        mutableStateOf(
            when (initialTab) {
                "device" -> AppTab.Device
                "clipboard", "files" -> AppTab.Files
                "ai" -> AppTab.Ai
                "activity" -> AppTab.Activity
                else -> AppTab.Home
            }
        )
    }
    fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
    fun add(m: String) { log = (log + m).takeLast(40) }
    LaunchedEffect(Unit) { prefs.serverUrlFlow.collect { url = it; repo.connectToServer(it) } }
    LaunchedEffect(Unit) { prefs.connectionStatusFlow.collect { status = it; add("[STATUS] $it") } }
    LaunchedEffect(Unit) { prefs.themePackFlow.collect { theme = it } }
    val connected = status == "CONNECTED"
    val health = if (connected) 0.998f else if (status == "CONNECTING") 0.68f else 0.12f
    val cyan = Color(0xFF00BFFF)
    val ink = Color(0xFF021018)
    SyncTheme(theme) {
        Scaffold(
            containerColor = theme.background,
            bottomBar = {
                NavigationBar(containerColor = theme.surface, tonalElevation = 0.dp, modifier = Modifier.navigationBarsPadding()) {
                    @Composable
                    fun item(v: AppTab, icon: ImageVector, label: String) {
                        NavigationBarItem(
                            selected = tab == v,
                            onClick = { tab = v },
                            icon = { Icon(icon, label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = cyan,
                                selectedTextColor = cyan,
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B),
                                indicatorColor = Color(0xFF123044)
                            )
                        )
                    }
                    item(AppTab.Home, Icons.Outlined.Home, "Inicio")
                    item(AppTab.Device, Icons.Outlined.Smartphone, "Dispositivo")
                    item(AppTab.Activity, Icons.Outlined.History, "Actividad")
                    item(AppTab.Files, Icons.Outlined.Folder, "Archivos")
                    item(AppTab.Ai, Icons.Outlined.AutoAwesome, "IA")
                }
            }
        ) { pad ->
            Column(
                Modifier.fillMaxSize().padding(pad).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (tab) {
                    AppTab.Home -> {
                        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val g = if (h < 12) "Buenos d\u00edas" else if (h < 19) "Buenas tardes" else "Buenas noches"
                        Text("$g,", color = theme.textSecondary)
                        Text("Jose.", color = theme.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text(if (connected) "Todo conectado. Listo para avanzar." else "Buscando tu laptop\u2026", color = theme.textSecondary, fontSize = 13.sp)
                        GlowCard(theme) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Ring(health, Modifier.size(132.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("${(health * 1000).toInt() / 10f}%", color = Color(0xFF10B981), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("Sync", color = theme.textSecondary)
                                    Text(Build.MODEL ?: "Xiaomi 2312", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(if (connected) "Conectado" else "Desconectado", color = if (connected) Color(0xFF10B981) else theme.danger, fontSize = 12.sp)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickTile("Portapapeles", Icons.Outlined.ContentCopy, theme, Modifier.weight(1f)) { tab = AppTab.Files }
                            QuickTile("Sync", Icons.Outlined.Refresh, theme, Modifier.weight(1f)) { repo.triggerManualSync(); toast("Sincronizando\u2026") }
                            QuickTile("IA", Icons.Outlined.AutoAwesome, theme, Modifier.weight(1f)) { tab = AppTab.Ai }
                            QuickTile("Lock", Icons.Outlined.Lock, theme, Modifier.weight(1f)) {
                                toast(if (repo.lockScreen()) "Bloqueando\u2026" else "Sin conexi\u00f3n")
                            }
                        }
                    }
                    AppTab.Device -> {
                        Text("Control de dispositivo", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        GlowCard(theme) { Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) { SyncEngineMark(size = 88.dp) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { repo.lockScreen(); toast("Lock") }, colors = ButtonDefaults.buttonColors(cyan, ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Bloquear") }
                            Button(onClick = { repo.presentationNext() }, colors = ButtonDefaults.buttonColors(theme.surfaceAlt, theme.textPrimary), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text("Slide") }
                        }
                    }
                    AppTab.Activity -> {
                        Text("Actividad", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(url, { url = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("ws://IP:8123") }, colors = fieldColors(theme), shape = RoundedCornerShape(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showQr = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(cyan, ink), shape = RoundedCornerShape(14.dp)) {
                                Icon(Icons.Outlined.QrCodeScanner, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("QR")
                            }
                            Button(onClick = { viewModel?.setServerUrl(url); repo.connectToServer(url); toast("Conectando\u2026") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(theme.surfaceAlt, theme.textPrimary), shape = RoundedCornerShape(14.dp)) { Text("Conectar") }
                        }
                        ThemePack.values().forEach { p ->
                            Text(p.displayName, color = if (p == theme) cyan else theme.textSecondary, modifier = Modifier.clickable { scope.launch { prefs.setTheme(p) } }.padding(6.dp))
                        }
                        log.takeLast(12).reversed().forEach { Text(it, color = Color(0xFF7DD3FC), fontSize = 11.sp) }
                    }
                    AppTab.Files -> {
                        Text("Portapapeles", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(clip, { clip = it }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("Texto o URL\u2026") }, colors = fieldColors(theme), shape = RoundedCornerShape(14.dp))
                        Button(onClick = {
                            val ok = repo.syncClipboard(clip); toast(if (ok) "Enviado" else "Sin conexi\u00f3n"); if (ok) clip = ""
                        }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(cyan, ink), shape = RoundedCornerShape(16.dp)) { Text("Enviar al dispositivo", fontWeight = FontWeight.Bold) }
                    }
                    AppTab.Ai -> {
                        Text("IA Assist", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SyncEngineMark(size = 88.dp) }
                        OutlinedTextField(clip, { clip = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pregunta algo\u2026") }, colors = fieldColors(theme), shape = RoundedCornerShape(14.dp))
                        Button(onClick = { repo.sendData("AI:" + clip); toast("Pedido enviado") }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(cyan, ink), shape = RoundedCornerShape(16.dp)) { Text("Enviar", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        if (showQr) QrScannerDialog(onDismiss = { showQr = false }, onQrScanned = {
            showQr = false; url = it; viewModel?.setServerUrl(it); repo.connectToServer(it); toast("Vinculado")
        })
    }
}

@Composable private fun GlowCard(theme: ThemePack, c: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), theme.surface, border = BorderStroke(1.dp, Color(0xFF1E293B)), content = c)
}

@Composable private fun QuickTile(label: String, icon: ImageVector, theme: ThemePack, mod: Modifier, onClick: () -> Unit) {
    Column(mod.clip(RoundedCornerShape(18.dp)).background(theme.surface).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = Color(0xFF00BFFF), modifier = Modifier.size(18.dp))
        Text(label, color = theme.textSecondary, fontSize = 10.sp)
    }
}

@Composable private fun Ring(progress: Float, modifier: Modifier) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "r")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(size.minDimension * 0.08f, cap = StrokeCap.Round)
            drawArc(Color(0xFF123044), -90f, 360f, false, style = s)
            drawArc(Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFF00BFFF), Color(0xFF8000FF), Color(0xFF00F0FF))), -90f, 360f * p, false, style = s)
        }
        SyncEngineMark(size = 48.dp)
    }
}

@Composable private fun fieldColors(theme: ThemePack) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = theme.primary, unfocusedBorderColor = theme.border,
    focusedLabelColor = theme.primary, unfocusedLabelColor = theme.textSecondary,
    focusedTextColor = theme.textPrimary, unfocusedTextColor = theme.textPrimary, cursorColor = theme.primary
)
