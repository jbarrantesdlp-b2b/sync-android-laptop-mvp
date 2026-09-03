package com.example.syncwidget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.example.core.datastore.PreferencesManager
import com.example.sync.SyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class WidgetUpdateObserver(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val repository = SyncRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun startObserving() {
        // Listen to connection status changes
        scope.launch {
            prefs.connectionStatusFlow
                .debounce(300)
                .collect {
                    updateWidget()
                }
        }

        // Listen to message flow changes
        scope.launch {
            repository.messageFlow
                .debounce(500)
                .collect {
                    updateWidget()
                }
        }

        // Listen to theme changes
        scope.launch {
            prefs.themePackFlow
                .debounce(300)
                .collect {
                    updateWidget()
                }
        }

        // Combined observation for multiple changes
        scope.launch {
            combine(
                prefs.connectionStatusFlow,
                prefs.themePackFlow,
                repository.messageFlow
            ) { status, theme, _ ->
                Pair(status, theme)
            }
                .debounce(500)
                .collect {
                    updateWidget()
                }
        }

        // Schedule periodic widget updates every 5 minutes
        schedulePeriodicWidgetUpdates()
    }

    fun stopObserving() {
        scope.cancel()
    }

    private fun updateWidget() {
        scope.launch {
            SyncGlanceWidget.updateAll(context)
        }
    }

    private fun schedulePeriodicWidgetUpdates() {
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            5, TimeUnit.MINUTES
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
        } catch (e: Exception) {
            // WorkManager may not be available in all contexts
        }
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            runBlocking {
                SyncGlanceWidget.updateAll(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
