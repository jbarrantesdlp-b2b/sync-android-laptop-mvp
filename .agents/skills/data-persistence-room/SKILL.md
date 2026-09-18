---
name: data-persistence-room
description: Guía especializada para el Agente Data & Persistence. Cubre Room Database SQLite, DAOs, colas transaccionales de mensajes offline, DataStore y migraciones de esquema.
---

# Skill: Data & Persistence Specialist

Este rol gobierna la capa de persistencia en los módulos `:data` y `:core`.

## Directrices Técnicas

### 1. Base de Datos Room (`SyncDatabase.kt`)
- Entidad: `SyncMessageEntity.kt` con clave primaria UUID, tipo, payload, dirección y estado.
- DAO: `SyncMessageDao.kt` exponiendo `Flow<List<SyncMessageEntity>>` para reactividad continua.
- Estados de mensaje transaccionales:
  - `PENDING`: Mensaje encolado offline esperando conexión.
  - `SENT`: Mensaje transmitido por el WebSocket pero pendiente de ACK.
  - `RECEIVED`: Mensaje entrante recibido y confirmado.
  - `FAILED`: Fallo tras reintentos máximos.

### 2. Idempotencia y Reenvío
- Al reconectarse el WebSocket, procesar los mensajes en estado `PENDING` en orden cronológico ascendente (`ORDER BY timestamp ASC`).
- Asegurar que el `id` (UUID) sea el mismo en reintentos para evitar duplicados en el servidor.

### 3. DataStore Preferences (`PreferencesManager.kt`)
- Almacenamiento no relacional para:
  - Estado de conexión actual (`DISCONNECTED`, `CONNECTING`, `CONNECTED`).
  - Paleta de tema seleccionada (`ThemePack`).
  - Timestamp del último sync exitoso.
