package com.example.syncapp

import android.app.Application
import com.example.sync.ConnectivityObserver
import com.example.sync.SyncRepository
import com.example.sync.SyncScheduler
import com.example.sync.SyncTrigger
import com.example.syncwidget.WidgetUpdateObserver
import com.example.core.datastore.PreferencesManager

class SyncApp : Application() {
    private var observer: WidgetUpdateObserver? = null
    private var connectivityObserver: ConnectivityObserver? = null
    private var syncTrigger: SyncTrigger? = null

    override fun onCreate() {
        super.onCreate()
        
        // Initialize widget observer
        observer = WidgetUpdateObserver(this)
        observer?.start()
        
        // Initialize WorkManager periodic sync
        SyncScheduler.schedule(this)
        
        // Initialize connectivity observer for manual sync triggers
        val prefs = PreferencesManager(this)
        val repository = SyncRepository(this, prefs)
        syncTrigger = SyncTrigger(this, repository, prefs)
        syncTrigger?.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        syncTrigger?.stop()
    }
}
