# SyncApp MVP - Resumen Completo

## 📊 Estructura del Proyecto

```
sync-android-laptop-mvp/
├── README.md                                          # Arquitectura general
├── PROJECT_OVERVIEW.md                                 # Este archivo
├── WIDGET_IMPROVEMENTS.md                              # Mejoras widget real-time
├── WIDGET_ARCHITECTURE.md                              # Integración arquitectónica
├── WIDGET_VISUAL.md                                    # Estados visuales y UX
├── SYNC_BIDIRECTIONAL.md                             # Sincronización send/receive
├── CONNECTIVITY_OBSERVER.md                          # Detector de conectividad
├── ROOM_CACHE.md                                     # Base de datos persistente
├── TESTING.md                                        # Tests unitarios
│
├── app/                                              # Módulo principal
│   ├── build.gradle.kts
│   ├── src/main/AndroidManifest.xml                 # Permisos: INTERNET, ACCESS_NETWORK_STATE, etc
│   └── src/main/java/com/example/syncapp/
│       ├── SyncApp.kt                               # Inicializador (WidgetObserver, SyncScheduler, SyncTrigger)
│       ├── MainActivity.kt                          # Activity principal
│       ├── SyncViewModel.kt                         # ViewModel reactivo
│       └── ui/
│           ├── SettingsScreen.kt                    # UI con estado de conexión y logs
│           └── theme/Theme.kt                       # Tema Material3
│
├── widget/                                           # Módulo widget Glance 2x2 (Real-Time)
│   ├── build.gradle.kts
│   ├── src/main/AndroidManifest.xml
│   └── src/main/java/com/example/syncwidget/
│       ├── SyncGlanceWidget.kt                      # ✅ Widget reactivo con multi-flow listener
│       ├── ManualSyncAction.kt                      # ✅ Botón sync manual (instant trigger)
│       ├── WidgetUpdateObserver.kt                  # ✅ Multi-flow listener con debounce
│       ├── WidgetUpdateWorker.kt                    # ✅ Periodic updates via WorkManager
│       ├── GlanceWidgetViewModel.kt                 # ✅ ViewModel scaffold (Phase 2)
│       ├── ThemeMapper.kt                           # Traducción ThemePack → GlancePalette
│       └── SyncWidgetReceiver.kt                    # Receiver de widget
│
├── core/                                             # Módulo compartido
│   ├── build.gradle.kts
│   ├── src/main/java/com/example/core/
│   │   ├── datastore/
│   │   │   └── PreferencesManager.kt               # DataStore: theme, font, connectionStatus
│   │   └── model/
│   │       └── ThemePack.kt                        # Minimal, Neón, Bancario (paletas de color)
│
├── data/                                             # Módulo de datos
│   ├── build.gradle.kts                            # Incluye Room + coroutines test
│   ├── src/main/java/com/example/data/
│   │   ├── ConnectionStatus.kt                     # Enum: DISCONNECTED, CONNECTING, CONNECTED, UNKNOWN
│   │   ├── SyncMessage.kt                          # Data class con UUID, type, payload, timestamp
│   │   └── db/
│   │       ├── SyncMessageEntity.kt                # Entidad Room
│   │       ├── SyncMessageDao.kt                   # DAO con queries
│   │       └── SyncDatabase.kt                     # Singleton Room
│   └── src/test/java/com/example/data/
│       └── db/SyncMessageEntityTest.kt             # Tests de entidad
│
├── sync/                                             # Módulo de sincronización
│   ├── build.gradle.kts                            # OkHttp, WorkManager, org.json, Room, test deps
│   ├── src/main/java/com/example/sync/
│   │   ├── LocalSocketClient.kt                    # WebSocket con reconexión exponencial
│   │   ├── SyncWorker.kt                           # CoroutineWorker (cada 15 min)
│   │   ├── SyncScheduler.kt                        # Enqeueador de trabajos periódicos
│   │   ├── SyncMessageQueue.kt                     # Cola en-memory + Room
│   │   ├── SyncRepository.kt                       # Façade de sincronización
│   │   ├── SyncNotificationHelper.kt               # Logging/notificaciones
│   │   ├── ConnectivityObserver.kt                 # Monitoreo de red con ConnectivityManager
│   │   ├── NetworkState.kt enum                    # Connected(type), Disconnected, etc
│   │   └── SyncTrigger.kt                          # Auto-sync en cambios de conectividad
│   └── src/test/java/com/example/sync/
│       ├── SyncMessageTest.kt                      # Tests de data class
│       ├── LocalSocketClientTest.kt                # Tests de WebSocket
│       └── BidirectionalSyncTest.kt                # Tests de flujos send/receive
│
├── desktop-electron/                                 # Servidor WebSocket Node.js
│   ├── package.json
│   ├── main.js                                      # WebSocket en puerto 8123
│   └── node_modules/ws/
│
├── settings.gradle.kts                              # Incluye: app, widget, core, data, sync
├── build.gradle.kts                                 # Versiones centralizadas
├── gradle.properties                                # JVM args, gradle settings
└── .git/                                            # Repositorio git
```

## 🔧 Versiones

| Componente | Versión |
|------------|---------|
| Kotlin | 1.8.22 |
| Compose | 1.4.7 |
| Glance | 1.0.0 |
| Coroutines | 1.7.3 |
| DataStore | 1.1.0 |
| OkHttp | 4.11.0 |
| WorkManager | 2.8.1 |
| Room | 2.5.2 |
| JUnit | 4.13.2 |

## 🚀 Flujos Principales

### 1. Sincronización Periódica (WorkManager)
```
SyncApp.onCreate()
  → SyncScheduler.schedule() // PeriodicWorkRequest c/15min
  → SyncWorker.doWork()
  → SyncRepository.connectToServer("ws://localhost:8123")
  → sendMessage("sync_request", timestamp)
  → Room.insert(PENDING)
  → WebSocket.send()
  → Room.update(SENT)
```

### 2. Detección de Conectividad
```
ConnectivityObserver.startObserving()
  → Red perdida → NetworkState.Disconnected → prefs.set("DISCONNECTED")
  → Red recuperada → NetworkState.Connected("WiFi")
  → SyncTrigger.triggerManualSync()  // No espera 15 min
  → sendMessage con trigger="connectivity_change"
```

### 3. Recepción de Mensajes
```
WebSocket.onMessage(json)
  → SyncRepository.handleIncomingMessage()
  → parse JSON → SyncMessage
  → SyncMessageQueue.addIncoming()
  → Room.insert(INBOUND, RECEIVED)
  → sendAcknowledgment(id)
```

### 4. Recuperación en Reconexión
```
SyncRepository.connectToServer()
  → onConnected()
  → processPendingMessages()
  → Room.getByStatus("PENDING")
  → reenviar cada uno
  → Room.update(SENT)
```

## 📱 Permisos Android

```xml
<!-- app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

## 🧪 Tests

```bash
# Ejecutar todos los tests
./gradlew test

# Módulo específico
./gradlew :sync:test
./gradlew :data:test

# Con reporte
./gradlew test --info
```

**Cobertura esperada: ~80%** en lógica crítica

## 🌐 WebSocket Server

```bash
cd desktop-electron
npm install
npm start
# Escucha en ws://localhost:8123
```

**Responde con echo:** `ws.send('echo: ' + message)`

## 🎨 Temas

- **Minimal** — Colores oscuros/purpura
- **Neón** — Verde brillante/cian/magenta
- **Bancario** — Azul/gris corporativo

Se almacenan en DataStore y se aplican a Widget + UI

## 📦 Dependencias Principales

```gradle
// Compose & UI
androidx.compose:ui:1.4.7
androidx.glance:glance:1.0.0

// Sync & Network
com.squareup.okhttp3:okhttp:4.11.0
androidx.work:work-runtime-ktx:2.8.1

// Data & Storage
androidx.datastore:datastore-preferences:1.1.0
androidx.room:room-runtime:2.5.2

// Async
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3
```

## 📋 Estado de Implementación

| Feature | Status |
|---------|--------|
| ✅ Estructura modular | ✅ Completado |
| ✅ WorkManager periódico | ✅ Completado |
| ✅ Sincronización bidireccional | ✅ Completado |
| ✅ Detector de conectividad | ✅ Completado |
| ✅ Caché Room DB | ✅ Completado |
| ✅ Tests unitarios | ✅ Completado |
| 📋 Widget Glance reactivo | ✅ Completado (real-time updates) |
| 📋 UI de historial de sync | En desarrollo |
| 📋 Tests E2E | Planeado |

## 🔗 Documentación

- `README.md` — Arquitectura y permisos
- `SYNC_BIDIRECTIONAL.md` — Flujos send/receive y formatos JSON
- `CONNECTIVITY_OBSERVER.md` — Eventos de red
- `ROOM_CACHE.md` — Base de datos y queries
- `TESTING.md` — Tests y cobertura
