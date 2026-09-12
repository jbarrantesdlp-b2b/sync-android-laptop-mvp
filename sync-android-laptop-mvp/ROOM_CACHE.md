# Room Database - Caché Persistente de Mensajes

## Arquitectura

### Entidades

**SyncMessageEntity**
```kotlin
@Entity(tableName = "sync_messages")
data class SyncMessageEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payload: String,
    val timestamp: Long,
    val direction: String,      // OUTBOUND | INBOUND
    val status: String,         // PENDING | SENT | RECEIVED | FAILED | ACKNOWLEDGED
    val createdAt: Long,
    val updatedAt: Long
)
```

### DAO - SyncMessageDao

Operaciones CRUD:
- `insert(message)` — Guardar mensaje
- `update(message)` — Actualizar estado
- `delete(message)` — Eliminar mensaje
- `getById(id)` — Recuperar por ID

Queries:
- `getAllFlow()` — Flow<List<SyncMessageEntity>> en tiempo real
- `getLatest(limit)` — Últimos N mensajes (default 50)
- `getByStatus(status)` — Filtrar por estado (PENDING, SENT, etc)
- `getByDirection(direction)` — Filtrar por dirección (OUTBOUND, INBOUND)
- `deleteOlderThan(timestamp)` — Limpiar antiguos
- `getCount()` — Contar mensajes

### Database - SyncDatabase

Singleton thread-safe:
```kotlin
val db = SyncDatabase.getDatabase(context)
val dao = db.syncMessageDao()
```

## Flujo de Mensajes con Room

### Envío (OUTBOUND)
```
SyncRepository.sendMessage()
  → SyncMessageQueue.enqueue()
  → SyncMessageEntity(status=PENDING) → Room.insert()
  → SharedFlow.emit()
  → WebSocket.send()
  → SyncMessageQueue.markSent()
  → SyncMessageEntity(status=SENT) → Room.update()
```

### Recepción (INBOUND)
```
WebSocket.onMessage()
  → SyncRepository.handleIncomingMessage()
  → SyncMessageQueue.addIncoming()
  → SyncMessageEntity(status=RECEIVED, direction=INBOUND) → Room.insert()
  → SharedFlow.emit()
```

### Recuperación de Pendientes (on reconnect)
```
SyncRepository.processPendingMessages()
  → SyncMessageQueue.getPending() (queries PENDING status)
  → Room.getByStatus("PENDING")
  → Reenviar cada mensaje
  → Room.update() con status=SENT
```

## Uso en Código

### Guardar Mensaje
```kotlin
val msg = SyncMessage(
    type = "sync_request",
    payload = "{...}"
)
messageQueue.enqueue(msg)  // Automáticamente en Room
```

### Recuperar Historial
```kotlin
val history = repository.getMessageHistory(limit = 100)
history.forEach { msg ->
    println("${msg.direction} - ${msg.status}: ${msg.type}")
}
```

### Limpiar Antiguos
```kotlin
repository.cleanupOldMessages(days = 7)  // Elimina mensajes > 7 días
```

### Obtener Pendientes
```kotlin
val pending = messageQueue.getPending()
pending.forEach { sendMessage(it) }
```

## Ventajas

1. **Persistencia** — Mensajes sobreviven cierre/crash de app
2. **Recuperación** — Reenvío automático de pendientes al reconectar
3. **Historial** — Completo de mensajes para debugging
4. **Cleanup** — Elimina automáticos de antiguos
5. **Transacciones** — ACID garantizado
6. **Queries Reactivas** — Flow para cambios en tiempo real

## Dependencias

```gradle
androidx.room:room-runtime:2.5.2
androidx.room:room-ktx:2.5.2
androidx.room:room-compiler:2.5.2  // kapt
```

## Migración (Futuro)

Para versión 2 de la BD:
```kotlin
@Database(
    entities = [SyncMessageEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
```

## Performance

- Índice en `id` (PK automático)
- Queries para status/direction son rápidas
- Cleanup por `timestamp` es eficiente
- In-memory cache (SharedFlow) + Room = lo mejor de ambos
