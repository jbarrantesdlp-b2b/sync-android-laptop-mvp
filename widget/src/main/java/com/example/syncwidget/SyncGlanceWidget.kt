package com.example.syncwidget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.core.datastore.PreferencesManager
import com.example.sync.SyncRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("RestrictedApi")
object SyncGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PreferencesManager(context)
        val connectionStatus = try {
            prefs.connectionStatusFlow.first()
        } catch (_: Exception) {
            "UNKNOWN"
        }

        val calendar = Calendar.getInstance()
        val timeString = SimpleDateFormat("HH:mm", Locale("es", "ES")).format(calendar.time)
        val dateFormatted = SimpleDateFormat("EEE, d MMM", Locale("es", "ES"))
            .format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        val connected = connectionStatus == "CONNECTED"
        val connecting = connectionStatus == "CONNECTING"
        val statusTitle = when {
            connected -> "Conectado"
            connecting -> "Conectando"
            else -> "Sin enlace"
        }
        val statusColor = when {
            connected -> Color(0xFF34D399)
            connecting -> Color(0xFFFBBF24)
            else -> Color(0xFFFB7185)
        }
        val badgeDrawable = when {
            connected -> R.drawable.widget_badge_green
            connecting -> R.drawable.widget_badge_yellow
            else -> R.drawable.widget_badge_red
        }

        val mainActivityIntent = Intent().apply {
            component = ComponentName("com.example.syncapp", "com.example.syncapp.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_dark_glass))
                    .cornerRadius(24.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity(mainActivityIntent)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_sync_mark),
                        contentDescription = "Sync Engine",
                        modifier = GlanceModifier.size(28.dp)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = "SYNC ENGINE",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF67E8F9))
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Text(
                        text = "\u21bb",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<ManualSyncAction>())
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = timeString,
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White),
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = dateFormatted,
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF94A3B8)),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(badgeDrawable),
                        contentDescription = statusTitle,
                        modifier = GlanceModifier.size(10.dp)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = statusTitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(statusColor)
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "Barrantes Co.",
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(Color(0xFF64748B))
                        )
                    )
                }
            }
        }
    }
}

class ManualSyncAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            SyncRepository(context).triggerManualSync()
        } catch (_: Exception) {
        }
    }
}

class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyncGlanceWidget
}
