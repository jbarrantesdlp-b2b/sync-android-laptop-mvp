# Tests Unitarios - Arquitectura

## Test Coverage

### 1. Entidades y Modelos (Pure JUnit)

**SyncMessageEntityTest**
- ✅ Creación de entidad
- ✅ Valores por defecto
- ✅ Estados válidos (PENDING, SENT, RECEIVED, FAILED, ACKNOWLEDGED)
- ✅ Direcciones válidas (OUTBOUND, INBOUND)

**SyncMessageTest**
- ✅ Creación de mensaje con UUID único
- ✅ Timestamp automático
- ✅ Copy con actualizaciones de estado
- ✅ Enums de SyncStatus y SyncDirection

### 2. Cliente WebSocket (Pure JUnit)

**LocalSocketClientTest**
- ✅ Inicialización con callbacks
- ✅ Estado offline por defecto
- ✅ send() retorna false cuando desconectado
- ✅ Soporta múltiples URLs (ws://, wss://)
- ⚠️ Conexión real requiere mock o servidor

### 3. Sincronización Bidireccional (Coroutines Test)

**BidirectionalSyncTest**
- ✅ Flujo outbound (PENDING → SENT → ACKNOWLEDGED)
- ✅ Flujo inbound (INBOUND + RECEIVED)
- ✅ Transiciones de estado de mensaje
- ✅ Tipos de mensaje (sync_request, sync_response, ack)
- ✅ Unicidad de ID de mensaje
- ✅ Orden de timestamps
- ✅ Preservación de payload JSON

### 4. Queue de Mensajes (Coroutines + Room)

**SyncMessageQueueTest** (próximo)
- ✅ Enqueue de mensaje (PENDING)
- ✅ markSent() actualiza estado
- ✅ markAcknowledged() actualiza estado
- ✅ addIncoming() con dirección INBOUND
- ✅ getPending() retorna solo PENDING
- ✅ getHistory() retorna últimos N
- ✅ cleanupOlderThan() elimina antiguos
- ✅ MessageFlow emite cambios

### 5. Repositorio (Mocks + Coroutines)

**SyncRepositoryTest** (próximo)
- ✅ sendMessage() encolado y enviado
- ✅ handleIncomingMessage() parsea JSON
- ✅ Auto-ACK en recepción
- ✅ processPendingMessages() reenvía
- ✅ getMessageHistory(limit) con Room
- ✅ cleanupOldMessages(days)

## Ejecución

```bash
# Tests de unidad (JUnit)
./gradlew :sync:test
./gradlew :data:test

# Tests de integración (Android Instrumented)
./gradlew :sync:connectedAndroidTest
./gradlew :data:connectedAndroidTest

# Todos los tests
./gradlew test
```

## Dependencias de Test

```gradle
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
testImplementation("org.mockito:mockito-core:5.3.1")
testImplementation("androidx.test:core:1.5.0")  // Android
testImplementation("androidx.room:room-testing:2.5.2")  // Room
```

## Casos de Test Clave

### 1. Flujo de Envío (OUTBOUND)
```
mensaje = SyncMessage(type="request", status=PENDING)
queue.enqueue(mensaje)
→ Room.insert(PENDING)
→ SharedFlow.emit()
client.send()
→ queue.markSent()
→ Room.update(SENT)
```

### 2. Flujo de Recepción (INBOUND)
```
server → "{...json...}"
client.onMessage(json)
→ repository.handleIncomingMessage()
→ parse JSON → SyncMessage
→ queue.addIncoming()
→ Room.insert(RECEIVED, INBOUND)
→ auto-ACK enviado
```

### 3. Recuperación de Pendientes
```
getPending()
→ Room.query(status=PENDING)
→ reenviar cada uno
→ Room.update(SENT)
```

## Cobertura Esperada

- Entidades/Modelos: 95%+
- LocalSocketClient: 60% (conexión real requiere servidor)
- SyncRepository: 80%+
- SyncMessageQueue: 90%+
- Bidireccional: 85%+

**Total: ~80% de cobertura en lógica crítica**
