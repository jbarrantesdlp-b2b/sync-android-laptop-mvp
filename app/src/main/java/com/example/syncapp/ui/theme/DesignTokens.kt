package com.example.syncapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for Sync Engine following Grok Build visual direction:
 * - Premium, modern, technological, clean, modular
 * - Cool blue/gray background with pure white surfaces
 * - Sleek dark hero card for current device
 * - Dark navigation bar with electric blue accent
 * - Semantic emerald for connected, amber for connecting, coral for disconnected
 */
object DesignTokens {

    // Surfaces & Backgrounds
    val CanvasBackground = Color(0xFFF4F6FA)       // Light cool blue/gray
    val SurfaceWhite = Color(0xFFFFFFFF)           // Pure white cards
    val SurfaceSubtle = Color(0xFFF8FAFC)          // Soft container
    val SurfaceBorder = Color(0xFFE2E8F0)          // Clean border

    // Dark Hero & Navigation
    val HeroDarkBg = Color(0xFF0B132B)             // Deep tech navy
    val HeroDarkGradientStart = Color(0xFF0F172A)  // Slate dark
    val HeroDarkGradientEnd = Color(0xFF020617)    // Obsidian dark
    val HeroBorder = Color(0xFF1E293B)
    val NavigationDarkBg = Color(0xFFFFFFFF)       // Clean white navigation bar
    val NavigationIndicator = Color.Transparent   // No dark pill indicator

    // Accents & Actions
    val ElectricBlue = Color(0xFF007AFF)           // Primary electric blue
    val CyanGaze = Color(0xFF00BFFF)               // Highlight cyan
    val VioletAccent = Color(0xFF7C3AED)           // Subtle secondary

    // Semantics
    val StatusConnected = Color(0xFF10B981)        // Emerald green
    val StatusConnecting = Color(0xFFF59E0B)       // Warm amber
    val StatusDisconnected = Color(0xFFEF4444)     // Coral red
    val StatusUnknown = Color(0xFF64748B)          // Slate gray

    // Text & Hierarchy
    val TextPrimary = Color(0xFF0F172A)            // Slate 900 on light
    val TextSecondary = Color(0xFF64748B)          // Slate 500 on light
    val TextMuted = Color(0xFF94A3B8)              // Slate 400
    val TextOnDark = Color(0xFFFFFFFF)             // White on dark hero
    val TextOnDarkMuted = Color(0xFF94A3B8)        // Soft muted on dark hero

    // Corner Radii
    val ShapeSmall = RoundedCornerShape(12.dp)
    val ShapeMedium = RoundedCornerShape(18.dp)
    val ShapeLarge = RoundedCornerShape(24.dp)
    val ShapeXLarge = RoundedCornerShape(32.dp)
    val ShapePill = RoundedCornerShape(999.dp)

    // Gradients
    val HeroGradient = Brush.linearGradient(
        listOf(HeroDarkGradientStart, HeroDarkBg, HeroDarkGradientEnd)
    )

    val ActionGradient = Brush.horizontalGradient(
        listOf(ElectricBlue, CyanGaze)
    )
}
