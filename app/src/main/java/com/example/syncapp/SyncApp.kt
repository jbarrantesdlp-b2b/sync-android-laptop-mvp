package com.example.syncapp

import android.app.Application
import com.example.core.datastore.PreferencesManager
import com.example.sync.AutoDiscovery
import com.example.sync.DiscoveredPeer
import com.example.sync.SyncRepository
import com.example.sync.SyncScheduler
import com.example.sync.SyncTrigger
import com.example.syncwidget.WidgetUpdateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    lateinit var discovery: AutoDiscovery
        private set

    private var widgetObserver: WidgetUpdateObserver? = null
    private var syncTrigger: SyncTrigger? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        prefs = PreferencesManager(this)

        ioScope.launch {
            prefs.setConnectionStatus("DISCONNECTED")
        }

        repository = SyncRepository(this, prefs)

        widgetObserver = WidgetUpdateObserver(this)
        widgetObserver?.startObserving()

        SyncScheduler.schedule(this)

        syncTrigger = SyncTrigger(this, repository, prefs)
        syncTrigger?.start()

        // Auto-discover and connect on startup via Wi-Fi subnet probe
        repository.autoDiscoverAndConnect()

        discovery = AutoDiscovery(this, ::onLaptopFound)
        discovery.start()
    }

    fun restartDiscovery() {
        discovery.stop()
        discovery.start()
    }

    private fun onLaptopFound(peer: DiscoveredPeer) {
        ioScope.launch {
            val current = try { prefs.serverUrlFlow.first() } catch (_: Exception) { "" }
            val already = repository.isConnected() && current == peer.url
            if (!already) {
                prefs.setServerUrl(peer.url)
                repository.connectToServer(peer.url)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        discovery.stop()
        syncTrigger?.stop()
        widgetObserver?.stopObserving()
    }
}
