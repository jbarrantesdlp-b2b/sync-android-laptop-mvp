package com.example.core.model

import androidx.compose.ui.graphics.Color

enum class ThemePack(
    val displayName: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color
) {
    SyncEngine(
        displayName = "Sync Engine",
        background = Color(0xFF0B0F19),
        surface = Color(0xFF151D2A),
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF8000FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF233044)
    ),
    Minimal(
        displayName = "Lumina Light",
        background = Color(0xFFEBF0F5),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFF1E293B),
        secondary = Color(0xFFF1F5F9),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFFE2E8F0)
    ),
    Neón(
        displayName = "Cyberpunk OLED",
        background = Color(0xFF000000),
        surface = Color(0xFF121218),
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF8000FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFF27272A)
    )
}
