package com.example.syncapp.ui.panes

import android.os.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncMessage
import com.example.syncapp.R
import com.example.syncapp.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.*

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOPBAR: Hamburger menu, notification bell with badge, and Jose avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDeviceDetail) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = Color(0xFF0F172A))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bell with red dot
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }

                // Profile Avatar Jose
                Image(
                    painter = painterResource(id = R.drawable.avatar_jose),
                    contentDescription = "Jose Barrantes",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFCBD5E1), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // GREETING HEADER
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Buenos días,",
                color = Color(0xFF0F172A),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Jose.",
                color = Color(0xFF0F172A),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Todos tus dispositivos conectados y listos para avanzar.",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // HERO CARD: DISPOSITIVO ACTUAL (Matches exact mobile mockup)
        CurrentDeviceHero(
            status = status,
            latencyMs = latencyMs,
            serverUrl = serverUrl,
            onOpenDetail = onOpenDeviceDetail
        )

        // 3 METRIC CARDS ROW (Salud, Eventos, Archivos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Salud de sync
            SmallStatCard(
                title = "Salud de sync",
                value = "99.8%",
                symbol = "○",
                symbolColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )

            // Card 2: Eventos hoy
            SmallStatCard(
                title = "Eventos hoy",
                value = "24",
                symbol = "ıll",
                symbolColor = Color(0xFF007AFF),
                modifier = Modifier.weight(1f)
            )

            // Card 3: Archivos sync
            SmallStatCard(
                title = "Archivos sync",
                value = "1,842",
                symbol = "〰",
                symbolColor = Color(0xFF00BFFF),
                modifier = Modifier.weight(1f)
            )
        }

        // QUICK ACTIONS HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acciones rápidas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Ver todo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.clickable(onClick = onViewAllActivity)
            )
        }

        // 2x2 QUICK ACTIONS GRID (Matches exact right mockup)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 1: Enviar al portapapeles (Vibrant Blue Solid Card)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clickable(onClick = onQuickSend),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF007AFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Enviar al", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("portapapeles", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Action 2: Sincronizar ahora (White Card)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clickable(onClick = onSync),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Sincronizar", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("ahora", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action 3: Ask AI (White Card)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clickable(onClick = onQuickAi),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Ask AI", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Resumir / Analizar", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }

            // Action 4: Automatizaciones (White Card)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp)
                    .clickable(onClick = onQuickControl),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.GridView, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Automatizaciones", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Crear nueva regla", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        // RECENT ACTIVITY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Actividad reciente",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Ver todo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                modifier = Modifier.clickable(onClick = onViewAllActivity)
            )
        }

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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mensaje enviado al portapapeles", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Xiaomi 2312 · hace 2 min", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8))
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

/**
 * Current Device Hero Card matching exact mobile mockup
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
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenDetail),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF091226), Color(0xFF030712))
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left text info
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("DISPOSITIVO ACTUAL", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Text(
                            text = if (isConnected) "Conectado" else "Sin conexión",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Xiaomi 2312",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = "Windows 11 · Wi-Fi",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Text(
                        text = if (isConnected && latencyMs != null) "$latencyMs ms de latencia" else "12 ms de latencia",
                        color = Color(0xFF00BFFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right laptop image & Arrow action
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.laptop_hero),
                        contentDescription = "Laptop",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Small Stat Card with circular progress, mini bars, or curve
 */
@Composable
private fun SmallStatCard(
    title: String,
    value: String,
    symbol: String,
    symbolColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(value, color = Color(0xFF0F172A), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(symbol, color = symbolColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
