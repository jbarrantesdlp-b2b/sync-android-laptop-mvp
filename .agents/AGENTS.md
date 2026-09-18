# SYNC ENGINE - Multi-Agent Architecture & Governance

Bienvenido al sistema de desarrollo orquestado para **SYNC ENGINE by Barrantes Co.** (`sync-android-laptop-mvp`).

Este repositorio opera bajo una arquitectura de **Agentes Especializados** coordinados de manera estricta e indispensable por el **Agente Orquestador**.

---

## 👑 El Agente Orquestador (Indispensable)

El Agente Orquestador es la máxima autoridad técnica y de producto en el repositorio. Ningún cambio que afecte contratos entre plataformas o dependencias entre módulos puede ser ejecutado sin su aprobación y mediación.

### Responsabilidades Clave:
1. **Custodio de Contratos:** Mantiene y congela las especificaciones técnicas en `.agents/rules/contracts.md` (puertos, esquemas JSON, protocolos UDP/mDNS/BLE).
2. **Secuenciador de Dependencias:** Descompone los requerimientos en tareas atómicas y las delega en el orden estricto de dependencias:
   $$\text{Contrato} \rightarrow \text{Data / Persistence} \rightarrow \text{Sync / Network} \rightarrow \text{UI / Widgets} \rightarrow \text{Desktop Electron} \rightarrow \text{QA Verification}$$
3. **Calidad y Verificación Global:** Ejecuta los pipelines de prueba (`./gradlew test`, verificación de compilación de recursos AAPT y builds de Electron).
4. **Interlocutor Único con el Usuario:** Traduce las peticiones del usuario a especificaciones de ingeniería y presenta resúmenes claros sin ruido interno.

---

## 🤖 Squad de Agentes Especializados

| Agente | Skill Asociada | Alcance (Scope) | Rol Principal |
| :--- | :--- | :--- | :--- |
| 🎨 **Brand Creative Director & Lead UI/UX** | `brand-creative-director` | [`brand/`](../brand), [`app/ui/`](../app/src/main/java/com/example/syncapp/ui), [`widget/`](../widget), [`desktop-electron/styles.css`](../desktop-electron/styles.css) | **Dirección creativa integral:** Identidad de marca Barrantes Co., diseño OLED, Compose, Glance Widgets, Desktop UI, marketing, publicidad, copywriting y propuestas proactivas. |
| ⚡ **Sync & Network Engine** | `sync-network-protocol` | [`sync/`](../sync) | OkHttp WebSockets, reconexión exponencial, AutoDiscovery (Multicast UDP 239.55.55.1:41234, mDNS, BLE) y WorkManager. |
| 💾 **Data & Persistence** | `data-persistence-room` | [`data/`](../data), [`core/`](../core) | Room SQLite DB, DAOs, DataStore Preferences, colas de mensajes offline transaccionales e idempotencia. |
| 💻 **Desktop & Electron (Runtime)** | `desktop-electron-engine` | [`desktop-electron/`](../desktop-electron) | Node.js, Electron runtime, servidor WebSocket local (puerto 8123), Bonjour discovery, scripts PowerShell y empaquetado Windows. |
| 🛡️ **QA & Quality Gate** | `qa-quality-gate` | Todo el repositorio | Pruebas unitarias JUnit, validación de recursos AAPT, análisis estático y empaquetado de APKs y portables. |

---

## 📜 Reglas de Interacción entre Agentes
1. **Aislamiento de Dominio:** Ningún agente especializado debe modificar archivos fuera de su ámbito sin la intervención y coordinación del Orquestador.
2. **Contratos Inmutables:** Los modelos de datos compartidos (`SyncMessage`, enums de conexión) deben actualizarse en paralelo en Android y Desktop bajo la supervisión del Orquestador.
3. **Integridad de Recursos Android:** Cualquier recurso visual añadido a `res/drawable*` debe ser validado por el Agente QA antes de su inclusión para evitar archivos corruptos o extensiones falsas.
