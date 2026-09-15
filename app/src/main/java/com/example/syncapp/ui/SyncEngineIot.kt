package com.example.syncapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.syncapp.iot.IotSnapshot

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SensorsPane(
    theme: ThemePack,
    snapshot: IotSnapshot,
    streaming: Boolean,
    connected: Boolean,
    remoteLabel: String?,
    onToggleStream: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Sensores IoT", color = theme.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            if (connected) "El telefono es el hub. Un ESP32 puede publicar en el mismo enlace."
            else "Conecta la laptop para reenviar telemetria.",
            color = theme.textSecondary,
            fontSize = 13.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SensorChip("Luz", fmt(snapshot.lightLux, " lx"), theme)
            SensorChip("Movimiento", fmt(snapshot.accelG, " g"), theme)
            SensorChip("Proximidad", fmt(snapshot.proximityCm, " cm"), theme)
            SensorChip("Bateria", snapshot.batteryPct?.let { "$it%${if (snapshot.charging) " +" else ""}" } ?: "--", theme)
            SensorChip("Pasos", snapshot.steps?.toString() ?: "--", theme)
            SensorChip("Temp", fmt(snapshot.tempC, " C"), theme)
            SensorChip("Humedad", fmt(snapshot.humidity, " %"), theme)
            SensorChip("Presion", fmt(snapshot.pressureHpa, " hPa"), theme)
        }
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GPS", color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (snapshot.lat != null && snapshot.lng != null) "${snapshot.lat} , ${snapshot.lng}"
                    else "Sin ubicacion. Concede permiso de ubicacion si quieres GPS.",
                    color = theme.textPrimary,
                    fontSize = 13.sp
                )
                Text(
                    "Disponibles: ${snapshot.available.joinToString(" · ").ifBlank { "ninguno" }}",
                    color = theme.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        if (!remoteLabel.isNullOrBlank()) {
            GlassCard(theme) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("NODO EXTERNO", color = Color(0xFF00BFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(remoteLabel, color = theme.textPrimary, fontSize = 13.sp)
                }
            }
        }
        Button(
            onClick = onToggleStream,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (streaming) Color(0xFF00BFFF) else theme.surfaceAlt,
                contentColor = if (streaming) Color(0xFF021018) else theme.textPrimary
            )
        ) {
            Text(if (streaming) "Transmitiendo al PC" else "Iniciar transmision", fontWeight = FontWeight.Bold)
        }
        GlassCard(theme) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ESP32 / Arduino", color = theme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    "POST http://IP-LAPTOP:8123/api/iot  JSON type=IOT_TELEMETRY. Tambien ws://IP:8123.",
                    color = theme.textPrimary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SensorChip(label: String, value: String, theme: ThemePack) {
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(18.dp)).background(theme.surface).border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp)).padding(14.dp)
    ) {
        Text(value, color = Color(0xFF00BFFF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = theme.textSecondary, fontSize = 11.sp)
    }
}

private fun fmt(v: Float?, unit: String): String =
    if (v == null) "--" else String.format(java.util.Locale.US, "%.1f%s", v, unit)
