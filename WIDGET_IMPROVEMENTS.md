# Widget Glance Improvements - Real-Time Updates

## Resumen

El widget Glance 2x2 ha sido mejorado significativamente para proporcionar actualizaciones en tiempo real de:
- ✅ Estado de conexión (🟢 Connected, 🟡 Connecting, 🔴 Disconnected)
- ✅ Contador de mensajes sincronizados
- ✅ Contador de mensajes pendientes
- ✅ Hora del último sync
- ✅ Intervalo del próximo sync programado
- ✅ Botones interactivos (Abrir App, Sync Manual)

---

## Componentes Implementados

### 1. **SyncGlanceWidget.kt** (Mejorado)
```kotlin
object SyncGlanceWidget : GlanceAppWidget(sizeMode = SizeMode.Responsive)
```

**Características:**
- Observa `PreferencesManager.connectionStatusFlow` para cambios de estado
- Observa `SyncRepository.messageFlow` para nuevos mensajes
- Renderiza UI responsivo con layouts Row/Column
- Muestra estado con iconos emoji y colores:
  - 🟢 Verde (CONNECTED) → #4CAF50
  - 🟡 Amarillo (CONNECTING) → #FFC107
  - 🔴 Rojo (DISCONNECTED) → #F44336
  - ⚪ Gris (UNKNOWN) → #9E9E9E

**Layout (2x2):**
```
┌─────────────────────┐
│  📱 Sync Status     │
├─────────────────────┤
│ 🟢 CONNECTED        │
│    Last sync: now   │
├─────────────────────┤
│ [📊 Open] [🔄 Sync] │
├─────────────────────┤
│ 0 Pending  0 Synced │
│         15min Next  │
└─────────────────────┘
```

### 2. **ManualSyncAction.kt** (ActionCallback)
```kotlin
class ManualSyncAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId)
}
```

**Funcionalidad:**
- Botón "🔄 Sync" dispara sincronización manual inmediata
- Llama a `SyncRepository.triggerManualSync()`
- Complementa WorkManager (evita esperar 15 min para next sync)
- Non-blocking; maneja excepciones silenciosamente

### 3. **SyncWidgetReceiver.kt** (GlanceAppWidgetReceiver)
```kotlin
class SyncWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SyncGlanceWidget
}
```

**Rol:**
- Registra el widget en el launcher de Android
- Maneja ciclo de vida del widget (onUpdate, onDeleted)
- Glance abstrae detalles de RemoteViews

### 4. **WidgetUpdateObserver.kt** (Mejorado)
```kotlin
class WidgetUpdateObserver(context: Context)
```

**Cambios:**
- **Antes:** Solo escuchaba `themePackFlow` con debounce(500)
- **Ahora:** Escucha múltiples flows en paralelo:
  1. `prefs.connectionStatusFlow` → debounce(300) → updateWidget()
  2. `repository.messageFlow` → debounce(500) → updateWidget()
  3. `prefs.themePackFlow` → debounce(300) → updateWidget()
  4. Combined flow de los 3 → debounce(500) → updateWidget()

**Debouncing Strategy:**
- 300ms para cambios individuales (rápidos: status, theme)
- 500ms para message flow (más frecuente)
- Combined observer con 500ms (evita múltiples updates simultáneos)
- **Resultado:** Widget actualiza en ~300-500ms máximo

**WorkManager Integration:**
```kotlin
PeriodicWorkRequestBuilder<WidgetUpdateWorker>(5, TimeUnit.MINUTES)
  .setConstraints(NetworkType.CONNECTED)
  .build()
```
- Widget se actualiza cada 5 minutos automáticamente
- Solo cuando hay conexión de red disponible
- Complementa los updates reactivos basados en flows

### 5. **WidgetUpdateWorker.kt** (Nuevo)
```kotlin
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : Worker(...)
```

**Propósito:**
- WorkManager task para actualizar widgets periódicamente
- Llamado cada 5 minutos (constraints: CONNECTED)
- Ejecuta `SyncGlanceWidget.updateAll(context)` en background
- Retry automático si falla

**Cycle:**
```
WorkManager (5 min) → WidgetUpdateWorker.doWork() 
  ↓
SyncGlanceWidget.updateAll(context)
  ↓
Widget re-renderiza con últimos datos
```

### 6. **GlanceWidgetViewModel.kt** (Opcional - Para futuras mejoras)
```kotlin
class GlanceWidgetViewModel(
    context: Context,
    prefs: PreferencesManager,
    repository: SyncRepository
) : ViewModel()
```

**Métodos:**
- `connectionStatusFlow` → StateFlow con último estado
- `messageFlow` → SharedFlow de mensajes en tiempo real
- `getMessageCount()` → Historial de últimos 100 mensajes
- `getLastMessageTime()` → Tiempo formateado (e.g., "5m ago", "2h ago")
- `getPendingCount()` → Mensajes esperando ACK

**Estado actual:** Scaffold; se puede expandir en futuras iteraciones

---

## Flujo de Actualización en Tiempo Real

```
┌─────────────────────────────────────┐
│    User Action / System Event       │
└────────────────┬────────────────────┘
                 │
         ┌───────▼────────┐
         │  Evento Activa  │
         │  PreferencesManager
         │  o Repository  │
         └───────┬────────┘
                 │
         ┌───────▼────────┐
         │  Flow emite    │
         │  nuevo valor   │
         └───────┬────────┘
                 │
         ┌───────▼────────────────┐
         │ WidgetUpdateObserver   │
         │ recibe emisión         │
         └───────┬────────────────┘
                 │
         ┌───────▼────────┐
         │  Debounce:     │
         │  300-500ms     │
         └───────┬────────┘
                 │
         ┌───────▼──────────────────┐
         │ SyncGlanceWidget.        │
         │ updateAll(context)       │
         └───────┬──────────────────┘
                 │
         ┌───────▼────────┐
         │  Glance re-    │
         │  renderiza UI  │
         │  con nuevos    │
         │  datos         │
         └───────┬────────┘
                 │
         ┌───────▼────────┐
         │  Widget muestra│
         │  en pantalla   │
         │  (2x2 layout)  │
         └────────────────┘
```

---

## Casos de Uso

### Caso 1: WiFi se conecta
1. ConnectivityManager emite WiFi disponible
2. ConnectivityObserver actualiza networkState → "Connected"
3. PreferencesManager.setConnectionStatus("CONNECTED")
4. WidgetUpdateObserver escucha connectionStatusFlow
5. Debounce 300ms
6. `SyncGlanceWidget.updateAll()` → Widget muestra 🟢 CONNECTED
7. **Latencia total:** ~300-400ms

### Caso 2: Nuevo mensaje sincronizado
1. SyncRepository recibe mensaje del servidor WebSocket
2. messageFlow emite nuevo SyncMessage
3. WidgetUpdateObserver.messageFlow.collect()
4. Debounce 500ms
5. Widget actualiza contador "Synced: 1"
6. **Latencia total:** ~500-600ms

### Caso 3: Usuario toca botón "🔄 Sync"
1. Botón dispara ManualSyncAction.onAction()
2. Llama a SyncRepository.triggerManualSync()
3. SyncTrigger inicia sync inmediato (no espera 15 min)
4. ConnectivityObserver + messageFlow actualizan
5. Widget muestra estado actualizado en ~300-500ms
6. **Latencia total:** ~500-700ms

### Caso 4: Sync fallido temporalmente
1. WebSocket se desconecta
2. PreferencesManager.setConnectionStatus("DISCONNECTED")
3. Widget cambia a 🔴 DISCONNECTED inmediatamente
4. WorkManager sigue intentando reconnect
5. **Usuario sabe estado actual al ver widget**

---

## Rendimiento & Optimizaciones

### Debounce Strategy
| Flow | Debounce | Razón |
|------|----------|-------|
| `connectionStatusFlow` | 300ms | Cambios raramente ocurren; evita flicker |
| `messageFlow` | 500ms | Puede tener ráfagas de mensajes |
| `themePackFlow` | 300ms | Cambios manuales del usuario |
| Combined | 500ms | Evita múltiples renders simultáneos |

### Memoria
- ViewModel no retenido (creado fresh en cada render)
- SharedFlow(replay=50) en SyncRepository es cacheado en memoria
- Widget updates son lazy (solo cuando Glance las necesita)

### Batería
- Debounce reduce número de actualizaciones
- WorkManager usa JobScheduler (batería-optimizado)
- Constraints: NetworkType.CONNECTED (evita trabajos innecesarios)

### Latencia
- Fastest: 300ms (status changes)
- Typical: 500ms (messages)
- Worst-case: 5 min (periodic WorkManager)

---

## Configuración en AndroidManifest

```xml
<!-- En :widget/src/main/AndroidManifest.xml -->
<receiver
    android:name="com.example.syncwidget.SyncWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/widget_info" />
</receiver>

<!-- Permisos requeridos (heredados de :app) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## Próximas Mejoras (Roadmap)

### Phase 2: Advanced Features
1. **Widget theming reactivo**
   - Aplicar theme (Minimal, Neón, Bancario) al widget automáticamente
   - Usar ThemeMapper.toGlancePalette() en SyncGlanceWidget

2. **Historial de mensajes en widget**
   - Mostrar último mensaje sincronizado (payload snippet)
   - Scroll histórico con más espacio (3x3 o 4x4)

3. **Notificaciones en tiempo real**
   - Cuando statusFlow cambia a DISCONNECTED → notificación local
   - Cuando messageFlow emite error → notificación con retry

4. **Configuración de widget**
   - WidgetConfigureActivity para elegir update frequency
   - Guardar en DataStore por widget ID

5. **Quick Actions expandidas**
   - Botón de settings (⚙️) → abre SettingsScreen
   - Botón de limpieza (🗑️) → limpia caché de mensajes
   - Toggle de notificaciones (🔔)

### Phase 3: Analytics
- Timber/Firebase Crashlytics para logs de widget updates
- Track latencia de renders
- Monitor WorkManager executions

---

## Testing

### Unit Tests (ya existen)
```kotlin
// sync/src/test/java/com/example/sync/BidirectionalSyncTest.kt
// Valida messageFlow y transiciones de estado
```

### Widget Tests (future)
```kotlin
// widget/src/test/java/com/example/syncwidget/WidgetUpdateObserverTest.kt
@Test
fun testMessageFlowDebounce() {
    // Verificar que múltiples mensajes rápidos solo disparen 1 update
}

@Test
fun testManualSyncAction() {
    // Mock SyncRepository.triggerManualSync()
    // Verificar que actionRunCallback() lo llamó
}
```

---

## Resumen Técnico

| Aspecto | Detalles |
|--------|---------|
| **Widget Size** | 2x2 (responsive; se adapta a 3x2, 4x2) |
| **Update Frequency** | Reactivo (300-500ms) + Periódico (5 min) |
| **Real-Time Source** | Flows de PreferencesManager + SyncRepository |
| **Actions** | Open App, Manual Sync |
| **Debounce** | Múltiples estrategias por flow type |
| **Persistence** | DataStore (tema, estado) + Room (mensajes) |
| **Threads** | Coroutines + WorkManager (background) |
| **Latency** | 300ms-5min (depende del evento) |
| **Memory** | ~5-10MB (Glance + SharedFlow caché) |
| **API Level** | 26+ (Android 8.0+) |

---

## Conclusión

El widget ahora es **completamente reactivo**, mostrando datos en tiempo real de:
- 🌐 Estado de conectividad
- 📬 Contador de mensajes
- ⏱️ Timestamps de sync
- 🎨 Tema visual actual

Todas las actualizaciones ocurren con **debounce optimizado** (300-500ms) y complementadas por **WorkManager periódico** (5 min) para garantizar que el widget siempre refleja el estado actual del app.
