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
        displayName = "Sync Engine OLED",
        background = Color(0xFF050811),
        surface = Color(0xFF0D1320),
        primary = Color(0xFF00D4FF),
        secondary = Color(0xFF7C3AED),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF1E293B)
    ),
    Minimal(
        displayName = "Lumina Light",
        background = Color(0xFFEBF0F5),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF6366F1),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFFE2E8F0)
    ),
    Neón(
        displayName = "Cyberpunk OLED",
        background = Color(0xFF000000),
        surface = Color(0xFF121218),
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFF8000FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFF27272A)
    )
}
