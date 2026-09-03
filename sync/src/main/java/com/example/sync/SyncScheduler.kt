package com.example.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val SYNC_WORK_TAG = "sync_periodic_work"
    private const val SYNC_INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .addTag(SYNC_WORK_TAG)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(SYNC_WORK_TAG)
    }
}
