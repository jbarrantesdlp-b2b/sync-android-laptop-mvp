package com.example.syncapp.ui.panes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncMessage
import com.example.syncapp.ui.components.ActivityItem
import com.example.syncapp.ui.components.EmptyState
import com.example.syncapp.ui.components.SearchField
import com.example.syncapp.ui.theme.DesignTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ActivityFilter(val label: String) {
    ALL("Todos"),
    CLIPBOARD("Portapapeles"),
    COMMANDS("Comandos"),
    SYSTEM("Sistema")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityPane(
    events: List<SyncMessage>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(ActivityFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEvents = remember(events, selectedFilter, searchQuery) {
        events.filter { event ->
            val matchesFilter = when (selectedFilter) {
                ActivityFilter.ALL -> true
                ActivityFilter.CLIPBOARD -> event.type.contains("CLIPBOARD", ignoreCase = true)
                ActivityFilter.COMMANDS -> event.type in setOf(
                    "LOCK_SCREEN", "VOLUME_UP", "VOLUME_DOWN", "VOLUME_MUTE",
                    "MEDIA_PLAY_PAUSE", "PRESENTATION_NEXT", "PRESENTATION_PREV",
                    "SHUTDOWN", "REBOOT", "ABORT_SHUTDOWN"
                )
                ActivityFilter.SYSTEM -> event.type in setOf("PING", "PONG", "sync_request", "CONNECTION_STATE")
            }
            val matchesSearch = searchQuery.isBlank() ||
                event.type.contains(searchQuery, ignoreCase = true) ||
                event.payload.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Actividad", color = DesignTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Registro limpio de eventos y sincronizaciones reales.", color = DesignTokens.TextSecondary, fontSize = 13.sp)
        }

        // Search Field
        SearchField(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = "Buscar en actividad..."
        )

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = filter.label,
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

        // Timeline List
        if (filteredEvents.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "Sin actividad",
                description = "No se encontraron eventos registrados en esta categoría."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    val timeFormatted = remember(event.timestamp) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))
                    }
                    val (title, subtitle) = when (event.type) {
                        "SYNC_CLIPBOARD" -> Pair("Portapapeles enviado", event.payload.take(60))
                        "CLIPBOARD_RECEIVED_FROM_PC" -> Pair("Portapapeles recibido", event.payload.take(60))
                        "LOCK_SCREEN" -> Pair("Comando Bloqueo", "Pantalla de laptop bloqueada")
                        "VOLUME_UP" -> Pair("Comando Volumen", "Subir volumen en PC")
                        "VOLUME_DOWN" -> Pair("Comando Volumen", "Bajar volumen en PC")
                        "VOLUME_MUTE" -> Pair("Comando Audio", "Silenciar audio en PC")
                        "MEDIA_PLAY_PAUSE" -> Pair("Comando Multimedia", "Reproducir / Pausar")
                        "PRESENTATION_NEXT" -> Pair("Presentación", "Diapositiva siguiente")
                        "PRESENTATION_PREV" -> Pair("Presentación", "Diapositiva anterior")
                        "PING" -> Pair("Ping", "Comprobación de enlace")
                        "PONG" -> Pair("Pong", "Respuesta recibida")
                        "sync_request" -> Pair("Sincronización manual", "Petición de enlace completada")
                        "SHUTDOWN" -> Pair("Sistema", "Apagado de laptop programado")
                        "REBOOT" -> Pair("Sistema", "Reinicio de laptop programado")
                        "ABORT_SHUTDOWN" -> Pair("Sistema", "Apagado cancelado")
                        else -> Pair(event.type, event.payload.take(60))
                    }

                    ActivityItem(
                        title = title,
                        subtitle = subtitle,
                        timestamp = timeFormatted,
                        direction = event.direction.name,
                        status = event.status.name
                    )
                }
                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
