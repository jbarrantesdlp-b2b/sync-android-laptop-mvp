package com.example.syncapp.ui.panes

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.RemoteDirResponse
import com.example.sync.RemoteFileItem
import com.example.sync.SyncRepository
import com.example.syncapp.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteDrivePane(
    repository: SyncRepository,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPath by remember { mutableStateOf("") }
    var dirResponse by remember { mutableStateOf<RemoteDirResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadingFile by remember { mutableStateOf<String?>(null) }

    fun loadPath(path: String) {
        if (!isConnected) {
            errorMessage = "Conecta tu laptop para explorar su disco duro."
            return
        }
        isLoading = true
        errorMessage = null
        scope.launch {
            val res = repository.fetchRemoteDirectory(path)
            isLoading = false
            if (res != null) {
                dirResponse = res
                currentPath = res.currentPath
            } else {
                errorMessage = "No se pudo leer la carpeta de la laptop."
            }
        }
    }

    LaunchedEffect(isConnected) {
        if (isConnected && dirResponse == null) {
            loadPath("")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP HEADER: Title + Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Disco Duro PC",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (isConnected) "Exploración directa de archivos locales" else "Laptop desconectada",
                    fontSize = 12.sp,
                    color = if (isConnected) DesignTokens.CyanGaze else Color(0xFFEF4444)
                )
            }

            IconButton(
                onClick = { loadPath(currentPath) },
                enabled = isConnected && !isLoading
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Recargar",
                    tint = if (isConnected) DesignTokens.CyanGaze else Color(0xFF64748B)
                )
            }
        }

        // QUICK SHORTCUT CHIPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Descargas" to "Downloads", "Documentos" to "Documents", "Escritorio" to "Desktop").forEach { (label, folder) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF0D1527))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(999.dp))
                        .clickable(enabled = isConnected) {
                            val home = dirResponse?.homeDir ?: ""
                            val target = if (home.isNotBlank()) "$home\\$folder" else folder
                            loadPath(target)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // CURRENT PATH BAR & GO UP
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
            border = BorderStroke(1.dp, Color(0x26FFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val parent = dirResponse?.parentPath
                        if (!parent.isNullOrBlank() && parent != currentPath) {
                            loadPath(parent)
                        }
                    },
                    enabled = isConnected && dirResponse?.parentPath != null && dirResponse?.parentPath != currentPath,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Subir nivel",
                        tint = if (dirResponse?.parentPath != null) DesignTokens.CyanGaze else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = currentPath.ifBlank { "Directorio raíz de PC" },
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // CONTENT AREA: LOADING / ERROR / LIST
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DesignTokens.CyanGaze)
                }
            }

            !isConnected -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Laptop no enlazada en la red local",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Abre SYNC ENGINE en tu PC para navegar sus carpetas.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage ?: "", color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }

            dirResponse?.items.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Carpeta vacía en la laptop", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }

            else -> {
                val items = dirResponse?.items ?: emptyList()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items, key = { it.path }) { item ->
                        RemoteFileRow(
                            item = item,
                            isDownloading = downloadingFile == item.path,
                            onClick = {
                                if (item.isDirectory) {
                                    loadPath(item.path)
                                } else {
                                    // Download file
                                    downloadingFile = item.path
                                    scope.launch {
                                        val destDir = context.getExternalFilesDir(null) ?: context.filesDir
                                        val destFile = File(destDir, item.name)
                                        val success = repository.downloadRemoteFile(item.path, destFile)
                                        downloadingFile = null
                                        if (success) {
                                            Toast.makeText(context, "Descargado: ${item.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Error descargando archivo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteFileRow(
    item: RemoteFileItem,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527)),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (item.isDirectory) Color(0x2600BFFF) else Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = if (item.isDirectory) DesignTokens.CyanGaze else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Carpeta" else formatFileSize(item.sizeBytes),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DesignTokens.CyanGaze)
            } else if (!item.isDirectory) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Descargar",
                    tint = DesignTokens.CyanGaze,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
