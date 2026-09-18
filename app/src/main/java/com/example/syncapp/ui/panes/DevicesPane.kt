package com.example.syncapp.ui.panes

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.SyncRepository
import com.example.syncapp.R
import kotlinx.coroutines.launch

@Composable
fun DevicesPane(
    status: String,
    repository: SyncRepository,
    onBack: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = status == "CONNECTED"

    val bgDark = Color(0xFF050811)
    val textWhite = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF94A3B8)
    val textCyan = Color(0xFF00BFFF)
    val cardBg = Color(0xFF0D1527)
    val cardBorder = Color(0x3300BFFF)
    val activeBlue = Color(0xFF007AFF)

    var selectedSegment by remember { mutableStateOf("Control") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        repository.uploadFileToLaptop(name, stream.readBytes())
                        Toast.makeText(context, "Enviando $name...", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
            // 1. TOP BAR: [< Control de dispositivo] | [⏻ Power]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Volver",
                            tint = textWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Control de dispositivo",
                            color = textWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Xiaomi 2312",
                                color = textCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Text(
                                text = if (isConnected) "Conectado" else "Desconectado",
                                color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            repository.lockScreen()
                            Toast.makeText(context, "Comando enviado", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = textWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. SEGMENTED FILTER PILLS: [Control] [Archivos] [Portapapeles] [Info]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Control", "Archivos", "Portapapeles", "Info").forEach { seg ->
                    val isSel = selectedSegment == seg
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) activeBlue else Color(0xFF0D1527))
                            .border(1.dp, if (isSel) activeBlue else Color(0x2200BFFF), RoundedCornerShape(10.dp))
                            .clickable {
                                selectedSegment = seg
                                when (seg) {
                                    "Archivos" -> onNavigateTab("files")
                                    "Portapapeles" -> onNavigateTab("clipboard")
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = seg,
                            color = textWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. LAPTOP VISUAL BANNER (Matches Screen 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0A152E), Color(0xFF050811), Color(0xFF1E1435))
                        )
                    )
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.laptop_hero),
                    contentDescription = "Laptop Render",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // 4. 3x3 CONTROL GRID (Matches Screen 3 with exact actions)
            // Row 1: [Bloquear (active blue)] [Reiniciar] [Apagar]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlGridTile(
                    icon = Icons.Outlined.Lock,
                    label = "Bloquear",
                    isActive = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.lockScreen()
                            Toast.makeText(context, "Pantalla PC bloqueada", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.Refresh,
                    label = "Reiniciar",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.rebootPc()
                            Toast.makeText(context, "Reiniciando PC...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.PowerSettingsNew,
                    label = "Apagar",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.shutdownPc()
                            Toast.makeText(context, "Apagando PC...", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Row 2: [Volumen] [Presentación] [Captura]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlGridTile(
                    icon = Icons.Outlined.VolumeUp,
                    label = "Volumen",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.adjustVolume("VOLUME_UP")
                            Toast.makeText(context, "Volumen PC +", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.Tv,
                    label = "Presentación",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.presentationNext()
                            Toast.makeText(context, "Siguiente diapositiva", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.CropFree,
                    label = "Captura",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.sendMessage("TRIGGER_SCREENSHOT", "{}")
                            Toast.makeText(context, "Captura solicitada", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Row 3: [Multimedia] [Enviar archivo] [Más]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlGridTile(
                    icon = Icons.Outlined.PlayArrow,
                    label = "Multimedia",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            repository.sendMessage("MEDIA_PLAY_PAUSE", "{}")
                            Toast.makeText(context, "Play / Pause", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.UploadFile,
                    label = "Enviar archivo",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    }
                )
                ControlGridTile(
                    icon = Icons.Outlined.MoreHoriz,
                    label = "Más",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        Toast.makeText(context, "Opciones de dispositivo", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ControlGridTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (isActive) Color(0xFF007AFF) else Color(0xFF0D1527)
    val border = if (isActive) Color(0xFF007AFF) else Color(0x2200BFFF)

    Box(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
