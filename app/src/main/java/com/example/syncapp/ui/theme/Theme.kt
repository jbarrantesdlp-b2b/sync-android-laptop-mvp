package com.example.syncapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.core.model.ThemePack

@Composable
fun SyncTheme(
    pack: ThemePack = ThemePack.SyncEngine,
    content: @Composable () -> Unit
) {
    val dark = pack != ThemePack.Minimal
    val scheme = if (dark) {
        darkColorScheme(
            primary = pack.primary,
            onPrimary = Color(0xFF041018),
            secondary = pack.secondary,
            onSecondary = Color.White,
            background = pack.background,
            onBackground = pack.textPrimary,
            surface = pack.surface,
            onSurface = pack.textPrimary,
            surfaceVariant = Color(0xFF151D2A),
            onSurfaceVariant = pack.textSecondary,
            outline = pack.border,
            error = Color(0xFFFB7185)
        )
    } else {
        lightColorScheme(
            primary = pack.primary,
            onPrimary = Color.White,
            secondary = pack.secondary,
            background = pack.background,
            onBackground = pack.textPrimary,
            surface = pack.surface,
            onSurface = pack.textPrimary,
            outline = pack.border
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
