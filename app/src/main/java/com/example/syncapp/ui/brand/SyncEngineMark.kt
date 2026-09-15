package com.example.syncapp.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SyncEngineMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    glow: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val sx = w / 108f
            val sy = h / 108f
            val path = Path().apply {
                moveTo(38f * sx, 28f * sy)
                cubicTo(58f * sx, 28f * sy, 62f * sx, 38f * sy, 58f * sx, 46f * sy)
                lineTo(48f * sx, 64f * sy)
                cubicTo(40f * sx, 78f * sy, 56f * sx, 86f * sy, 70f * sx, 86f * sy)
                cubicTo(88f * sx, 86f * sy, 96f * sx, 68f * sy, 82f * sx, 50f * sy)
                cubicTo(70f * sx, 32f * sy, 50f * sx, 28f * sy, 38f * sx, 28f * sy)
            }
            val plasma = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00F0FF),
                    Color(0xFF00BFFF),
                    Color(0xFF38BDF8),
                    Color(0xFF8000FF)
                ),
                start = Offset(w * 0.25f, h * 0.2f),
                end = Offset(w * 0.85f, h * 0.85f)
            )
            val metal = Brush.linearGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFF94A3B8),
                    Color(0xFF334155),
                    Color(0xFFCBD5E1)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            val metalWidth = w * 0.12f
            val coreWidth = w * 0.065f
            if (glow) {
                drawPath(
                    path = path,
                    brush = plasma,
                    style = Stroke(width = coreWidth * 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    alpha = 0.28f
                )
            }
            drawPath(
                path = path,
                brush = metal,
                style = Stroke(width = metalWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter)
            )
            drawPath(
                path = path,
                brush = plasma,
                style = Stroke(width = coreWidth, cap = StrokeCap.Square, join = StrokeJoin.Miter)
            )
        }
    }
}
