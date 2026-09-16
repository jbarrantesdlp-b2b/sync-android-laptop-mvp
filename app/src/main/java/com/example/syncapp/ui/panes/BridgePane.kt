package com.example.syncapp.ui.panes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncMessage
import com.example.syncapp.ui.components.ClipboardCard
import com.example.syncapp.ui.components.EmptyState
import com.example.syncapp.ui.components.SearchField
import com.example.syncapp.ui.components.SectionHeader
import com.example.syncapp.ui.theme.DesignTokens

enum class BridgeTab(val title: String) {
    CLIPBOARD("Portapapeles"),
    FILES("Archivos"),
    FAVORITES("Favoritos")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgePane(
    clipboardHistory: List<SyncMessage>,
    onSendClipboard: (String) -> Unit,
    onCopyLocal: (String) -> Unit,
    onOpenUrlOnPc: (String) -> Unit,
    onSendToAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(BridgeTab.CLIPBOARD) }
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val isInputUrl = remember(inputText) {
        inputText.trim().startsWith("http://", ignoreCase = true) ||
            inputText.trim().startsWith("https://", ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Bridge", color = DesignTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Conexión fluida de datos entre móvil y laptop.", color = DesignTokens.TextSecondary, fontSize = 13.sp)
        }

        // Sub-tabs Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BridgeTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DesignTokens.ElectricBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DesignTokens.SurfaceWhite,
                        labelColor = DesignTokens.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) DesignTokens.ElectricBlue else DesignTokens.SurfaceBorder,
                        selectedBorderColor = DesignTokens.ElectricBlue
                    ),
                    shape = DesignTokens.ShapePill
                )
            }
        }

        when (selectedTab) {
            BridgeTab.CLIPBOARD -> {
                // Clipboard transmitter card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.ShapeMedium,
                    colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                    border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Transmitir al Dispositivo",
                            color = DesignTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth().height(110.dp),
                            placeholder = { Text("Pega un texto o URL para transferir al PC...", color = DesignTokens.TextMuted, fontSize = 13.sp) },
                            shape = DesignTokens.ShapeSmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DesignTokens.ElectricBlue,
                                unfocusedBorderColor = DesignTokens.SurfaceBorder,
                                focusedContainerColor = DesignTokens.SurfaceSubtle,
                                unfocusedContainerColor = DesignTokens.SurfaceSubtle
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        onSendClipboard(inputText.trim())
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = DesignTokens.ShapeSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DesignTokens.ElectricBlue,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Enviar al PC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (isInputUrl) {
                                Button(
                                    onClick = {
                                        onOpenUrlOnPc(inputText.trim())
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = DesignTokens.ShapeSmall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.CyanGaze,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Abrir en PC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (inputText.isNotBlank()) {
                                Button(
                                    onClick = {
                                        onSendToAi(inputText.trim())
                                    },
                                    modifier = Modifier.height(44.dp),
                                    shape = DesignTokens.ShapeSmall,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesignTokens.VioletAccent,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = "IA", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Search Bar
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Buscar en portapapeles histórico..."
                )

                // History List
                SectionHeader(title = "Historial de Bridge")

                val filteredHistory = clipboardHistory.filter {
                    it.type.contains("CLIPBOARD", ignoreCase = true) &&
                        (searchQuery.isBlank() || it.payload.contains(searchQuery, ignoreCase = true))
                }

                if (filteredHistory.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.ContentPaste,
                        title = "Sin elementos en Bridge",
                        description = "Los textos o enlaces que envíes o recibas aparecerán sincronizados aquí."
                    )
                } else {
                    filteredHistory.forEach { item ->
                        val cleanPayload = try {
                            val obj = org.json.JSONObject(item.payload)
                            obj.optString("text", item.payload)
                        } catch (_: Exception) {
                            item.payload
                        }
                        val isUrl = cleanPayload.startsWith("http://") || cleanPayload.startsWith("https://")
                        ClipboardCard(
                            text = cleanPayload,
                            isUrl = isUrl,
                            onCopy = { onCopyLocal(cleanPayload) },
                            onSend = { onSendClipboard(cleanPayload) },
                            onOpenUrl = if (isUrl) { { onOpenUrlOnPc(cleanPayload) } } else null,
                            onAi = { onSendToAi(cleanPayload) }
                        )
                    }
                }
            }

            BridgeTab.FILES -> {
                EmptyState(
                    icon = Icons.Outlined.Folder,
                    title = "Transferencia de Archivos",
                    description = "El motor de transferencia de archivos por bloques binarios se habilitará en la siguiente fase cuando el backend WebSocket soporte streams de archivos."
                )
            }

            BridgeTab.FAVORITES -> {
                EmptyState(
                    icon = Icons.Outlined.Star,
                    title = "Elementos Favoritos",
                    description = "Puedes marcar clips de texto o enlaces recurrentes como favoritos para tenerlos siempre a mano."
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}
