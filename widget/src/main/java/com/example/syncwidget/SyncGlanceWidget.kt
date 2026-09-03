package com.example.syncwidget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionStartActivity
import androidx.glance.Button
import androidx.glance.Column
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.Row
import androidx.glance.Spacer
import androidx.glance.Text
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Arrangement
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dp
import androidx.glance.unit.TextUnit
import com.example.core.datastore.PreferencesManager
import com.example.sync.SyncRepository
import com.example.syncapp.MainActivity
import kotlinx.coroutines.flow.first

object SyncGlanceWidget : GlanceAppWidget(sizeMode = SizeMode.Responsive) {

    override suspend fun provideContent(context: Context, id: GlanceId) {
        provideContent {
            val ctx = LocalContext.current
            val prefs = PreferencesManager(ctx)
            val repository = SyncRepository(ctx)

            val connectionStatus = try {
                prefs.connectionStatusFlow.first()
            } catch (e: Exception) {
                "UNKNOWN"
            }

            val statusIcon = when (connectionStatus) {
                "CONNECTED" -> "🟢"
                "CONNECTING" -> "🟡"
                "DISCONNECTED" -> "🔴"
                else -> "⚪"
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(0xFFFAFAFA.toInt())
                    .padding(Dp(12f)),
                verticalAlignment = Alignment.Top
            ) {
                // Header
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = Dp(8f)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📱 Sync Status",
                        style = TextStyle(
                            fontSize = TextUnit.Sp(16),
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(android.graphics.Color.valueOf(0xFF212121))
                        )
                    )
                }

                // Status Box
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(0xFFEEEEEE.toInt())
                        .padding(Dp(8f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dp(8f))
                ) {
                    Text(
                        text = statusIcon,
                        style = TextStyle(fontSize = TextUnit.Sp(20))
                    )
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = connectionStatus.replace("_", " "),
                            style = TextStyle(
                                fontSize = TextUnit.Sp(13),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Last sync: now",
                            style = TextStyle(fontSize = TextUnit.Sp(10))
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(Dp(8f)))

                // Action Buttons
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dp(6f))
                ) {
                    Button(
                        text = "📊 Open",
                        onClick = actionStartActivity(
                            Intent(ctx, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        ),
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(Dp(4f))
                    )
                    Button(
                        text = "🔄 Sync",
                        onClick = actionRunCallback<ManualSyncAction>(),
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(Dp(4f))
                    )
                }

                Spacer(modifier = GlanceModifier.height(Dp(6f)))

                // Stats Row
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(0xFFF5F5F5.toInt())
                        .padding(Dp(6f)),
                    horizontalArrangement = Arrangement.spacedBy(Dp(8f))
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                fontSize = TextUnit.Sp(14),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Pending",
                            style = TextStyle(fontSize = TextUnit.Sp(9))
                        )
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "0",
                            style = TextStyle(
                                fontSize = TextUnit.Sp(14),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Synced",
                            style = TextStyle(fontSize = TextUnit.Sp(9))
                        )
                    }
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "15min",
                            style = TextStyle(
                                fontSize = TextUnit.Sp(14),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Next sync",
                            style = TextStyle(fontSize = TextUnit.Sp(9))
                        )
                    }
                }
            }
        }
    }
}

class ManualSyncAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId) {
        try {
            val repository = SyncRepository(context)
            repository.triggerManualSync()
        } catch (e: Exception) {
            // Sync trigger failed silently
        }
    }
}

class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyncGlanceWidget
}
