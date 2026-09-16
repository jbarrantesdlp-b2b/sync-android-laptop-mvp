package com.example.syncapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.syncapp.ui.brand.SyncEngineMark
import com.example.syncapp.ui.theme.DesignTokens

/**
 * Top App Bar with official Sync Engine Mark, connection status pill, and secondary settings trigger.
 */
@Composable
fun SyncEngineTopBar(
    status: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            SyncEngineMark(size = 32.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "SYNC ENGINE",
                    color = DesignTokens.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "BY BARRANTES CO.",
                    color = DesignTokens.TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        StatusBadge(status = status)
        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DesignTokens.SurfaceWhite)
                .border(1.dp, DesignTokens.SurfaceBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Ajustes",
                tint = DesignTokens.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Status badge with animated pulsing indicator dot.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when (status) {
        "CONNECTED" -> Pair(DesignTokens.StatusConnected, "Conectado")
        "CONNECTING" -> Pair(DesignTokens.StatusConnecting, "Enlazando")
        else -> Pair(DesignTokens.StatusDisconnected, "Sin enlace")
    }

    val isPulsing = status == "CONNECTING" || status == "CONNECTED"
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) }
    }

    Surface(
        modifier = modifier,
        shape = DesignTokens.ShapePill,
        color = statusColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .alpha(pulseAlpha)
                    .background(statusColor)
            )
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Connection indicator pill.
 */
@Composable
fun ConnectionIndicator(
    status: String,
    latencyMs: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusBadge(status = status)
        if (latencyMs != null && status == "CONNECTED") {
            Surface(
                shape = DesignTokens.ShapePill,
                color = DesignTokens.ElectricBlue.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, DesignTokens.ElectricBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "$latencyMs ms",
                    color = DesignTokens.ElectricBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Device Card showing real hardware information.
 */
@Composable
fun DeviceCard(
    name: String,
    type: String,
    status: String,
    ipAddress: String?,
    latencyMs: Long? = null,
    batteryPct: Int? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceAvatar(type = type, isConnected = status == "CONNECTED")
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        color = DesignTokens.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (status == "CONNECTED") "En línea" else "Desconectado",
                        color = if (status == "CONNECTED") DesignTokens.StatusConnected else DesignTokens.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!ipAddress.isNullOrBlank()) {
                        Text("•", color = DesignTokens.TextMuted, fontSize = 10.sp)
                        Text(
                            text = ipAddress,
                            color = DesignTokens.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (status == "CONNECTED" && latencyMs != null) {
                    Text(
                        text = "$latencyMs ms",
                        color = DesignTokens.CyanGaze,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (batteryPct != null) {
                    Text(
                        text = "$batteryPct%",
                        color = DesignTokens.TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Avatar icon for laptop, phone, or IoT node.
 */
@Composable
fun DeviceAvatar(
    type: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isConnected) DesignTokens.ElectricBlue.copy(alpha = 0.12f) else DesignTokens.SurfaceSubtle
    val iconColor = if (isConnected) DesignTokens.ElectricBlue else DesignTokens.TextMuted

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(DesignTokens.ShapeSmall)
            .background(bg)
            .border(1.dp, if (isConnected) DesignTokens.ElectricBlue.copy(alpha = 0.25f) else DesignTokens.SurfaceBorder, DesignTokens.ShapeSmall),
        contentAlignment = Alignment.Center
    ) {
        val icon = when (type.lowercase()) {
            "laptop", "pc", "desktop" -> Icons.Outlined.Settings
            "nodo", "iot", "esp32" -> Icons.Outlined.Sensors
            else -> Icons.Outlined.Smartphone
        }
        Icon(icon, contentDescription = type, tint = iconColor, modifier = Modifier.size(22.dp))
    }
}

/**
 * Quick Action tile.
 */
@Composable
fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = DesignTokens.ElectricBlue
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = DesignTokens.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Metric card displaying real measured data. Never fake.
 */
@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String = "",
    icon: ImageVector? = null,
    accentColor: Color = DesignTokens.ElectricBlue,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    color = DesignTokens.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = DesignTokens.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = unit,
                        color = DesignTokens.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean timeline item for real actions and sync events.
 */
@Composable
fun ActivityItem(
    title: String,
    subtitle: String,
    timestamp: String,
    direction: String = "INBOUND",
    status: String = "SUCCESS",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isOutbound = direction.uppercase() == "OUTBOUND"
        val icon = if (isOutbound) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward
        val tint = if (isOutbound) DesignTokens.ElectricBlue else DesignTokens.StatusConnected

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = direction, tint = tint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = DesignTokens.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = DesignTokens.TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = timestamp,
            color = DesignTokens.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Card for Clipboard items inside Bridge.
 */
@Composable
fun ClipboardCard(
    text: String,
    isUrl: Boolean,
    onCopy: () -> Unit,
    onSend: () -> Unit,
    onOpenUrl: (() -> Unit)? = null,
    onAi: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = DesignTokens.SurfaceWhite),
        border = BorderStroke(1.dp, DesignTokens.SurfaceBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = DesignTokens.ShapePill,
                    color = if (isUrl) DesignTokens.CyanGaze.copy(alpha = 0.12f) else DesignTokens.SurfaceSubtle,
                    border = BorderStroke(1.dp, if (isUrl) DesignTokens.CyanGaze.copy(alpha = 0.3f) else DesignTokens.SurfaceBorder)
                ) {
                    Text(
                        text = if (isUrl) "LINK" else "TEXTO",
                        color = if (isUrl) DesignTokens.ElectricBlue else DesignTokens.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar", tint = DesignTokens.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    if (isUrl && onOpenUrl != null) {
                        IconButton(onClick = onOpenUrl, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = "Abrir URL en PC", tint = DesignTokens.ElectricBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (onAi != null) {
                        IconButton(onClick = onAi, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.SyncAlt, contentDescription = "IA", tint = DesignTokens.VioletAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = text,
                color = DesignTokens.TextPrimary,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSend,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = DesignTokens.ShapeSmall,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignTokens.ElectricBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar a la Laptop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Control Button for hardware triggers.
 */
@Composable
fun ControlButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val containerColor = if (isDestructive) DesignTokens.StatusDisconnected.copy(alpha = 0.08f) else DesignTokens.SurfaceWhite
    val contentColor = if (isDestructive) DesignTokens.StatusDisconnected else DesignTokens.TextPrimary
    val borderColor = if (isDestructive) DesignTokens.StatusDisconnected.copy(alpha = 0.3f) else DesignTokens.SurfaceBorder

    Card(
        modifier = modifier
            .clip(DesignTokens.ShapeMedium)
            .clickable(enabled = enabled, onClick = onClick),
        shape = DesignTokens.ShapeMedium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) contentColor else DesignTokens.TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                color = if (enabled) contentColor else DesignTokens.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Section Header with optional action button.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = DesignTokens.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                color = DesignTokens.ElectricBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

/**
 * Search input field for Bridge & Activity.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Buscar...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = DesignTokens.TextMuted, fontSize = 13.sp) },
        singleLine = true,
        shape = DesignTokens.ShapeMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DesignTokens.ElectricBlue,
            unfocusedBorderColor = DesignTokens.SurfaceBorder,
            focusedContainerColor = DesignTokens.SurfaceWhite,
            unfocusedContainerColor = DesignTokens.SurfaceWhite
        )
    )
}

/**
 * Standard Empty State.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DesignTokens.SurfaceSubtle),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DesignTokens.TextMuted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            color = DesignTokens.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            color = DesignTokens.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Feedback banner for toast messages.
 */
@Composable
fun FeedbackToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = DesignTokens.ShapePill,
            color = DesignTokens.HeroDarkBg.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, DesignTokens.HeroBorder),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = DesignTokens.StatusConnected, modifier = Modifier.size(16.dp))
                Text(
                    text = message ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Bottom navigation item for the 5-tab bar.
 */
@Composable
fun RowScope.SyncNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp)) },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = DesignTokens.ElectricBlue,
            selectedTextColor = DesignTokens.ElectricBlue,
            unselectedIconColor = DesignTokens.TextMuted,
            unselectedTextColor = DesignTokens.TextMuted,
            indicatorColor = DesignTokens.NavigationIndicator
        )
    )
}
