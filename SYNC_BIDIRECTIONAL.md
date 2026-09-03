# Sincronización Bidireccional - Arquitectura

## Modelo de Datos

### SyncMessage
```kotlin
data class SyncMessage(
    val id: String,              // UUID único
    val type: String,            // "sync_request", "ack", "data_update"
    val payload: String,         // Datos JSON
    val timestamp: Long,
    val direction: SyncDirection, // OUTBOUND | INBOUND
    val status: SyncStatus       // PENDING | SENT | RECEIVED | FAILED | ACKNOWLEDGED
)
```

## Flujo de Sincronización

### 1. Envío (OUTBOUND)
```
App → SyncRepository.sendMessage()
  → SyncMessageQueue.enqueue()    (status: PENDING)
  → LocalSocketClient.send()       (WebSocket emit)
  → SyncMessageQueue.markSent()   (status: SENT)
  → await ACK from server
  → SyncMessageQueue.markAcknowledged() (status: ACKNOWLEDGED)
```

### 2. Recepción (INBOUND)
```
Server → LocalSocketClient.onMessage()
  → SyncRepository.handleIncomingMessage()
  → parse JSON → SyncMessage
  → SyncMessageQueue.addIncoming() (status: RECEIVED)
  → sendAcknowledgment() (auto ACK)
```

## Componentes Principales

### LocalSocketClient
- Conecta a `ws://localhost:8123`
- Maneja reconexión automática (backoff exponencial)
- Callbacks: `onMessage`, `onConnected`, `onDisconnected`
- Método `send()` devuelve `Boolean` para verificar delivery

### SyncMessageQueue
- Almacena mensajes en memoria (replay=50)
- SharedFlow<SyncMessage> para observar cambios
- Métodos: enqueue, markSent, markAcknowledged, addIncoming

### SyncRepository
- Façade para la lógica de sincronización
- Expone `messageFlow: SharedFlow<SyncMessage>`
- Auto-procesa mensajes pendientes al conectar
- Auto-envía ACK en recepción

### SyncWorker (PeriodicWorkRequest)
- Ejecuta cada 15 minutos
- Usa SyncRepository para conectar y enviar
- Actualiza PreferencesManager con estado

## Formato de Mensajes JSON

### Sync Request (OUTBOUND)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "type": "sync_request",
  "payload": "{\"timestamp\": 1234567890}",
  "timestamp": 1234567890
}
```

### Acknowledgment (OUTBOUND)
```json
{
  "type": "ack",
  "ackId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": 1234567890
}
```

### Server Response (INBOUND)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "type": "sync_response",
  "payload": "{...data...}",
  "timestamp": 1234567890
}
```

## UI Integration

### SettingsScreen
- Muestra connectionStatus en tiempo real
- Síncro log (últimos 10 eventos)
- Botón "Trigger Sync Now" para envío manual
- Monitoreo de SyncRepository.messageFlow

## Error Handling

1. **WebSocket Desconexión**: LocalSocketClient reintentos con backoff exponencial
2. **Mensajes No Entregados**: Guardados en cola y reenviados al reconectar
3. **Timeout**: SyncWorker retira Result.retry() automático
4. **JSON Parse Error**: Capturado, logged pero no fatal

## Testing

Servidor WebSocket echo (desktop-electron/main.js):
```javascript
ws.on('message', (message) => {
  ws.send(`echo: ${message}`);
});
```

Para pruebas manuales:
```bash
npm start  # en desktop-electron/
```

Desde Android: SyncRepository.sendMessage("test", "{...}")
