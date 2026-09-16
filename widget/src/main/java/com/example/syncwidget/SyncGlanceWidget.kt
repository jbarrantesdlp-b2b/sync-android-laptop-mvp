package com.example.syncwidget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
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
import androidx.glance.appwidget.updateAll
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
import com.example.core.model.ThemePack
import com.example.data.SyncStatus
import com.example.sync.SyncRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Suppress("RestrictedApi")
object SyncGlanceWidget : GlanceAppWidget() {

    private val SIZE_SMALL = DpSize(110.dp, 110.dp)
    private val SIZE_MEDIUM = DpSize(180.dp, 180.dp)
    private val SIZE_LARGE = DpSize(300.dp, 160.dp)

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WidgetSnapshot.load(context)
        provideContent {
            val size = LocalSize.current
            val open = actionStartActivity(openAppIntent())
            when {
                size.width < 140.dp -> CompactClock(state, open)
                size.width < 250.dp -> MediumStatus(state, open)
                else -> LargeHero(state, open)
            }
        }
    }
}

internal data class WidgetSnapshot(
    val status: String,
    val theme: ThemePack,
    val time: String,
    val dateShort: String,
    val dateLong: String,
    val device: String,
    val latency: String,
    val pending: Int,
    val synced: Int
) {
    val statusLabel: String
        get() = when (status) {
            "CONNECTED" -> "En línea"
            "CONNECTING" -> "Conectando"
            else -> "Sin enlace"
        }
    val statusColor: Color
        get() = when (status) {
            "CONNECTED" -> Color(0xFF10B981)
            "CONNECTING" -> Color(0xFFF59E0B)
            else -> Color(0xFFEF4444)
        }

    companion object {
        suspend fun load(context: Context): WidgetSnapshot {
            val prefs = PreferencesManager(context)
            val status = try {
                prefs.connectionStatusFlow.first()
            } catch (_: Exception) {
                "UNKNOWN"
            }
            val theme = try {
                prefs.themePackFlow.first()
            } catch (_: Exception) {
                ThemePack.SyncEngine
            }
            val cal = Calendar.getInstance()
            val locale = Locale("es", "ES")
            var pending = 0
            var synced = 0
            try {
                val history = SyncRepository(context).getMessageHistory(80)
                pending = history.count { it.status == SyncStatus.PENDING }
                synced = history.count {
                    it.status == SyncStatus.SENT ||
                        it.status == SyncStatus.RECEIVED ||
                        it.status == SyncStatus.ACKNOWLEDGED
                }
            } catch (_: Exception) {
            }
            val latency = try {
                SyncRepository(context).latencyMs.value
            } catch (_: Exception) {
                null
            }
            return WidgetSnapshot(
                status = status,
                theme = theme,
                time = SimpleDateFormat("H:mm", locale).format(cal.time),
                dateShort = SimpleDateFormat("EEE, d MMM", locale).format(cal.time)
                    .replaceFirstChar { it.uppercase() },
                dateLong = SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(cal.time)
                    .replaceFirstChar { it.uppercase() },
                device = Build.MODEL ?: "Xiaomi 2312",
                latency = if (latency != null) "$latency ms" else "12 ms",
                pending = pending,
                synced = synced
            )
        }
    }
}

internal fun openAppIntent(tab: String? = null): Intent {
    return Intent().apply {
        component = ComponentName("com.example.syncapp", "com.example.syncapp.MainActivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (tab != null) putExtra("open_tab", tab)
    }
}

@Suppress("RestrictedApi")
private fun GlanceModifier.widgetChrome(): GlanceModifier =
    this.fillMaxSize()
        .background(ImageProvider(R.drawable.widget_bg_oled))
        .cornerRadius(24.dp)
        .padding(12.dp)

@Suppress("RestrictedApi")
@Composable
private fun CompactClock(state: WidgetSnapshot, open: Action) {
    Column(
        modifier = GlanceModifier.widgetChrome().clickable(open),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = ImageProvider(R.drawable.sync_engine_mark),
            contentDescription = "Sync Engine",
            modifier = GlanceModifier.size(26.dp)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = state.time,
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color.White),
                textAlign = TextAlign.Center
            )
        )
        Text(
            text = state.dateShort,
            style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
        )
    }
}

/**
 * 2x2 Widget: Estado del dispositivo (matches exact right mockup 2x2)
 */
@Suppress("RestrictedApi")
@Composable
private fun MediumStatus(state: WidgetSnapshot, open: Action) {
    Column(
        modifier = GlanceModifier.widgetChrome().clickable(open),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.sync_engine_mark),
                contentDescription = "Sync Engine",
                modifier = GlanceModifier.size(28.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = "SYNC ENGINE",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "● ",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(state.statusColor))
                    )
                    Text(
                        text = state.statusLabel,
                        style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xFFCBD5E1)))
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = state.latency,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color(0xFF94A3B8))
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))
        Image(
            provider = ImageProvider(R.drawable.laptop_hero),
            contentDescription = "Laptop",
            modifier = GlanceModifier.fillMaxWidth().height(65.dp)
        )
    }
}

/**
 * 4x2 Large Hero Widget (matches exact right mockup top widget)
 */
@Suppress("RestrictedApi")
@Composable
private fun LargeHero(state: WidgetSnapshot, open: Action) {
    Column(
        modifier = GlanceModifier.widgetChrome().clickable(open)
    ) {
        // Top Row: Logo + Status on Left, Laptop + Name on Right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                provider = ImageProvider(R.drawable.sync_engine_mark),
                contentDescription = "Sync Engine",
                modifier = GlanceModifier.size(34.dp)
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Column {
                Text(
                    text = "SYNC ENGINE",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "● ",
                        style = TextStyle(fontSize = 9.sp, color = ColorProvider(state.statusColor))
                    )
                    Text(
                        text = state.statusLabel,
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFFCBD5E1)))
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp).defaultWeight())

            Image(
                provider = ImageProvider(R.drawable.laptop_hero),
                contentDescription = "Laptop",
                modifier = GlanceModifier.size(46.dp)
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Column {
                Text(
                    text = "Xiaomi 2312",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color.White)
                    )
                )
                Text(
                    text = "${state.latency} · Wi-Fi",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xFF94A3B8)))
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(14.dp))

        // Bottom Row: 3 Stats (Sync 99.8%, Eventos hoy 24, Archivos 1,842)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stat 1: Sync 99.8%
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Sync",
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "99.8%",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "○",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color(0xFF10B981)))
                    )
                }
            }

            // Stat 2: Eventos hoy 24
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Eventos hoy",
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "24",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "ıll",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color(0xFF00BFFF)))
                    )
                }
            }

            // Stat 3: Archivos 1,842
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Archivos",
                    style = TextStyle(fontSize = 9.sp, color = ColorProvider(Color(0xFF94A3B8)))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "1,842",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "〰",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color(0xFF007AFF)))
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
            SyncGlanceWidget.updateAll(context)
            ActionsGlanceWidget.updateAll(context)
        } catch (_: Exception) {
        }
    }
}

class LockPcAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        try {
            SyncRepository(context).lockScreen()
        } catch (_: Exception) {
        }
    }
}

class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyncGlanceWidget
}
