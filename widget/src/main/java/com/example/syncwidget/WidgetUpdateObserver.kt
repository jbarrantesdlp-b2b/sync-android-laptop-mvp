package com.example.syncwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.core.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter

class WidgetUpdateObserver(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            refreshAll()
        }
    }
    private var isReceiverRegistered = false

    fun startObserving() {
        scope.launch {
            combine(
                prefs.connectionStatusFlow,
                prefs.themePackFlow
            ) { status, theme ->
                Pair(status, theme)
            }.collect {
                refreshAll()
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(timeTickReceiver, filter)
            isReceiverRegistered = true
        } catch (_: Exception) {}

        schedulePeriodicWidgetUpdates()
    }

    fun stopObserving() {
        scope.cancel()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(timeTickReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
    }

    private fun refreshAll() {
        scope.launch {
            try {
                SyncGlanceWidget.updateAll(context)
                ActionsGlanceWidget.updateAll(context)
                CompactGlanceWidget.updateAll(context)
            } catch (_: Exception) {
            }
        }
    }

    private fun schedulePeriodicWidgetUpdates() {
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        try {
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "widget_update_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    updateRequest
                )
        } catch (_: Exception) {
        }
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            SyncGlanceWidget.updateAll(applicationContext)
            ActionsGlanceWidget.updateAll(applicationContext)
            CompactGlanceWidget.updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
