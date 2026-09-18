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

    // Surfaces & Backgrounds (Pure OLED Neo-Obsidian + Dark Glass)
    val CanvasBackground = Color(0xFF05070B)       // Neo-Obsidian Precision Base
    val SurfaceWhite = Color(0xFF0D1527)           // Dark glass / carbon surface
    val SurfaceSubtle = Color(0xFF131E36)          // Dark subtle container
    val SurfaceBorder = Color(0x26FFFFFF)          // 15% clean glass border
    val GlassPanel = Color(0x0DFFFFFF)             // Translucent glass layer

    // Dark Hero & Navigation
    val HeroDarkBg = Color(0xFF070C18)             // Deep tech obsidian
    val HeroDarkGradientStart = Color(0xFF0B172E)  // Dark carbon navy
    val HeroDarkGradientEnd = Color(0xFF020612)    // Deep black
    val HeroBorder = Color(0x3300BFFF)             // Plasma cyan subtle glow
    val NavigationDarkBg = Color(0xFF050811)       // Pure OLED navigation bar
    val NavigationIndicator = Color(0x2600BFFF)    // Plasma cyan soft indicator

    // Accents & Actions
    val ElectricBlue = Color(0xFF007AFF)           // Primary electric blue
    val CyanGaze = Color(0xFF00BFFF)               // Highlight cyan (Plasma)
    val VioletAccent = Color(0xFF7C3AED)           // Subtle secondary

    // Semantics
    val StatusConnected = Color(0xFF10B981)        // Emerald green
    val StatusConnecting = Color(0xFF00BFFF)       // Cyan connecting
    val StatusDisconnected = Color(0xFFEF4444)     // Coral red
    val StatusUnknown = Color(0xFF64748B)          // Slate gray

    // Text & Hierarchy (High Contrast OLED)
    val TextPrimary = Color(0xFFFFFFFF)            // Pure white on dark
    val TextSecondary = Color(0xFF94A3B8)          // Slate 400
    val TextMuted = Color(0xFF64748B)              // Slate 500
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
