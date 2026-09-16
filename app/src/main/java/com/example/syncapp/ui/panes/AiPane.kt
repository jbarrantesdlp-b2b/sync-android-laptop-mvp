package com.example.syncapp.ui.panes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.ai.AiContextAction
import com.example.sync.ai.GeminiClient
import com.example.syncapp.ui.components.SectionHeader
import com.example.syncapp.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiPane(
    geminiClient: GeminiClient,
    initialContextText: String = "",
    onCopyResult: (String) -> Unit,
    onSendToPc: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var contextText by remember(initialContextText) { mutableStateOf(initialContextText) }
    var userQuery by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf<AiContextAction?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun runAction(action: AiContextAction) {
        if (contextText.isBlank()) {
            errorMessage = "Ingresa o pega un texto antes de ejecutar una acción IA."
            return
        }
        selectedAction = action
        isGenerating = true
        errorMessage = null
        resultText = null

        scope.launch {
            val res = geminiClient.executeAction(action, contextText, userQuery.ifBlank { null })
            isGenerating = false
            res.onSuccess { text ->
                resultText = text
            }.onFailure { err ->
                errorMessage = err.message ?: "Error al procesar con Gemini"
            }
        }
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
            Text("IA Contextual", color = DesignTokens.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Asistencia inteligente sobre tu contexto y portapapeles con Gemini.", color = DesignTokens.TextSecondary, fontSize = 13.sp)
        }

        // Context Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.ShapeMedium,
            colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
            border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Contenido de Trabajo",
                        color = DesignTokens.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (contextText.isNotBlank()) {
                        Text(
                            text = "Limpiar",
                            color = DesignTokens.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = contextText,
                    onValueChange = { contextText = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Pega un artículo, código, mensaje o resumen aquí...", color = DesignTokens.TextMuted, fontSize = 13.sp) },
                    shape = DesignTokens.ShapeSmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.ElectricBlue,
                        unfocusedBorderColor = DesignTokens.SurfaceBorder,
                        focusedContainerColor = DesignTokens.SurfaceSubtle,
                        unfocusedContainerColor = DesignTokens.SurfaceSubtle
                    )
                )

                OutlinedTextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Instrucción opcional (ej. 'En 3 puntos', 'A inglés')...", color = DesignTokens.TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    shape = DesignTokens.ShapeSmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.ElectricBlue,
                        unfocusedBorderColor = DesignTokens.SurfaceBorder,
                        focusedContainerColor = DesignTokens.SurfaceSubtle,
                        unfocusedContainerColor = DesignTokens.SurfaceSubtle
                    )
                )
            }
        }

        // 5 Contextual Actions (No generic chat)
        SectionHeader(title = "Acciones Contextuales")

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AiActionTile(
                title = "Resumir",
                icon = Icons.Outlined.AutoAwesome,
                isSelected = selectedAction == AiContextAction.SUMMARIZE,
                enabled = !isGenerating,
                onClick = { runAction(AiContextAction.SUMMARIZE) },
                modifier = Modifier.weight(1f)
            )
            AiActionTile(
                title = "Traducir",
                icon = Icons.Outlined.Translate,
                isSelected = selectedAction == AiContextAction.TRANSLATE,
                enabled = !isGenerating,
                onClick = { runAction(AiContextAction.TRANSLATE) },
                modifier = Modifier.weight(1f)
            )
            AiActionTile(
                title = "Corregir",
                icon = Icons.Outlined.Check,
                isSelected = selectedAction == AiContextAction.CORRECT,
                enabled = !isGenerating,
                onClick = { runAction(AiContextAction.CORRECT) },
                modifier = Modifier.weight(1f)
            )
            AiActionTile(
                title = "Acciones",
                icon = Icons.Outlined.ListAlt,
                isSelected = selectedAction == AiContextAction.EXTRACT_ACTIONS,
                enabled = !isGenerating,
                onClick = { runAction(AiContextAction.EXTRACT_ACTIONS) },
                modifier = Modifier.weight(1f)
            )
            AiActionTile(
                title = "Mejorar",
                icon = Icons.Outlined.Edit,
                isSelected = selectedAction == AiContextAction.IMPROVE,
                enabled = !isGenerating,
                onClick = { runAction(AiContextAction.IMPROVE) },
                modifier = Modifier.weight(1f)
            )
        }

        // Loading State
        if (isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeMedium,
                colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                border = BorderStroke(1.dp, DesignTokens.SurfaceBorder)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = DesignTokens.ElectricBlue
                    )
                    Column {
                        Text(
                            text = "Procesando con Gemini...",
                            color = DesignTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aplicando acción ${selectedAction?.displayName ?: ""}",
                            color = DesignTokens.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Error Banner
        if (errorMessage != null) {
            Surface(
                shape = DesignTokens.ShapeMedium,
                color = DesignTokens.StatusDisconnected.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, DesignTokens.StatusDisconnected.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = DesignTokens.StatusDisconnected,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // Result Card
        if (resultText != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.ShapeMedium,
                colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
                border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = DesignTokens.ShapePill,
                            color = DesignTokens.VioletAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, DesignTokens.VioletAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "RESULTADO IA",
                                color = DesignTokens.VioletAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onCopyResult(resultText ?: "") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = DesignTokens.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onSendToPc(resultText ?: "") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Send, contentDescription = "Enviar a laptop", tint = DesignTokens.ElectricBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = resultText ?: "",
                        color = DesignTokens.TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSendToPc(resultText ?: "") },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = DesignTokens.ShapeSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DesignTokens.ElectricBlue,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Enviar a Laptop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedAction?.let { runAction(it) } },
                            modifier = Modifier.height(42.dp),
                            shape = DesignTokens.ShapeSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DesignTokens.SurfaceSubtle,
                                contentColor = DesignTokens.TextPrimary
                            )
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Reintentar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun AiActionTile(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) DesignTokens.VioletAccent.copy(alpha = 0.12f) else DesignTokens.SurfaceWhite
    val border = if (isSelected) DesignTokens.VioletAccent else DesignTokens.SurfaceBorder
    val tint = if (isSelected) DesignTokens.VioletAccent else DesignTokens.TextPrimary

    Card(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
