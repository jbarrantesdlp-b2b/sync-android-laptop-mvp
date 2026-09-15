package com.example.syncwidget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

@Suppress("RestrictedApi")
object ActionsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_dark_glass))
                    .cornerRadius(24.dp)
                    .padding(10.dp)
            ) {
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ActionCell("\u21bb", "Sync", GlanceModifier.defaultWeight().clickable(actionRunCallback<ManualSyncAction>()))
                    Spacer(GlanceModifier.padding(4.dp))
                    ActionCell("\u2191", "Enviar", GlanceModifier.defaultWeight().clickable(openTab("activity")))
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    ActionCell("\uD83D\uDD12", "Bloquear", GlanceModifier.defaultWeight().clickable(openTab("control")))
                    Spacer(GlanceModifier.padding(4.dp))
                    ActionCell("\u2726", "IA", GlanceModifier.defaultWeight().clickable(openTab("ai")))
                }
            }
        }
    }

    private fun openTab(tab: String) = actionStartActivity(
        Intent().apply {
            component = ComponentName("com.example.syncapp", "com.example.syncapp.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", tab)
        }
    )
}

@Suppress("RestrictedApi")
@androidx.compose.runtime.Composable
private fun ActionCell(symbol: String, label: String, modifier: GlanceModifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_tile_bg))
            .cornerRadius(16.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = symbol,
            style = TextStyle(
                fontSize = 18.sp,
                color = ColorProvider(Color.White),
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF94A3B8)),
                textAlign = TextAlign.Center
            )
        )
    }
}

class ActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionsGlanceWidget
}
