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
import androidx.glance.layout.defaultWeight
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
                        "\u21bb",
                        "Sync",
                        GlanceModifier.defaultWeight().clickable(actionRunCallback<ManualSyncAction>())
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    ActionTile(
                        "\u2191",
                        "Enviar",
                        GlanceModifier.defaultWeight().clickable(actionStartActivity(openAppIntent("clipboard")))
                    )
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ActionTile(
                        "\uD83D\uDD12",
                        "Bloquear",
                        GlanceModifier.defaultWeight().clickable(actionRunCallback<LockPcAction>())
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    ActionTile(
                        "\u2726",
                        "IA",
                        GlanceModifier.defaultWeight().clickable(actionStartActivity(openAppIntent("ai")))
                    )
                }
            }
        }
    }
}

@Suppress("RestrictedApi")
@Composable
private fun ActionTile(symbol: String, label: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_tile_bg))
            .cornerRadius(18.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            style = TextStyle(
                fontSize = 18.sp,
                color = ColorProvider(Color(0xFF00BFFF)),
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color(0xFFE2E8F0)),
                textAlign = TextAlign.Center
            )
        )
    }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionsGlanceWidget
}
