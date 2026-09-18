package com.example.syncapp.ui.panes

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sync.ai.AiContextAction
import com.example.sync.ai.GeminiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPane(
    geminiClient: GeminiClient,
    onCopyResult: (String) -> Unit = {},
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
    val orbBlue = Color(0xFF0284C7)

    var queryInput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var responseText by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pulsing Orb Animation
    val infiniteTransition = rememberInfiniteTransition(label = "orbGlow")
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    fun executeQuery(prompt: String, action: AiContextAction? = null) {
        if (prompt.isBlank()) return
        isGenerating = true
        errorMessage = null
        responseText = null

        scope.launch {
            val res = geminiClient.executeAction(action ?: AiContextAction.IMPROVE, prompt, null)
            isGenerating = false
            res.onSuccess { text ->
                responseText = text
            }.onFailure { err ->
                errorMessage = err.message ?: "No se pudo conectar con Gemini AI"
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
            // 1. TOP BAR: [IA Assist] | [⋮]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IA Assist",
                        color = textWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tu contexto, más inteligente.",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }

                IconButton(onClick = { Toast.makeText(context, "Ajustes de Gemini AI", Toast.LENGTH_SHORT).show() }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Opciones",
                        tint = textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. CENTRAL GLOWING BLUE ORB (Exact match to Screen 5)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radial outer atmospheric glow
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(orbScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    orbBlue.copy(alpha = 0.35f),
                                    textCyan.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // The glowing blue Orb
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(orbScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF67E8F9),
                                    Color(0xFF0284C7),
                                    Color(0xFF0369A1),
                                    Color(0xFF0B1220)
                                )
                            )
                        )
                        .border(1.dp, Color(0x6638BDF8), CircleShape)
                )
            }

            // 3. 2x2 ACTION CARDS: [Resumir] [Traducir] [Extraer acciones] [Mejorar texto]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AiActionCard(
                    icon = Icons.Outlined.Description,
                    title = "Resumir",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        executeQuery("Resume el contenido de forma concisa y ejecutiva.", AiContextAction.SUMMARIZE)
                    }
                )
                AiActionCard(
                    icon = Icons.Outlined.Translate,
                    title = "Traducir",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        executeQuery("Traduce el texto seleccionado al inglés con tono profesional.", AiContextAction.TRANSLATE)
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AiActionCard(
                    icon = Icons.Outlined.Checklist,
                    title = "Extraer acciones",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        executeQuery("Extrae las tareas y pendientes accionables en una lista clara con viñetas.", AiContextAction.EXTRACT_ACTIONS)
                    }
                )
                AiActionCard(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Mejorar texto",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        executeQuery("Mejora la redacción haciéndola más elegante, profesional y directa.", AiContextAction.IMPROVE)
                    }
                )
            }

            // 4. INPUT FIELD: "Pregunta algo..." with blue send button ➔
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryInput,
                        onValueChange = { queryInput = it },
                        placeholder = { Text("Pregunta algo...", color = textMuted, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .clickable {
                                if (queryInput.isNotBlank()) {
                                    val q = queryInput
                                    queryInput = ""
                                    executeQuery(q)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ArrowForward,
                            contentDescription = "Enviar",
                            tint = textWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Loading state or result
            if (isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = textCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Procesando con Gemini AI...", color = textCyan, fontSize = 12.sp)
                }
            }

            if (responseText != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0x3300BFFF), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Respuesta de IA Assist:", color = textCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(responseText!!, color = textWhite, fontSize = 13.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Text(errorMessage!!, color = Color(0xFFEF4444), fontSize = 12.sp)
            }

            // 5. SUGERENCIAS LIST (Exact match to Screen 5)
            Text(
                text = "Sugerencias",
                color = textWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiSuggestionRow(Icons.Outlined.Description, "Resume mi último documento") {
                    executeQuery("Resume mi último documento guardado.", AiContextAction.SUMMARIZE)
                }
                AiSuggestionRow(Icons.Outlined.Checklist, "Extrae tareas de esta nota") {
                    executeQuery("Extrae tareas de esta nota.", AiContextAction.EXTRACT_ACTIONS)
                }
                AiSuggestionRow(Icons.Outlined.Language, "Traduce este texto") {
                    executeQuery("Traduce este texto al español neutral.", AiContextAction.TRANSLATE)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AiActionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D1527))
            .border(1.dp, Color(0x2600BFFF), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF00BFFF),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AiSuggestionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D1527))
            .border(1.dp, Color(0x1A00BFFF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00BFFF),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
