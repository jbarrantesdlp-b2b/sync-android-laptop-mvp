package com.example.syncwidget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.core.model.ThemePack

object ThemeMapper {
    fun toGlancePalette(theme: ThemePack): Map<String, ColorProvider> {
        return mapOf(
            "primary" to ColorProvider(theme.primary),
            "secondary" to ColorProvider(theme.secondary),
            "accent" to ColorProvider(theme.primary)
        )
    }
}
