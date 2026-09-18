# Especificación de Contratos y Protocolos de Red

Este documento define la única fuente de verdad para la interoperabilidad entre Android y Windows Desktop.

---

## 1. Conexión WebSocket
- **Puerto Desktop Server:** `8123`
- **URL por defecto:** `ws://<ip_laptop>:8123` o `ws://localhost:8123`
- **Formato de Mensaje JSON (`SyncMessage`):**
```json
{
  "id": "uuid-v4-string",
  "type": "sync_request" | "sync_response" | "sync_ack" | "device_info" | "heartbeat",
  "payload": "string-o-json-escapado",
  "timestamp": 1726532400000,
  "direction": "OUTBOUND" | "INBOUND",
  "status": "PENDING" | "SENT" | "RECEIVED" | "FAILED"
}
```

---

## 2. Auto-Discovery en Red Local

### Canal A: Multicast / Broadcast UDP
- **Puerto UDP:** `41234`
- **Dirección Multicast:** `239.55.55.1`
- **Magic Header:** `SYNC_ENGINE`
- **Payload de anuncio:**
```json
{
  "magic": "SYNC_ENGINE",
  "name": "Laptop Host Name",
  "port": 8123,
  "ip": "192.168.1.X"
}
```

### Canal B: mDNS / DNS-SD (Bonjour)
- **Tipo de servicio:** `_syncengine._tcp.`
- **Nombre:** `SyncEngine-Host`
- **Puerto:** `8123`
- **TXT Record:** `path=/`, `v=1.0`

### Canal C: Bluetooth Low Energy (BLE)
- **Manufacturer ID:** `0xFFFF`
- **Prefijo:** `SE`
- **Payload:** Código codificado con dirección IPv4 y puerto para escaneo rápido offline.

---

## 3. Códigos QR de Respaldo
- **Formato:** `syncengine://connect?ip=<IP>&port=8123`
