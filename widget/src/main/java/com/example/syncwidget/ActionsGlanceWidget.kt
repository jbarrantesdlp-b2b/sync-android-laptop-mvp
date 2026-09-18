package com.example.syncwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * 2x2 Widget Acciones (Acceso Rápido) - Exact Match to media_1789632468509.jpg:
 * [🔄 Sync]       [↗ Enviar]
 * [✦ IA]         [🔒 Bloquear]
 */
@Suppress("RestrictedApi")
object ActionsGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_oled))
                    .cornerRadius(24.dp)
                    .padding(10.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ActionTile(
                        symbol = "🔄",
                        label = "Sync",
                        bgRes = R.drawable.widget_tile_bg,
                        symbolColor = Color(0xFF00BFFF),
                        modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<ManualSyncAction>())
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    ActionTile(
                        symbol = "↗",
                        label = "Enviar",
                        bgRes = R.drawable.widget_tile_bg,
                        symbolColor = Color.White,
                        modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openAppIntent("clipboard")))
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ActionTile(
                        symbol = "✦",
                        label = "IA",
                        bgRes = R.drawable.widget_tile_bg,
                        symbolColor = Color(0xFF38BDF8),
                        modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openAppIntent("ai")))
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    ActionTile(
                        symbol = "🔒",
                        label = "Bloquear",
                        bgRes = R.drawable.widget_tile_bg,
                        symbolColor = Color(0xFFEF4444),
                        modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<LockPcAction>())
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApi")
@Composable
private fun ActionTile(
    symbol: String,
    label: String,
    bgRes: Int,
    symbolColor: Color,
    modifier: GlanceModifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(bgRes))
            .cornerRadius(16.dp)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(symbolColor),
                textAlign = TextAlign.Center
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White),
                textAlign = TextAlign.Center
            )
        )
    }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionsGlanceWidget
}
