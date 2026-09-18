---
name: desktop-electron-engine
description: Guía especializada para el Agente Desktop & Electron. Cubre Node.js, arquitectura de Electron, servidor WebSocket ws, broadcast mDNS con Bonjour y scripts PowerShell en Windows.
---

# Skill: Desktop & Electron Specialist

Este rol gobierna la aplicación de escritorio en [`desktop-electron/`](../../../desktop-electron).

## Directrices Técnicas

### 1. Servidor WebSocket (`main.js`)
- Módulo: `ws` en puerto `8123`.
- Escucha en `0.0.0.0` para permitir conexiones desde la red Wi-Fi local.
- Manejo de echo y despacho de mensajes bidireccionales hacia el renderer vía Electron IPC.

### 2. Servicios de Descubrimiento (`discovery.js`)
- **UDP Broadcast & Multicast:** Enviar periódicamente paquetes en puerto `41234` con el payload de anuncio.
- **Bonjour / mDNS:** Registrar el servicio `_syncengine._tcp.` usando `bonjour-service`.
- **Bluetooth LE:** Invocar `ble-advertise.ps1` usando Windows Runtime API (`Windows.Devices.Bluetooth.Advertisement.BluetoothLEAdvertisementPublisher`).

### 3. Interfaz de Usuario y Empaquetado
- UI: HTML/CSS en modo OLED oscuro ([`styles.css`](../../../desktop-electron/styles.css)).
- Generación de QR dinámico con librería `qrcode` para fallback manual.
- Empaquetado: `npm run dist` utilizando `electron-builder` para generar el portable de Windows en `desktop-electron/dist/`.
