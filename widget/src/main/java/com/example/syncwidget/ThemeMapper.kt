package com.example.syncwidget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.core.model.ThemePack

object ThemeMapper {
    fun toGlancePalette(theme: ThemePack): Map<String, ColorProvider> {
        return mapOf(
            "primary" to ColorProvider(theme.primary),
            "secondary" to ColorProvider(theme.secondary),
            "accent" to ColorProvider(theme.primary),
            "background" to ColorProvider(theme.background),
            "surface" to ColorProvider(theme.surface),
            "text" to ColorProvider(theme.textPrimary),
            "muted" to ColorProvider(theme.textSecondary)
        )
    }

    fun statusColor(status: String): Color = when (status) {
        "CONNECTED" -> Color(0xFF34D399)
        "CONNECTING" -> Color(0xFFFBBF24)
        else -> Color(0xFFFB7185)
    }
}
