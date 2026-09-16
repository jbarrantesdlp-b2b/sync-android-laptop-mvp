package com.example.syncapp.ui.panes

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NavigateBefore
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeDown
import androidx.compose.material.icons.outlined.VolumeMute
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncapp.iot.IotSnapshot
import com.example.syncapp.ui.components.ControlButton
import com.example.syncapp.ui.components.DeviceCard
import com.example.syncapp.ui.components.SectionHeader
import com.example.syncapp.ui.components.StatusBadge
import com.example.syncapp.ui.theme.DesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevicesPane(
    status: String,
    serverUrl: String,
    latencyMs: Long?,
    remoteNodeLabel: String?,
    iotSnapshot: IotSnapshot,
    isStreamingIot: Boolean,
    onToggleStreamIot: () -> Unit,
    onLock: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeMute: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onPresentationNext: () -> Unit,
    onPresentationPrev: () -> Unit,
    onPing: () -> Unit,
    onPower: () -> Unit,
    onReboot: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = status == "CONNECTED"
    var showSensorsDetail by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section title
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Dispositivos", color = DesignTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Nodos conectados en la red local Sync Engine.", color = DesignTokens.TextSecondary, fontSize = 13.sp)
        }

        // 1. Device List: Real available nodes
        SectionHeader(title = "Nodos en la Red")

        // Laptop / PC (Remote Node)
        DeviceCard(
            name = "Laptop Windows",
            type = "laptop",
            status = status,
            ipAddress = serverUrl.replace("ws://", "").replace(":8123", ""),
            latencyMs = latencyMs
        )

        // This Smartphone (Local Node)
        DeviceCard(
            name = Build.MODEL ?: "Este teléfono",
            type = "phone",
            status = "CONNECTED",
            ipAddress = "Nodo Local",
            batteryPct = iotSnapshot.batteryPct,
            onClick = { showSensorsDetail = !showSensorsDetail }
        )

        // Remote IoT Nodes if present
        if (!remoteNodeLabel.isNullOrBlank()) {
            DeviceCard(
                name = "Nodo Externo: $remoteNodeLabel",
                type = "iot",
                status = "CONNECTED",
                ipAddress = "ESP32 / Telemetría activa"
            )
        }

        // 2. Hardware Remote Controls for Connected Laptop
        SectionHeader(title = "Controles Remotos de Laptop")

        if (!isConnected) {
            Surface(
                shape = DesignTokens.ShapeMedium,
                color = DesignTokens.StatusConnecting.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, DesignTokens.StatusConnecting.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = DesignTokens.StatusConnecting, modifier = Modifier.size(20.dp))
                    Text(
                        "Conecta tu laptop para habilitar los controles de hardware.",
                        color = DesignTokens.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Grid of Real Controls
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val buttonModifier = Modifier.width(100.dp)

            ControlButton(
                label = "Bloquear",
                icon = Icons.Outlined.Lock,
                onClick = onLock,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Silenciar",
                icon = Icons.Outlined.VolumeMute,
                onClick = onVolumeMute,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Volumen +",
                icon = Icons.Outlined.VolumeUp,
                onClick = onVolumeUp,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Volumen -",
                icon = Icons.Outlined.VolumeDown,
                onClick = onVolumeDown,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Play / Pause",
                icon = Icons.Outlined.PlayArrow,
                onClick = onMediaPlayPause,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Siguiente",
                icon = Icons.Outlined.NavigateNext,
                onClick = onPresentationNext,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Anterior",
                icon = Icons.Outlined.NavigateBefore,
                onClick = onPresentationPrev,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Ping",
                icon = Icons.Outlined.Speed,
                onClick = onPing,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Reiniciar",
                icon = Icons.Outlined.Refresh,
                onClick = onReboot,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Apagar",
                icon = Icons.Outlined.PowerSettingsNew,
                onClick = onPower,
                isDestructive = true,
                enabled = isConnected,
                modifier = buttonModifier
            )
            ControlButton(
                label = "Abortar",
                icon = Icons.Outlined.Cancel,
                onClick = onAbort,
                enabled = isConnected,
                modifier = buttonModifier
            )
        }

        // 3. Contextual IoT Integration (Device Detail -> Sensors)
        SectionHeader(
            title = "Sensores del Dispositivo (IoT)",
            actionLabel = if (showSensorsDetail) "Ocultar" else "Ver telemetría",
            onAction = { showSensorsDetail = !showSensorsDetail }
        )

        AnimatedVisibility(visible = showSensorsDetail) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeMedium,
                colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                border = BorderStroke(1.dp, DesignTokens.SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Sensores Físicos Disponibles",
                        color = DesignTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Solo se muestran sensores físicos reales detectados en este hardware.",
                        color = DesignTokens.TextSecondary,
                        fontSize = 11.sp
                    )

                    // Real Sensors Grid
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iotSnapshot.lightLux?.let { SensorChip("Luz", String.format(java.util.Locale.US, "%.1f lx", it)) }
                        iotSnapshot.accelG?.let { SensorChip("Acelerómetro", String.format(java.util.Locale.US, "%.2f g", it)) }
                        iotSnapshot.proximityCm?.let { SensorChip("Proximidad", String.format(java.util.Locale.US, "%.1f cm", it)) }
                        iotSnapshot.batteryPct?.let { SensorChip("Batería", "$it%${if (iotSnapshot.charging) " +" else ""}") }
                        iotSnapshot.steps?.let { SensorChip("Pasos", it.toString()) }
                        iotSnapshot.tempC?.let { SensorChip("Temperatura", String.format(java.util.Locale.US, "%.1f °C", it)) }
                        iotSnapshot.humidity?.let { SensorChip("Humedad", String.format(java.util.Locale.US, "%.1f %%", it)) }
                        iotSnapshot.pressureHpa?.let { SensorChip("Presión", String.format(java.util.Locale.US, "%.1f hPa", it)) }
                        if (iotSnapshot.lat != null && iotSnapshot.lng != null) {
                            SensorChip("GPS", String.format(java.util.Locale.US, "%.3f, %.3f", iotSnapshot.lat, iotSnapshot.lng))
                        }
                    }

                    if (iotSnapshot.available.isEmpty()) {
                        Text("No se detectaron sensores activos adicionales.", color = DesignTokens.TextMuted, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = onToggleStreamIot,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = DesignTokens.ShapeSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreamingIot) DesignTokens.StatusConnected else DesignTokens.ElectricBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isStreamingIot) "Transmitiendo telemetría al PC" else "Iniciar transmisión de sensores",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = DesignTokens.ShapeSmall,
                        color = DesignTokens.SurfaceSubtle,
                        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Integración ESP32 / Arduino externa:", color = DesignTokens.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("POST http://IP-LAPTOP:8123/api/iot con JSON IOT_TELEMETRY.", color = DesignTokens.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SensorChip(label: String, value: String) {
    Surface(
        shape = DesignTokens.ShapeSmall,
        color = DesignTokens.SurfaceSubtle,
        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(value, color = DesignTokens.ElectricBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = DesignTokens.TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}
