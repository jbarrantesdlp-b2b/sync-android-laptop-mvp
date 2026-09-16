package com.example.syncapp.ui.panes

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncapp.R
import com.example.syncapp.iot.IotSnapshot
import com.example.syncapp.ui.components.ControlButton
import com.example.syncapp.ui.components.SectionHeader
import com.example.syncapp.ui.theme.DesignTokens

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
    var selectedFilter by remember { mutableStateOf("Todos") }
    var showRemoteControls by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER: "Dispositivos" + Blue Circle '+' Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dispositivos",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .clickable { showRemoteControls = !showRemoteControls },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Añadir / Opciones", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // FILTER PILLS: Todos, Windows, Android, Apple
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Windows", "Android", "Apple").forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) Color(0xFF0F172A) else Color.White)
                        .border(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }

        // DEVICES LIST (Exact Match to Mockup)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Device 1: Xiaomi 2312 (Laptop)
            DeviceRowCard(
                name = "Xiaomi 2312",
                details = "Windows 11 · 12 ms",
                isConnected = isConnected,
                imageRes = R.drawable.laptop_hero,
                onClick = { showRemoteControls = !showRemoteControls }
            )

            // Device 2: Galaxy S24 (Android)
            DeviceRowCard(
                name = "Galaxy S24",
                details = "Android 14 · 28 ms",
                isConnected = true,
                iconVector = Icons.Outlined.Smartphone,
                onClick = { }
            )

            // Device 3: iPad Pro (Apple)
            DeviceRowCard(
                name = "iPad Pro",
                details = "iPadOS 17 · Hace 3 h",
                isConnected = false,
                iconVector = Icons.Outlined.TabletMac,
                onClick = { }
            )

            // Device 4: Oficina - Desktop
            DeviceRowCard(
                name = "Oficina - Desktop",
                details = "Windows 11 · 16 ms",
                isConnected = true,
                iconVector = Icons.Outlined.Computer,
                onClick = { }
            )
        }

        // PROMO CARD: Tu ecosistema más inteligente
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tu ecosistema, más inteligente", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Configura reglas, sincronizaciones y automatizaciones con IA.", color = Color(0xFF64748B), fontSize = 11.sp)
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
            }
        }

        // USAGE SECTION: 816 MB este mes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Uso de sincronización", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("816 MB este mes", color = Color(0xFF0F172A), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)

                // Segmented Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    Box(modifier = Modifier.weight(0.42f).fillMaxHeight().background(Color(0xFF007AFF)))
                    Box(modifier = Modifier.weight(0.28f).fillMaxHeight().background(Color(0xFF00BFFF)))
                    Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFF8B5CF6)))
                    Box(modifier = Modifier.weight(0.10f).fillMaxHeight().background(Color(0xFF64748B)))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("● Documentos 42%", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("● Imágenes 28%", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("● Videos 20%", color = Color(0xFF64748B), fontSize = 10.sp)
                    Text("● Otros 10%", color = Color(0xFF64748B), fontSize = 10.sp)
                }
            }
        }

        // QUICK REMOTE ACTIONS FOR PC (Expandable / Direct Controls)
        AnimatedVisibility(visible = showRemoteControls || isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF091226)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Control Remoto de Laptop", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onLock,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🔒 Bloquear", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = onVolumeMute,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🔇 Mute", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = onPresentationNext,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Siguiente →", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun DeviceRowCard(
    name: String,
    details: String,
    isConnected: Boolean,
    imageRes: Int? = null,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Device Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFEEF2F6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRes != null) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = name,
                        modifier = Modifier.size(38.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (iconVector != null) {
                    Icon(iconVector, contentDescription = name, tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                }
            }

            // Center details
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFF94A3B8))
                    )
                    Text(
                        text = if (isConnected) "Conectado" else "Sin conexión",
                        color = if (isConnected) Color(0xFF10B981) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("·", color = Color(0xFFCBD5E1))
                    Text(details, color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }

            // Right Chevron Action
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}
