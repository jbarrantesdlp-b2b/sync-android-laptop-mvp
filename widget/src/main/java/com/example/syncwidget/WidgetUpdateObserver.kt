package com.example.syncwidget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.core.datastore.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WidgetUpdateObserver(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun startObserving() {
        scope.launch {
            combine(
                prefs.connectionStatusFlow,
                prefs.themePackFlow
            ) { status, theme ->
                Pair(status, theme)
            }.collect {
                updateWidget()
            }
        }

        schedulePeriodicWidgetUpdates()
    }

    fun stopObserving() {
        scope.cancel()
    }

    private fun updateWidget() {
        scope.launch {
            try {
                SyncGlanceWidget.updateAll(context)
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
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
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
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
