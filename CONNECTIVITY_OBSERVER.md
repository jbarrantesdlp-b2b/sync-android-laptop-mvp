# Detector de Conectividad - Arquitectura

## Componentes

### ConnectivityObserver
Monitorea cambios en el estado de la red usando ConnectivityManager:

```kotlin
val observer = ConnectivityObserver(context)
observer.startObserving()

observer.networkState.collect { state ->
    when (state) {
        is NetworkState.Connected -> println("WiFi/Cellular/Ethernet: ${state.type}")
        NetworkState.Disconnected -> println("No connection")
        NetworkState.Connecting -> println("Connecting...")
        NetworkState.Unknown -> println("Unknown state")
    }
}
observer.stopObserving()
```

**NetworkState:**
- `Connected(type: String)` — WiFi, Cellular, Ethernet
- `Disconnected` — Sin conexión de red
- `Connecting` — En proceso de conexión
- `Unknown` — Estado desconocido

### SyncTrigger
Observa cambios de ConnectivityObserver y dispara sincronización automática:

```kotlin
val trigger = SyncTrigger(context, repository, prefs)
trigger.start()  // Inicia observación
trigger.stop()   // Detiene observación
```

**Comportamiento:**
- Cuando red se conecta → dispara `triggerManualSync()`
- Envía `sync_request` con trigger="connectivity_change"
- Actualiza PreferencesManager según estado de red

### Integración en SyncApp.onCreate()
```kotlin
override fun onCreate() {
    // ... otros inicializadores ...
    
    val prefs = PreferencesManager(this)
    val repository = SyncRepository(this, prefs)
    syncTrigger = SyncTrigger(this, repository, prefs)
    syncTrigger?.start()
}
```

## Flujo de Eventos

```
Dispositivo pierde WiFi
    ↓
ConnectivityObserver.onLost()
    ↓
networkState → NetworkState.Disconnected
    ↓
SyncTrigger.collect() → prefs.setConnectionStatus("DISCONNECTED")
    ↓
SettingsScreen actualiza UI (rojo)

---

Dispositivo se reconecta a WiFi
    ↓
ConnectivityObserver.onAvailable()
    ↓
networkState → NetworkState.Connected("WiFi")
    ↓
SyncTrigger.collect() → triggerManualSync()
    ↓
repository.connectToServer() + sendMessage()
    ↓
Sync inmediato (no espera próximo trabajo periódico de 15 min)
```

## Permisos Requeridos

Ya incluidos en AndroidManifest (:app):
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.CHANGE_NETWORK_STATE`

## UI Improvements

**SettingsScreen ahora muestra:**
- Estado de sincronización (CONNECTED/CONNECTING/DISCONNECTED)
- Tipo de red actual (WiFi, Cellular, No connection, Unknown)
- Color dinámico según estado (🟢 connected, 🟡 connecting, 🔴 disconnected)
- Log de eventos de conectividad

## Ventajas

1. **Sincronización Reactiva** — No espera 15 minutos después de perder/recuperar red
2. **Eficiencia** — No intenta conectar si no hay red
3. **Debugging** — Eventos en log para ver cuándo se dispara sync
4. **Lifecycle** — Cleanup automático en onTerminate()
