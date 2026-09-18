package com.example.syncapp.ui.panes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncMessage
import com.example.sync.SyncRepository
import com.example.syncapp.R
import kotlinx.coroutines.launch

data class ClipboardDisplayItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val meta: String,
    val isFavorite: Boolean = false,
    val hasAiAction: Boolean = true,
    val isImage: Boolean = false,
    val fullContent: String
)

@Composable
fun ActivityPane(
    events: List<SyncMessage>,
    repository: SyncRepository,
    onSendToPc: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgDark = Color(0xFF050811)
    val textWhite = Color(0xFFFFFFFF)
    val textMuted = Color(0xFF94A3B8)
    val textCyan = Color(0xFF00BFFF)
    val cardBg = Color(0xFF0D1527)
    val cardBorder = Color(0x2800BFFF)
    val activeFilterBlue = Color(0xFF0284C7)

    var selectedFilter by remember { mutableStateOf("Reciente") }
    var showSendDialog by remember { mutableStateOf(false) }
    var sendTextInput by remember { mutableStateOf("") }

    // Built-in canonical items matching Screen 4 in master design
    val sampleItems = remember {
        listOf(
            ClipboardDisplayItem(
                id = "1",
                icon = Icons.Outlined.Link,
                title = "https://barrantes.co",
                meta = "Enlace · Hace 2 min",
                fullContent = "https://barrantes.co"
            ),
            ClipboardDisplayItem(
                id = "2",
                icon = Icons.Outlined.Description,
                title = "Propuesta Sync Engine...",
                meta = "Texto · Hace 12 min",
                fullContent = "Propuesta de arquitectura Sync Engine v1.2 con enlace bidireccional de baja latencia."
            ),
            ClipboardDisplayItem(
                id = "3",
                icon = Icons.Outlined.Image,
                title = "Captura de pantalla",
                meta = "Imagen · Hace 1 h",
                isImage = true,
                fullContent = "captura_pantalla.png"
            ),
            ClipboardDisplayItem(
                id = "4",
                icon = Icons.Outlined.FormatQuote,
                title = "\"Controlar. Conectar. Avanzar.\"",
                meta = "Texto · Hace 2 h",
                hasAiAction = false,
                fullContent = "Controlar. Conectar. Avanzar."
            ),
            ClipboardDisplayItem(
                id = "5",
                icon = Icons.Outlined.Description,
                title = "Ideas reunión",
                meta = "Texto · Hace 3 h",
                hasAiAction = false,
                fullContent = "1. Optimización de memoria en widgets. 2. Buffer circular de sincronización."
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP BAR: [★ Portapapeles] | [⏻ Power]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = "Favoritos",
                        tint = textCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Portapapeles",
                        color = textWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), CircleShape)
                        .clickable {
                            Toast.makeText(context, "Portapapeles enlazado", Toast.LENGTH_SHORT).show()
                        },
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

            // 2. FILTER PILLS: [Reciente] [Favoritos] [IA]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Reciente", "Favoritos", "IA").forEach { tab ->
                    val isSel = selectedFilter == tab
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) activeFilterBlue else Color(0xFF0D1527))
                            .border(1.dp, if (isSel) activeFilterBlue else Color(0x2200BFFF), RoundedCornerShape(10.dp))
                            .clickable { selectedFilter = tab }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = textWhite,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. CLIPBOARD ITEMS LIST (Exact match to Screen 4)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sampleItems) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Sync", item.fullContent))
                                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (item.isImage) {
                                Image(
                                    painter = painterResource(id = R.drawable.laptop_hero),
                                    contentDescription = "Imagen",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, Color(0x3300BFFF), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        tint = textCyan,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = textWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.meta,
                                    color = textMuted,
                                    fontSize = 11.sp
                                )
                            }

                            if (item.hasAiAction) {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = "IA",
                                    tint = textCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. FLOATING BLUE BUTTON: "Enviar al dispositivo  ➔"
            Button(
                onClick = { showSendDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = textWhite
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Enviar al dispositivo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Quick Send Dialog
        if (showSendDialog) {
            AlertDialog(
                onDismissRequest = { showSendDialog = false },
                title = { Text("Enviar texto a PC", color = textWhite, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = sendTextInput,
                        onValueChange = { sendTextInput = it },
                        placeholder = { Text("Escribe o pega texto para enviar...", color = textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = textCyan,
                            unfocusedBorderColor = cardBorder
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (sendTextInput.isNotBlank()) {
                                scope.launch {
                                    repository.syncClipboard(sendTextInput)
                                    Toast.makeText(context, "Texto enviado a la Laptop", Toast.LENGTH_SHORT).show()
                                    sendTextInput = ""
                                    showSendDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Enviar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSendDialog = false }) {
                        Text("Cancelar", color = textMuted)
                    }
                },
                containerColor = Color(0xFF0D1527)
            )
        }
    }
}
