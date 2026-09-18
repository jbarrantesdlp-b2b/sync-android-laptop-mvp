---
name: sync-network-protocol
description: Guía especializada para el Agente Sync & Network. Cubre OkHttp WebSockets, reconexión exponencial, AutoDiscovery triple canal (UDP Multicast, mDNS, BLE) y WorkManager.
---

# Skill: Sync & Network Protocol Specialist

Este rol gobierna el motor de comunicaciones en el módulo `:sync`.

## Directrices Técnicas

### 1. Conexión WebSocket
- Cliente: `LocalSocketClient.kt` basado en `OkHttpClient`.
- Reconexión: Backoff exponencial con jitter para no saturar el servidor desktop.
- Manejo de ciclo de vida: Desconectar limpiamente cuando no hay red activa para preservar batería.

### 2. Auto-Discovery Multi-Canal (`AutoDiscovery.kt`)
- **Multicast UDP:**
  - Socket Multicast a `239.55.55.1` en puerto `41234`.
  - Adquirir `WifiManager.MulticastLock` antes de escuchar paquetes.
- **mDNS / NSD:**
  - Usar `NsdManager.DiscoveryListener` para el tipo de servicio `_syncengine._tcp.`.
- **Bluetooth LE:**
  - `BluetoothLeScanner` con filtro de fabricante `0xFFFF` y payload prefijado `SE`.
  - Siempre detener el escaneo BLE tras encontrar el dispositivo o tras 15 segundos de timeout para evitar drenaje de batería.

### 3. Tareas en Background (WorkManager)
- `SyncWorker.kt` configurado como `CoroutineWorker`.
- `SyncScheduler.kt` programa trabajos periódicos cada 15 minutos con constraints de red (`NetworkType.CONNECTED`).
