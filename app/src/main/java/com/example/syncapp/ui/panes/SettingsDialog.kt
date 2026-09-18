package com.example.syncapp.ui.panes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.core.model.ThemePack
import com.example.syncapp.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    currentServerUrl: String,
    currentGeminiKey: String,
    currentTheme: ThemePack,
    technicalLogs: List<String>,
    onDismiss: () -> Unit,
    onSaveServerUrl: (String) -> Unit,
    onSaveGeminiKey: (String) -> Unit,
    onSelectTheme: (ThemePack) -> Unit,
    onOpenQrScanner: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var serverUrlInput by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }
    var geminiKeyInput by remember(currentGeminiKey) { mutableStateOf(currentGeminiKey) }
    var showTechLogs by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DesignTokens.SurfaceWhite,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ajustes de Configuración", color = DesignTokens.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = DesignTokens.TextSecondary)
                }
            }

            // Server URL & QR Scanner
            Text("VINCULACIÓN DE SERVIDOR", color = DesignTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = { serverUrlInput = it },
                label = { Text("URL WebSocket de laptop", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignTokens.ElectricBlue,
                    unfocusedBorderColor = DesignTokens.SurfaceBorder
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenQrScanner,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = DesignTokens.ShapeSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.CyanGaze,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Escanear QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onSaveServerUrl(serverUrlInput.trim()) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = DesignTokens.ShapeSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.ElectricBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Guardar y Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Gemini API Key
            Text("INTELIGENCIA ARTIFICIAL", color = DesignTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            OutlinedTextField(
                value = geminiKeyInput,
                onValueChange = { geminiKeyInput = it },
                label = { Text("Clave API de Google Gemini", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignTokens.ElectricBlue,
                    unfocusedBorderColor = DesignTokens.SurfaceBorder
                )
            )
            Button(
                onClick = { onSaveGeminiKey(geminiKeyInput.trim()) },
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = DesignTokens.ShapeSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignTokens.VioletAccent,
                    contentColor = Color.White
                )
            ) {
                Text("Guardar Clave Gemini", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Theme Selection (3 Diseños de Vanguardia)
            Text("DISEÑO Y TEMA DEL ECOSISTEMA", color = DesignTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePack.values().forEach { pack ->
                    val isSelected = pack == currentTheme
                    val icon = when (pack) {
                        ThemePack.NeoObsidian -> "🌌"
                        ThemePack.QuantumLumina -> "💎"
                        ThemePack.HyperBauhaus -> "⚡"
                    }
                    val subtitle = when (pack) {
                        ThemePack.NeoObsidian -> "Diseño #1 Base · Precisión de Élite (Raycast / Porsche)"
                        ThemePack.QuantumLumina -> "Alternativa · Vidrio Líquido Refractivo (VisionOS)"
                        ThemePack.HyperBauhaus -> "Alternativa · Cyber-Brutalismo Cinético (Nothing OS)"
                    }
                    Surface(
                        shape = DesignTokens.ShapeMedium,
                        color = if (isSelected) pack.surface else DesignTokens.SurfaceSubtle,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) pack.primary else DesignTokens.SurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(pack) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = icon, fontSize = 20.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.displayName,
                                    color = if (isSelected) Color.White else DesignTokens.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = subtitle,
                                    color = if (isSelected) pack.primary else DesignTokens.TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Text("● ACTIVO", color = pack.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Diagnostic Technical Logs
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("DIAGNÓSTICO TÉCNICO", color = DesignTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                Text(
                    text = if (showTechLogs) "Ocultar" else "Ver registros",
                    color = DesignTokens.ElectricBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showTechLogs = !showTechLogs }
                )
            }

            if (showTechLogs) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.ShapeSmall,
                    colors = CardDefaults.cardColors(containerColor = DesignTokens.HeroDarkBg),
                    border = BorderStroke(1.dp, DesignTokens.HeroBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (technicalLogs.isEmpty()) {
                            Text("Sin eventos técnicos.", color = DesignTokens.TextMuted, fontSize = 11.sp)
                        } else {
                            technicalLogs.takeLast(25).reversed().forEach { line ->
                                Text(line, color = DesignTokens.CyanGaze, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
