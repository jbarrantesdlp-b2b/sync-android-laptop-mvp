package com.example.syncapp.ui

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.ui.brand.SyncEngineLockup
import com.example.syncapp.ui.brand.SyncEngineMark
import java.util.Calendar

@Composable
internal fun HomePane(
    theme: ThemePack,
    status: String,
    health: Float,
    latency: String,
    lastMessage: String?,
    onClipboard: () -> Unit,
    onSend: () -> Unit,
    onAi: () -> Unit,
    onMore: () -> Unit,
    onSync: () -> Unit
) {
    val greeting = remember {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            h < 12 -> "Buenos dias"
            h < 19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    val connected = status == "CONNECTED"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting + ",", color = theme.textSecondary, fontSize = 16.sp)
                Text("Jose.", color = theme.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (connected) "Todo conectado. Listo para avanzar." else "Buscando tu laptop...",
                    color = theme.textSecondary,
                    fontSize = 13.sp
                )
            }
            IconButton(onClick = onMore) {
                Icon(Icons.Outlined.Notifications, null, tint = theme.textSecondary)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF123044)),
                contentAlignment = Alignment.Center
            ) {
                Text("J", color = Color(0xFF00BFFF), fontWeight = FontWeight.Bold)
            }
        }

        GlassCard(theme) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                HealthRing(progress = health, modifier = Modifier.size(150.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricChip("Sync", "${(health * 1000).toInt() / 10f}%", Color(0xFF10B981))
                    MetricChip("Latencia", latency, Color(0xFF00BFFF))
                }
            }
        }

        GlassCard(theme, onClick = onMore) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF1D4ED8), Color(0xFF020617)))),
                    contentAlignment = Alignment.Center
                ) {
                    SyncEngineMark(size = 32.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(Build.MODEL ?: "Xiaomi 2312", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (connected) "Conectado" else "Desconectado",
                        color = if (connected) Color(0xFF10B981) else theme.danger,
                        fontSize = 12.sp
                    )
                }
                Text(">", color = theme.textSecondary, fontSize = 22.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Portapapeles", Icons.Outlined.ContentCopy, Modifier.weight(1f), theme, onClipboard)
            QuickAction("Enviar", Icons.Outlined.NorthEast, Modifier.weight(1f), theme, onSend)
            QuickAction("IoT", Icons.Outlined.Sensors, Modifier.weight(1f), theme, onAi)
            QuickAction("Mas", Icons.Outlined.Lock, Modifier.weight(1f), theme, onMore)
        }

        Text("Actividad reciente", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
        GlassCard(theme) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF123044)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentCopy, null, tint = Color(0xFF00BFFF), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(lastMessage?.take(42) ?: "Mensaje sincronizado", color = theme.textPrimary, fontSize = 14.sp)
                    Text(
                        if (connected) "Hace un momento  ${Build.MODEL}" else "Esperando enlace",
                        color = theme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Button(
            onClick = onSync,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFFF), contentColor = Color(0xFF021018))
        ) {
            Icon(Icons.Outlined.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Sincronizar ahora", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DevicePane(
    theme: ThemePack,
    status: String,
    serverUrl: String,
    onLock: () -> Unit,
    onVolume: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onPower: () -> Unit,
    onReboot: () -> Unit,
    onAbort: () -> Unit
) {
    val connected = status == "CONNECTED"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Control de dispositivo", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "${Build.MODEL ?: "Este telefono"}  ${if (connected) "Conectado" else "Desconectado"}",
            color = if (connected) Color(0xFF10B981) else theme.textSecondary,
            fontSize = 13.sp
        )
        GlassCard(theme) {
            Box(
                Modifier.fillMaxWidth().height(160.dp).background(
                    Brush.linearGradient(listOf(Color(0xFF022C43), Color(0xFF0B1220), Color(0xFF1E1B4B)))
                ),
                contentAlignment = Alignment.Center
            ) {
                SyncEngineLockup(size = 148.dp)
            }
        }
        Text("Acciones", color = theme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlTile("Bloquear", Icons.Outlined.Lock, theme, onLock)
            ControlTile("Reiniciar", Icons.Outlined.Refresh, theme, onReboot)
            ControlTile("Apagar", Icons.Outlined.PowerSettingsNew, theme, onPower)
            ControlTile("Volumen", Icons.Outlined.VolumeUp, theme, onVolume)
            ControlTile("Siguiente", Icons.Outlined.NorthEast, theme, onNext)
            ControlTile("Anterior", Icons.Outlined.History, theme, onPrev)
            ControlTile("Abortar", Icons.Outlined.Notifications, theme, onAbort)
        }
        Text("Enlace", color = theme.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(serverUrl, color = Color(0xFF00BFFF), fontSize = 12.sp)
                Text("Escanea el QR de la laptop en la pestana Actividad.", color = theme.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun SyncingOverlay(theme: ThemePack, progress: Float, onCancel: () -> Unit) {
    val animated by animateFloatAsState(progress, tween(700), label = "sync")
    Box(
        Modifier.fillMaxSize().background(theme.background.copy(alpha = 0.96f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Sincronizando...", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("Manten tus ideas en movimiento.", color = theme.textSecondary)
            HealthRing(progress = animated, modifier = Modifier.size(200.dp), showLogo = true)
            Text("${(animated * 100).toInt()}%", color = Color(0xFF00BFFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
            ) { Text("Cancelar") }
        }
    }
}

@Composable
internal fun HealthRing(progress: Float, modifier: Modifier, showLogo: Boolean = true) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = size.minDimension * 0.08f, cap = StrokeCap.Round)
            drawArc(color = Color(0xFF123044), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFF00BFFF), Color(0xFF8000FF), Color(0xFF00F0FF))),
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                style = stroke
            )
        }
        if (showLogo) SyncEngineMark(size = 56.dp)
    }
}

@Composable
internal fun GlassCard(theme: ThemePack, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(24.dp),
        color = theme.surface,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        shadowElevation = 0.dp,
        content = content
    )
}

@Composable
internal fun MetricChip(label: String, value: String, color: Color) {
    Column {
        Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
    }
}

@Composable
internal fun QuickAction(label: String, icon: ImageVector, modifier: Modifier, theme: ThemePack, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(theme.surface).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = Color(0xFF00BFFF), modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = theme.textSecondary, fontSize = 10.sp)
    }
}

@Composable
internal fun ControlTile(label: String, icon: ImageVector, theme: ThemePack, onClick: () -> Unit) {
    Column(
        Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)).background(theme.surface).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = Color(0xFF00BFFF), modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = theme.textPrimary, fontSize = 11.sp)
    }
}

@Composable
internal fun fieldColors(theme: ThemePack) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = theme.primary,
    unfocusedBorderColor = theme.border,
    focusedLabelColor = theme.primary,
    unfocusedLabelColor = theme.textSecondary,
    focusedTextColor = theme.textPrimary,
    unfocusedTextColor = theme.textPrimary,
    cursorColor = theme.primary
)
