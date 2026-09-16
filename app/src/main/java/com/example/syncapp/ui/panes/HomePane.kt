package com.example.syncapp.ui.panes

import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncMessage
import com.example.syncapp.ui.brand.SyncEngineMark
import com.example.syncapp.ui.components.ActivityItem
import com.example.syncapp.ui.components.MetricCard
import com.example.syncapp.ui.components.QuickAction
import com.example.syncapp.ui.components.SectionHeader
import com.example.syncapp.ui.components.StatusBadge
import com.example.syncapp.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomePane(
    status: String,
    latencyMs: Long?,
    serverUrl: String,
    recentEvents: List<SyncMessage>,
    batteryPct: Int?,
    onSync: () -> Unit,
    onQuickSend: () -> Unit,
    onQuickAi: () -> Unit,
    onQuickControl: () -> Unit,
    onViewAllActivity: () -> Unit,
    onOpenDeviceDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = status == "CONNECTED"
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Buenos días"
            hour < 19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting,
                color = DesignTokens.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isConnected) "Todo sincronizado." else "Buscando tu laptop...",
                color = DesignTokens.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Current-Device Hero Card (Dark technological styling)
        CurrentDeviceHero(
            status = status,
            latencyMs = latencyMs,
            serverUrl = serverUrl,
            onOpenDetail = onOpenDeviceDetail
        )

        // Quick Actions Grid (Exact 4: SYNC, SEND, AI, CONTROL)
        SectionHeader(title = "Acciones Rápidas")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction(
                label = "SYNC",
                icon = Icons.Outlined.Refresh,
                onClick = onSync,
                accentColor = DesignTokens.ElectricBlue,
                modifier = Modifier.weight(1f)
            )
            QuickAction(
                label = "SEND",
                icon = Icons.Outlined.NorthEast,
                onClick = onQuickSend,
                accentColor = DesignTokens.CyanGaze,
                modifier = Modifier.weight(1f)
            )
            QuickAction(
                label = "AI",
                icon = Icons.Outlined.AutoAwesome,
                onClick = onQuickAi,
                accentColor = DesignTokens.VioletAccent,
                modifier = Modifier.weight(1f)
            )
            QuickAction(
                label = "CONTROL",
                icon = Icons.Outlined.Tune,
                onClick = onQuickControl,
                accentColor = DesignTokens.StatusConnected,
                modifier = Modifier.weight(1f)
            )
        }

        // Real Telemetry Metrics (Only real metrics, zero fake values)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                label = "Latencia",
                value = if (isConnected && latencyMs != null) "$latencyMs" else "—",
                unit = if (isConnected && latencyMs != null) "ms" else "",
                icon = Icons.Outlined.Speed,
                accentColor = DesignTokens.CyanGaze,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "Batería Celular",
                value = if (batteryPct != null) "$batteryPct" else "—",
                unit = if (batteryPct != null) "%" else "",
                icon = Icons.Outlined.BatteryChargingFull,
                accentColor = DesignTokens.StatusConnected,
                modifier = Modifier.weight(1f)
            )
        }

        // Recent Real Activity
        SectionHeader(
            title = "Actividad Reciente",
            actionLabel = "Ver todo",
            onAction = onViewAllActivity
        )

        if (recentEvents.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeMedium,
                colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                border = BorderStroke(1.dp, DesignTokens.SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.SurfaceSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Sensors, contentDescription = null, tint = DesignTokens.TextMuted, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Esperando eventos...", color = DesignTokens.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Las sincronizaciones y comandos aparecerán aquí.", color = DesignTokens.TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeMedium,
                colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                border = BorderStroke(1.dp, DesignTokens.SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    recentEvents.take(3).forEach { event ->
                        val timeFormatted = remember(event.timestamp) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp))
                        }
                        val title = when (event.type) {
                            "SYNC_CLIPBOARD" -> "Portapapeles enviado"
                            "CLIPBOARD_RECEIVED_FROM_PC" -> "Portapapeles recibido de laptop"
                            "LOCK_SCREEN" -> "Bloqueo de pantalla solicitado"
                            "VOLUME_UP", "VOLUME_DOWN", "VOLUME_MUTE" -> "Ajuste de volumen en laptop"
                            "PRESENTATION_NEXT", "PRESENTATION_PREV" -> "Control de diapositivas"
                            "PING" -> "Ping de verificación"
                            "PONG" -> "Respuesta Pong recibida"
                            "sync_request" -> "Sincronización manual iniciada"
                            else -> event.type
                        }
                        ActivityItem(
                            title = title,
                            subtitle = event.payload.take(50),
                            timestamp = timeFormatted,
                            direction = event.direction.name,
                            status = event.status.name
                        )
                    }
                }
            }
        }

        // Primary Sync Trigger Button
        Button(
            onClick = onSync,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = DesignTokens.ShapeMedium,
            colors = ButtonDefaults.buttonColors(
                containerColor = DesignTokens.ElectricBlue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Sincronizar ahora", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))
    }
}

/**
 * Sleek Current-Device Hero Card.
 */
@Composable
private fun CurrentDeviceHero(
    status: String,
    latencyMs: Long?,
    serverUrl: String,
    onOpenDetail: () -> Unit
) {
    val isConnected = status == "CONNECTED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DesignTokens.ShapeLarge)
            .clickable(onClick = onOpenDetail),
        shape = DesignTokens.ShapeLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, DesignTokens.HeroBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DesignTokens.HeroGradient)
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = DesignTokens.ShapePill,
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Computer, contentDescription = null, tint = DesignTokens.CyanGaze, modifier = Modifier.size(14.dp))
                            Text("DISPOSITIVO PRINCIPAL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        }
                    }

                    StatusBadge(status = status)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "Laptop Windows" else "Buscando enlace...",
                            color = DesignTokens.TextOnDark,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = serverUrl,
                            color = DesignTokens.CyanGaze,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(DesignTokens.ShapeMedium)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), DesignTokens.ShapeMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        SyncEngineMark(size = 28.dp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isConnected && latencyMs != null) "Latencia: $latencyMs ms • Dual Protocol" else "Pulsa para ver detalles de conexión",
                        color = DesignTokens.TextOnDarkMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Detalles →",
                        color = DesignTokens.CyanGaze,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
