# SyncApp MVP - Estructura Modular Android + WebSocket

## Arquitectura

```
sync-android-laptop-mvp/
├── app/                 # Módulo principal (MainActivity, SyncApp)
├── core/                # Shared preferences, modelos (ThemePack)
├── data/                # Enums y tipos de datos
├── sync/                # WorkManager, LocalSocketClient, SyncScheduler
├── widget/              # Glance widget reactivo 2x2
└── desktop-electron/    # Servidor WebSocket local (puerto 8123)
```

## Permisos Android

### Módulo :app
- **INTERNET** — Acceso a Internet para WebSocket
- **ACCESS_NETWORK_STATE** — Verificar estado de conectividad
- **ACCESS_FINE_LOCATION** — Ubicación GPS (red local)
- **ACCESS_COARSE_LOCATION** — Ubicación aproximada
- **RECEIVE_BOOT_COMPLETED** — Iniciar WorkManager tras reinicio
- **CHANGE_NETWORK_STATE** — Ajustar estado de red si es necesario

### Módulo :widget
- **RECEIVE_BOOT_COMPLETED** — Widget inicia tras reinicio
- **INTERNET** — Actualizaciones de widget vía sync
- **ACCESS_NETWORK_STATE** — Verificar conectividad

## Sincronización

**SyncWorker** (PeriodicWorkRequest cada 15 minutos):
1. Conecta a `ws://localhost:8123`
2. Envía solicitud de sync con timestamp
3. Actualiza PreferencesManager con estado de conexión
4. Reintentos exponenciales en caso de fallo

**LocalSocketClient**:
- WebSocket con reconexión automática
- Backoff exponencial (máx 60 segundos)
- Listener para mensajes entrantes

## Desktop WebSocket

```bash
cd desktop-electron
npm install
npm start
# Escucha en ws://localhost:8123
```

## Versiones

- Kotlin: 1.8.22
- Compose: 1.4.7
- Glance: 1.0.0
- Coroutines: 1.7.3
- DataStore: 1.1.0
- OkHttp: 4.11.0
- WorkManager: 2.8.1
