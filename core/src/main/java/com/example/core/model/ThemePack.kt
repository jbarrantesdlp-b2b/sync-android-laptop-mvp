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
    NeoObsidian(
        displayName = "Neo-Obsidian (Base)",
        background = Color(0xFF05070B),
        surface = Color(0xFF0D1527),
        surfaceAlt = Color(0xFF111827),
        primary = Color(0xFF00BFFF),
        secondary = Color(0xFF7C3AED),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0x3300BFFF),
        success = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        danger = Color(0xFFEF4444)
    ),
    QuantumLumina(
        displayName = "Quantum Lumina",
        background = Color(0xFF030712),
        surface = Color(0xFF0B132B),
        surfaceAlt = Color(0xFF1C2541),
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF818CF8),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        border = Color(0x4038BDF8),
        success = Color(0xFF34D399),
        warning = Color(0xFFFBBF24),
        danger = Color(0xFFF87171)
    ),
    HyperBauhaus(
        displayName = "Hyper-Bauhaus",
        background = Color(0xFF000000),
        surface = Color(0xFF111111),
        surfaceAlt = Color(0xFF1A1A1A),
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFFFF3333),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCBD5E1),
        border = Color(0x4DFFFFFF),
        success = Color(0xFF00F0FF),
        warning = Color(0xFFF59E0B),
        danger = Color(0xFFFF3333)
    );

    companion object {
        val SyncEngine: ThemePack get() = NeoObsidian
        val Minimal: ThemePack get() = QuantumLumina
        val Neón: ThemePack get() = HyperBauhaus
        val NightGlass: ThemePack get() = QuantumLumina

        fun fromString(name: String?): ThemePack {
            return when (name) {
                "QuantumLumina", "Minimal", "NightGlass" -> QuantumLumina
                "HyperBauhaus", "Neón" -> HyperBauhaus
                else -> NeoObsidian
            }
        }
    }
}
