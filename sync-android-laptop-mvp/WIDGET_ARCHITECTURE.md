# Widget Glance Real-Time Architecture

## Diagrama de Componentes Integrados

```
┌────────────────────────────────────────────────────────────────────┐
│                      Widget Glance (2x2)                            │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  📱 Sync Status        🟢 CONNECTED                          │  │
│  │  ┌────────────────────────────────────────────────────────┐  │  │
│  │  │  Last sync: now                                        │  │  │
│  │  └────────────────────────────────────────────────────────┘  │  │
│  │  [📊 Open]           [🔄 Sync]                              │  │
│  │  ┌─────────┬──────────┬──────────┐                         │  │
│  │  │ 0 Pend  │ 0 Synced │ 15m Next │                         │  │
│  │  └─────────┴──────────┴──────────┘                         │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
                 ▲                           ▲
                 │ data binding              │ action dispatch
                 │ (Flows)                   │
    ┌────────────┴────────────┐  ┌───────────┴────────────────┐
    │ PreferencesManager      │  │ ManualSyncAction            │
    │ ├─ connectionStatusFlow │  │ ├─ onAction()              │
    │ ├─ themePackFlow        │  │ └─ triggerManualSync()     │
    │ └─ fontFamilyFlow       │  │                             │
    └─────────────┬───────────┘  └───────────┬────────────────┘
                  │                          │
    ┌─────────────▼──────────────────────────▼───────────────┐
    │         SyncRepository (Central Façade)                 │
    │  ├─ messageFlow: SharedFlow<SyncMessage>               │
    │  ├─ connectionStatusFlow: Flow<String>                 │
    │  ├─ sendMessage(): suspend Unit                        │
    │  ├─ triggerManualSync(): suspend Unit                  │
    │  └─ getMessageHistory(): List<SyncMessage>             │
    └─────────────┬──────────────────┬──────────────────┬────┘
                  │                  │                  │
        ┌─────────▼─────────┐ ┌──────▼──────┐ ┌────────▼──────────┐
        │ LocalSocketClient  │ │ SyncWorker  │ │ SyncTrigger       │
        │ (WebSocket)        │ │ (periodic)  │ │ (on connect)      │
        │ • Reconnect Logic  │ │ • 15min     │ │ • triggers sync   │
        │ • send/receive     │ │ • WorkMgr   │ │ • observes network│
        └────────┬───────────┘ └─────┬───────┘ └────────┬──────────┘
                 │                   │                  │
                 └───────────────────┴──────────────────┘
                           ▲
                           │
                  ┌────────┴─────────────┐
                  │ SyncMessageQueue     │
                  │ (Room DB Persistence)│
                  │ • enqueue()          │
                  │ • markSent()         │
                  │ • markAcknowledged() │
                  │ • getAllFlow()       │
                  └──────────────────────┘
```

---

## Flujo de Datos: Widget ← Síncronización

```
1. ESTADO DE CONEXIÓN CAMBIA
   ┌────────────────────────────────────┐
   │ ConnectivityManager detecta cambio │ (Android System)
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ ConnectivityObserver.onAvailable() │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ PreferencesManager                 │
   │ .setConnectionStatus(status)       │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ connectionStatusFlow.emit(status)  │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ WidgetUpdateObserver escucha flow  │
   │ .debounce(300ms)                   │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ SyncGlanceWidget.updateAll()       │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ provideContent() re-renderiza UI   │
   │ Muestra: 🟢 CONNECTED              │
   └────────────────────────────────────┘


2. NUEVO MENSAJE RECIBIDO
   ┌────────────────────────────────────┐
   │ WebSocket Server envía JSON        │ (localhost:8123)
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ LocalSocketClient.onMessage()      │
   │ (async callback)                   │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ SyncRepository.handleIncomingMsg() │
   │ → parseJSON() → SyncMessageQueue   │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ SyncMessageQueue.addIncoming()     │
   │ → messageFlow.emit(message)        │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ WidgetUpdateObserver escucha flow  │
   │ .debounce(500ms)                   │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ SyncGlanceWidget.updateAll()       │
   │ → getMessageHistory(1)             │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ Widget muestra:                    │
   │ • "Last sync: now"                 │
   │ • "0 Synced" → incrementa          │
   └────────────────────────────────────┘


3. USUARIO TOCA BOTÓN "🔄 SYNC"
   ┌────────────────────────────────────┐
   │ Tap en Button: "🔄 Sync"          │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ ManualSyncAction.onAction() fired  │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ SyncRepository.triggerManualSync() │
   │ → SyncTrigger.triggerSync()        │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ LocalSocketClient.connect()        │
   │ Envía: SyncMessage(type="PING")    │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ WebSocket responde                 │
   │ → onMessage() callback             │
   └────────────────────────────────────┘
                    ▼
   ┌────────────────────────────────────┐
   │ Widget actualiza (300-500ms)       │
   │ Muestra estado CONNECTED/SENDING   │
   └────────────────────────────────────┘
```

---

## Integración con Otros Módulos

### :core (PreferencesManager)
```
Widget ◄─────────────► PreferencesManager
            ▲                  ▲
            │                  │
     Flow observation    DataStore persistence
            │                  │
       connectionStatusFlow    ════════════════
       themePackFlow
       fontFamilyFlow
```

**Datos observados:**
- `connectionStatusFlow: Flow<String>` → "CONNECTED", "DISCONNECTED", etc.
- `themePackFlow: Flow<ThemePack>` → Minimal, Neón, Bancario (opcional en widget)

---

### :sync (SyncRepository + LocalSocketClient)
```
Widget ◄─────────────► SyncRepository
         (manual)           ▲
       click "Sync"         │
            ▼               │
  ManualSyncAction    triggerManualSync()
            │               │
            └───────────────┘
                    │
          ┌─────────▼─────────┐
          │ LocalSocketClient  │
          │ ├─ connect()       │
          │ ├─ send(msg)       │
          │ └─ disconnect()    │
          └────────┬───────────┘
                   │
          ┌────────▼────────┐
          │ WebSocket       │
          │ localhost:8123  │
          │ (desktop-electron)
          └─────────────────┘
```

**Flujo:**
1. Usuario toca "🔄 Sync" → ManualSyncAction.onAction()
2. Llama SyncRepository.triggerManualSync()
3. Envía ping a WebSocket server
4. Server responde → messageFlow emite
5. WidgetUpdateObserver.debounce(500ms)
6. Widget re-renderiza con estado actualizado

---

### :data (Room Database)
```
Widget ◄────────────► SyncMessageQueue
             │              │
       getMessageHistory()   │
             │       ┌───────▼──────────┐
             │       │ SyncDatabase     │
             │       │ (Room)           │
             ├──────►├─ SyncMessageDao  │
             │       │ ├─ getByStatus() │
             │       │ ├─ getByDirection│
             │       │ └─ getAllFlow()  │
             │       └──────────────────┘
             │
       • Message count
       • Last message timestamp
       • Pending count
```

**Datos persistidos:**
- `sync_messages` table con campos:
  - id, type, payload, timestamp, direction, status, createdAt, updatedAt

---

### :app (MainActivity + SyncApp)
```
Widget Button ──────────► actionStartActivity()
  "📊 Open"              │
                         ▼
                  ┌─────────────────┐
                  │  MainActivity   │
                  │ (muestra UI)    │
                  │ • SettingsScreen│
                  │ • Sync logs     │
                  └─────────────────┘
```

**Integración:**
- SyncApp.onCreate() inicializa WidgetUpdateObserver
- MainActivity hereda observación automática
- Usuario puede abrir widget → app para más detalles

---

## Operaciones Clave del Widget

### 1. Display Connection Status (300ms latency)
```kotlin
// En SyncGlanceWidget.provideContent()
val connectionStatus = prefs.connectionStatusFlow.first()
val statusIcon = when (connectionStatus) {
    "CONNECTED" -> "🟢"
    "CONNECTING" -> "🟡"
    "DISCONNECTED" -> "🔴"
    else -> "⚪"
}
Text(text = statusIcon)
Text(text = connectionStatus.replace("_", " "))
```

**Flow:**
1. connectionStatusFlow emite valor
2. WidgetUpdateObserver.debounce(300ms)
3. SyncGlanceWidget.updateAll()
4. provideContent() llamado
5. Widget muestra nuevo icono

---

### 2. Display Message Count (500ms latency)
```kotlin
// getMessageHistory(100) → cuenta todos los mensajes
val history = repository.getMessageHistory(100)
val count = history.size

// En stats box:
Text(text = "$count")
Text(text = "Synced")
```

**Flow:**
1. messageFlow emite nuevo SyncMessage
2. WidgetUpdateObserver.messageFlow.collect()
3. Debounce 500ms
4. SyncGlanceWidget.updateAll()
5. getMessageHistory() consulta Room DB
6. Widget actualiza contador

---

### 3. Manual Sync Trigger (700ms latency)
```kotlin
// ManualSyncAction.kt
override suspend fun onAction(context: Context, glanceId: GlanceId) {
    val repository = SyncRepository(context)
    repository.triggerManualSync()  // ← No espera 15 min de WorkManager
}
```

**Flow:**
1. Usuario toca "🔄 Sync"
2. ManualSyncAction.onAction() ejecuta
3. SyncRepository.triggerManualSync() → SyncTrigger.triggerSync()
4. LocalSocketClient envía ping al servidor
5. Server responde → messageFlow emite
6. WidgetUpdateObserver + debounce
7. Widget refleja nuevo estado

---

## Performance Metrics

| Métrica | Valor | Notas |
|---------|-------|-------|
| **Time to display status** | ~300ms | Debounce(300) + Glance render |
| **Time to update message count** | ~500ms | Debounce(500) + DB query |
| **Time to respond to manual sync** | ~700ms | Network + debounce(500) |
| **WorkManager periodic updates** | 5 min | Fallback if flows fail |
| **Widget memory footprint** | ~5-10MB | SharedFlow(50) + UI state |
| **CPU usage** | Minimal | Only when data changes (debounced) |
| **Battery impact** | Low | Constraints + lazy rendering |

---

## Error Handling

### Case: WebSocket Disconnects
```
LocalSocketClient.onDisconnected()
  ▼
PreferencesManager.setConnectionStatus("DISCONNECTED")
  ▼
connectionStatusFlow emits
  ▼
WidgetUpdateObserver.debounce(300ms)
  ▼
SyncGlanceWidget shows 🔴 DISCONNECTED
  ▼
SyncTrigger waits for connectivity restore
  ▼
When reconnected → automatic triggerSync()
  ▼
Widget shows 🟢 CONNECTED again
```

### Case: Message Processing Error
```
SyncRepository.handleIncomingMessage() throws Exception
  ▼
Error logged (not visible in widget)
  ▼
messageFlow continues to emit (doesn't crash)
  ▼
Widget remains stable, shows last known state
  ▼
SyncWorker retries in 15 minutes
```

---

## Testing Strategy

### Unit Tests
```kotlin
// WidgetUpdateObserverTest.kt
@Test
fun testConnectionStatusDebounce() {
    // Emitir múltiples valores rápidos
    prefsFlow.emit("CONNECTING")
    prefsFlow.emit("CONNECTED")
    prefsFlow.emit("CONNECTED")
    
    // Solo debe actualizar widget 1 vez (debounced)
    advanceTimeBy(300)
    verify(glanceWidget).updateAll()
    verifyNoMoreInteractions()
}

@Test
fun testManualSyncActionTriggered() {
    val action = ManualSyncAction()
    action.onAction(context, glanceId)
    
    verify(repository).triggerManualSync()
}
```

### Integration Tests
```kotlin
// WidgetIntegrationTest.kt (future - requires emulator)
@RunWith(AndroidJUnit4::class)
class WidgetIntegrationTest {
    @Test
    fun testWidgetUpdatesOnStatusChange() {
        // Cambiar status de conectividad
        // Verificar que widget refleja cambio en <500ms
    }
}
```

---

## Roadmap de Mejoras

### Q4 2026 - Phase 2
- [ ] Widget theming dinámico (aplicar tema del app)
- [ ] Historial de últimos 5 mensajes
- [ ] Notificaciones locales en desconexión
- [ ] WidgetConfigureActivity para personalización

### Q1 2027 - Phase 3
- [ ] Widget 4x2 con más información
- [ ] Quick actions expandidas (settings, clear cache)
- [ ] Analytics de widget usage
- [ ] Dark mode automático

### Q2 2027 - Phase 4
- [ ] Widget responsivo (adaptarse a 3x3, 4x4)
- [ ] Gráficos de historial de sync
- [ ] Predicción de próximo sync
- [ ] Integration con sistema de notificaciones

---

## Conclusión

El widget Glance es ahora un **componente de primera clase** en la arquitectura del MVP:
- ✅ Reactivo a cambios en tiempo real (300-500ms)
- ✅ Integrado con todos los módulos críticos
- ✅ Optimizado para batería (debounce + constraints)
- ✅ Robusto ante errores (fallbacks, retries)
- ✅ Extensible para futuras mejoras

El usuario **ve el estado de la aplicación sin abrir el app**, lo que mejora la UX significativamente.
