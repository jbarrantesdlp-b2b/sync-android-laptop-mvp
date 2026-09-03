package com.example.syncwidget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.core.model.ThemePack

object ThemeMapper {
    fun toGlancePalette(theme: ThemePack): Map<String, ColorProvider> {
        return mapOf(
            "primary" to ColorProvider(Color(theme.primary.value.toInt())),
            "secondary" to ColorProvider(Color(theme.secondary.value.toInt())),
            "accent" to ColorProvider(Color(theme.accent.value.toInt()))
        )
    }
}
