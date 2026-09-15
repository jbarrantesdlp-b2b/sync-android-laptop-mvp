package com.example.syncwidget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.example.core.model.ThemePack

data class GlancePalette(
    val background: ColorProvider,
    val surface: ColorProvider,
    val primary: ColorProvider,
    val text: ColorProvider,
    val muted: ColorProvider,
    val success: ColorProvider,
    val warning: ColorProvider,
    val danger: ColorProvider,
    val onPrimary: ColorProvider
)

object ThemeMapper {
    fun toGlancePalette(theme: ThemePack): GlancePalette {
        return GlancePalette(
            background = ColorProvider(theme.background),
            surface = ColorProvider(theme.surface),
            primary = ColorProvider(theme.primary),
            text = ColorProvider(theme.textPrimary),
            muted = ColorProvider(theme.textSecondary),
            success = ColorProvider(theme.success),
            warning = ColorProvider(theme.warning),
            danger = ColorProvider(theme.danger),
            onPrimary = ColorProvider(Color(0xFF021018))
        )
    }
}
