package com.example.syncapp

import android.app.Application
import com.example.core.datastore.PreferencesManager
import com.example.sync.ConnectivityObserver
import com.example.sync.SyncRepository
import com.example.sync.SyncScheduler
import com.example.sync.SyncTrigger
import com.example.syncwidget.WidgetUpdateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncApp : Application() {

    companion object {
        lateinit var instance: SyncApp
            private set
    }

    lateinit var prefs: PreferencesManager
        private set
    lateinit var repository: SyncRepository
        private set

    private var widgetObserver: WidgetUpdateObserver? = null
    private var syncTrigger: SyncTrigger? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        prefs = PreferencesManager(this)

        // Reset connection status on startup to ensure accurate state
        CoroutineScope(Dispatchers.IO).launch {
            prefs.setConnectionStatus("DISCONNECTED")
        }

        repository = SyncRepository(this, prefs)

        // Start real-time widget observer
        widgetObserver = WidgetUpdateObserver(this)
        widgetObserver?.startObserving()

        // Schedule WorkManager periodic sync
        SyncScheduler.schedule(this)

        // Connectivity trigger
        syncTrigger = SyncTrigger(this, repository, prefs)
        syncTrigger?.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        syncTrigger?.stop()
        widgetObserver?.stopObserving()
    }
}
