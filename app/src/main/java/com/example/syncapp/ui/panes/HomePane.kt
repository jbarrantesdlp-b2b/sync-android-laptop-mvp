package com.example.syncapp.ui.panes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.ThemePack
import com.example.sync.SyncRepository
import com.example.syncapp.R
import com.example.syncapp.ui.brand.SyncEngineMark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePane(
    status: String,
    latencyMs: Long?,
    serverUrl: String,
    repository: SyncRepository,
    currentTheme: ThemePack = ThemePack.NeoObsidian,
    onOpenDevices: () -> Unit = {},
    onOpenClipboard: () -> Unit = {},
    onOpenDrive: () -> Unit = {},
    onOpenAi: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
    onDirectConnect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = status == "CONNECTED"

    // Colors matching master canonical design (media_1789632468509.jpg)
    val bgDark = Color(0xFF050811)
    val cardBg = Color(0xFF0D1527).copy(alpha = 0.85f)
    val cardBorder = Color(0x3300BFFF)
    val textWhite = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF94A3B8)
    val textCyan = Color(0xFF00BFFF)
    val accentGreen = Color(0xFF10B981)

    var isSyncingModalVisible by remember { mutableStateOf(false) }

    // File picker to send file to laptop
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val fileName = queryFileName(context, uri) ?: "archivo_compartido"
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val success = repository.uploadFileToLaptop(fileName, bytes)
                        if (success) {
                            Toast.makeText(context, "Enviando '$fileName' a la Laptop...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error: Laptop no conectada", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al leer archivo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Energy pulse animation for central telemetry ring
    val infiniteTransition = rememberInfiniteTransition(label = "ringPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOP BAR: Hamburger [≡] | [🔔 with red dot] [Avatar Jose]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Menu,
                        contentDescription = "Menú",
                        tint = textWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notification Bell with red badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), CircleShape)
                            .clickable { onOpenSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = "Notificaciones",
                            tint = textWhite,
                            modifier = Modifier.size(19.dp)
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

                    // Avatar Jose (Official asset)
                    Image(
                        painter = painterResource(id = R.drawable.avatar_jose),
                        contentDescription = "Jose",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, textCyan, CircleShape)
                            .clickable { onOpenSettings() },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 2. GREETING HEADER: "Buenos días, Jose. Todo conectado, Listo para avanzar."
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Buenos días,",
                    color = textMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Jose.",
                    color = textWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isConnected) "Todo conectado, Listo para avanzar." else "Buscando tu laptop...",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 3. HERO CENTRAL TELEMETRY RING (Exact match to Screen 1 in master design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle ambient glow
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    textCyan.copy(alpha = if (isConnected) 0.25f else 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Neon orbit ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .rotate(ringRotation)
                        .border(
                            BorderStroke(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        textCyan.copy(alpha = 0.2f),
                                        textCyan,
                                        Color(0xFF38BDF8),
                                        Color.Transparent
                                    )
                                )
                            ),
                            CircleShape
                        )
                )

                // Central inner circle with glowing SO logo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF030712))
                        .border(1.dp, textCyan.copy(alpha = 0.4f), CircleShape)
                        .clickable { isSyncingModalVisible = true },
                    contentAlignment = Alignment.Center
                ) {
                    SyncEngineMark(size = 56.dp)
                }

                // Left Telemetry Badge: [⚡ 99.8% Sync]
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(accentGreen.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.ElectricBolt,
                                    contentDescription = null,
                                    tint = accentGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isConnected) "99.8%" else "--%",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sync",
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Right Telemetry Badge: [〰 12 ms Latencia]
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(textCyan.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Sensors,
                                    contentDescription = null,
                                    tint = textCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isConnected) "${latencyMs ?: 12} ms" else "--",
                            color = textWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Latencia",
                            color = textMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 4. CONNECTED DEVICE CARD: [Laptop preview | Xiaomi 2312 ● Conectado 🔋 78% >]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenDevices() }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Laptop thumbnail image
                    Image(
                        painter = painterResource(id = R.drawable.laptop_hero),
                        contentDescription = "Laptop",
                        modifier = Modifier
                            .width(54.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0x3300BFFF), RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Xiaomi 2312",
                            color = textWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) accentGreen else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (isConnected) "Conectado" else "Desconectado",
                                color = if (isConnected) accentGreen else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "· 78%",
                                color = textMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "Detalle",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 5. 4 QUICK ACTION BUTTONS: [Portapapeles] [Enviar] [IA] [Más]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPillButton(
                    icon = Icons.Outlined.ContentPaste,
                    label = "Portapapeles",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenClipboard
                )
                ActionPillButton(
                    icon = Icons.Outlined.NorthEast,
                    label = "Enviar",
                    modifier = Modifier.weight(1f),
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                )
                ActionPillButton(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "IA",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAi
                )
                ActionPillButton(
                    icon = Icons.Outlined.Lock,
                    label = "Más",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDevices
                )
            }

            // 6. ACTIVIDAD RECIENTE SECTION (Exact match to bottom of Screen 1)
            Text(
                text = "Actividad reciente",
                color = textWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .clickable { onOpenClipboard() }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0x3300BFFF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = textCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mensaje sincronizado",
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Hace 2 min · Xiaomi 2312",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Botón de sincronización manual para probar la pantalla animada Screen 2
            Button(
                onClick = {
                    isSyncingModalVisible = true
                    scope.launch {
                        repository.triggerManualSync()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textCyan,
                    contentColor = Color(0xFF021018)
                )
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sincronizar ahora", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))
        }

        // =========================================================================
        // SCREEN 2 OVERLAY: "Sincronizando..." (Exact Match to Screen 2 in master design)
        // =========================================================================
        AnimatedVisibility(
            visible = isSyncingModalVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0030712))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sincronizando...",
                            color = textWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mantén tus ideas en movimiento.",
                            color = textMuted,
                            fontSize = 13.sp
                        )
                    }

                    // Circular Orbit with Phone, SO Mark, and Laptop
                    Box(
                        modifier = Modifier
                            .size(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotating outer dashed cyan orbit
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .rotate(ringRotation)
                                .border(1.5.dp, textCyan.copy(alpha = 0.5f), CircleShape)
                        )

                        // Phone on left of orbit
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterStart)
                                .clip(CircleShape)
                                .background(Color(0xFF0D1527))
                                .border(1.dp, textCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Smartphone,
                                contentDescription = "Teléfono",
                                tint = textCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Center SO Mark
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF030712))
                                .border(1.5.dp, textCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SyncEngineMark(size = 38.dp)
                        }

                        // Laptop on right of orbit
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .align(Alignment.CenterEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF0D1527))
                                .border(1.dp, textCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Laptop,
                                contentDescription = "Laptop",
                                tint = textCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Central Sync Metric: 68%
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "68%",
                            color = textCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Sincronizando archivos",
                            color = textWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "12 de 18 elementos",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }

                    // Breakdown List (Fotos 8/8, Documentos 3/5, Portapapeles 1/1, Enlaces 0/4)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0D1527))
                            .border(1.dp, Color(0x2200BFFF), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SyncCategoryRow(Icons.Outlined.Image, "Fotos", "8/8", textCyan)
                        SyncCategoryRow(Icons.Outlined.Description, "Documentos", "3/5", textCyan)
                        SyncCategoryRow(Icons.Outlined.ContentPaste, "Portapapeles", "1/1", textCyan)
                        SyncCategoryRow(Icons.Outlined.Link, "Enlaces", "0/4", textMuted)
                    }

                    // Blue Action Button: Cancelar
                    Button(
                        onClick = { isSyncingModalVisible = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = textWhite
                        )
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1527))
            .border(1.dp, Color(0x2600BFFF), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1A00BFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF00BFFF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SyncCategoryRow(
    icon: ImageVector,
    title: String,
    ratio: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(ratio, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
    }
    return uri.path?.substringAfterLast('/')
}
