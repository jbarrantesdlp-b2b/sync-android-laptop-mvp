package com.example.core.model

import androidx.compose.ui.graphics.Color

enum class ThemePack(val primary: Color, val secondary: Color, val accent: Color) {
    Minimal(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFFBB86FC)),
    Neón(Color(0xFF00FFAB), Color(0xFF00D1FF), Color(0xFFFF00FF)),
    Bancario(Color(0xFF0B3D91), Color(0xFF1E88E5), Color(0xFFB0BEC5))
}
