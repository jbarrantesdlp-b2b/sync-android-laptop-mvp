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

        val serverUrl = try {
            prefs.serverUrlFlow.first()
        } catch (_: Exception) {
            "ws://192.168.1.54:8123"
        }

        // Time and Date Formatting
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "ES"))
        val timeString = timeFormat.format(calendar.time)

        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale("es", "ES"))
        val dateFormatted = dateFormat.format(calendar.time).uppercase()

        // Status mapping matching reference Image 1
        val (statusTitle, statusSub, badgeDrawable, statusColor) = when (connectionStatus) {
            "CONNECTED" -> Quadruple("CONECTADA", "En vivo", R.drawable.widget_badge_green, Color(0xFF10B981))
            "CONNECTING" -> Quadruple("CONECTANDO", "Buscando...", R.drawable.widget_badge_yellow, Color(0xFFF59E0B))
            else -> Quadruple("DESCONECTADA", "Sin enlace", R.drawable.widget_badge_red, Color(0xFFEF4444))
        }

        val mainActivityIntent = Intent().apply {
            component = ComponentName("com.example.syncapp", "com.example.syncapp.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_transparent_glass))
                    .cornerRadius(20.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity(mainActivityIntent)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row: Title + Refresh Icon in Top-Right Corner
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNC ENGINE",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF94A3B8)),
                            textAlign = TextAlign.Start
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )

                    // ↻ Refresh Action Button
                    Text(
                        text = "↻",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White),
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<ManualSyncAction>())
                    )
                }

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Large Thin Digital Clock (Image 1 Style: 09:53)
                Text(
                    text = timeString,
                    style = TextStyle(
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Normal,
                        color = ColorProvider(Color.White),
                        textAlign = TextAlign.Center
                    )
                )

                // UPPERCASE Date (Image 1 Style: VIERNES, MARZO 26)
                Text(
                    text = dateFormatted,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFFE2E8F0)),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom Status Row (Image 1 Style: Location/Info + Badge + Stat)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Left Column: PC Host Info
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Laptop PC",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color.White)
                            )
                        )
                        Text(
                            text = if (serverUrl.length > 20) "Port 8123" else serverUrl.replace("ws://", ""),
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = ColorProvider(Color(0xFFCBD5E1))
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Center Badge Circle
                    Row(
                        modifier = GlanceModifier
                            .background(ImageProvider(badgeDrawable))
                            .padding(6.dp)
                    ) {}

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Right Column: Status & Subtitle
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = statusTitle,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(statusColor)
                            )
                        )
                        Text(
                            text = statusSub,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                color = ColorProvider(Color(0xFFCBD5E1))
                            )
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

class ManualSyncAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            val repository = SyncRepository(context)
            repository.triggerManualSync()
        } catch (_: Exception) {
            // Sync trigger handled
        }
    }
}

class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyncGlanceWidget
}
