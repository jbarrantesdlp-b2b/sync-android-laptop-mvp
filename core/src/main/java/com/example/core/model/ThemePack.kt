package com.example.core.model

import androidx.compose.ui.graphics.Color

enum class ThemePack(
    val displayName: String,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val primary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
) {
    SyncEngine(
        displayName = "Sync Engine OLED",
        background = Color(0xFF050811),
        surface = Color(0xFF0C1220),
        surfaceAlt = Color(0xFF111827),
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF7C3AED),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF1E293B),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        danger = Color(0xFFEF4444)
    ),
    NightGlass(
        displayName = "Cristal nocturno",
        background = Color(0xFF020617),
        surface = Color(0xFF0F172A),
        surfaceAlt = Color(0xFF1E293B),
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0xFF334155),
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171)
    ),
    Minimal(
        displayName = "Lumina Light",
        background = Color(0xFFEBF0F5),
        surface = Color(0xFFFFFFFF),
        surfaceAlt = Color(0xFFF1F5F9),
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF6366F1),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFFE2E8F0),
        success = Color(0xFF16A34A),
        warning = Color(0xFFD97706),
        danger = Color(0xFFDC2626)
    ),
    Neón(
        displayName = "Cyberpunk OLED",
        background = Color(0xFF000000),
        surface = Color(0xFF121218),
        surfaceAlt = Color(0xFF1A1A22),
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF8000FF),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF64748B),
        border = Color(0xFF27272A),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        danger = Color(0xFFEF4444)
    )
}
