package com.example.syncapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Plasma = Brush.linearGradient(
    colors = listOf(
        Color(0xFF00F0FF),
        Color(0xFF00BFFF),
        Color(0xFF38BDF8),
        Color(0xFF8000FF)
    )
)

fun syncMarkPath(w: Float, h: Float): Path {
    val sx = w / 512f
    val sy = h / 512f
    return Path().apply {
        moveTo(190f * sx, 140f * sy)
        cubicTo(260f * sx, 140f * sy, 280f * sx, 180f * sy, 260f * sx, 220f * sy)
        lineTo(220f * sx, 300f * sy)
        cubicTo(190f * sx, 360f * sy, 260f * sx, 400f * sy, 320f * sx, 400f * sy)
        cubicTo(400f * sx, 400f * sy, 440f * sx, 320f * sy, 380f * sx, 240f * sy)
        cubicTo(320f * sx, 160f * sy, 220f * sx, 140f * sy, 190f * sx, 140f * sy)
        close()
    }
}

@Composable
fun SyncMark(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    stroke: Dp = 3.2.dp,
    glow: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val path = syncMarkPath(this.size.width, this.size.height)
        if (glow) {
            drawPath(
                path = path,
                brush = Plasma,
                style = Stroke(width = stroke.toPx() * 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                alpha = 0.28f
            )
        }
        drawPath(
            path = path,
            color = Color(0xFFCBD5E1),
            style = Stroke(width = stroke.toPx() * 1.35f, cap = StrokeCap.Square, join = StrokeJoin.Miter),
            alpha = 0.55f
        )
        drawPath(
            path = path,
            brush = Plasma,
            style = Stroke(width = stroke.toPx(), cap = StrokeCap.Square, join = StrokeJoin.Miter)
        )
        drawLine(
            color = Color.White,
            start = Offset(this.size.width * 0.37f, this.size.height * 0.27f),
            end = Offset(this.size.width * 0.50f, this.size.height * 0.27f),
            strokeWidth = stroke.toPx() * 0.35f,
            cap = StrokeCap.Round,
            alpha = 0.85f
        )
    }
}

@Composable
fun SyncWordmark(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1220)),
            contentAlignment = Alignment.Center
        ) {
            SyncMark(size = if (compact) 26.dp else 32.dp, stroke = 2.6.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "SYNC ENGINE",
                color = Color(0xFF00D4FF),
                fontSize = if (compact) 12.sp else 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp
            )
            Text(
                "BY BARRANTES CO.",
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
        }
    }
}
