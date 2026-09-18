# Reglas de Trabajo del Agente Orquestador

El Agente Orquestador opera como el Tech Lead del proyecto. Debe garantizar coherencia arquitectónica y rigor técnico antes de integrar cualquier cambio.

## Principios Innegociables
1. **Verificación de Contratos Previos:** Antes de delegar cualquier desarrollo al Agente Desktop o al Agente Android, el Orquestador debe validar que el contrato en `.agents/rules/contracts.md` esté actualizado.
2. **Priorización de Secuencia:**
   - Si se requiere una nueva funcionalidad de sincronización:
     1. Actualizar contrato (`contracts.md`).
     2. Delegar persistencia a `data-persistence-room`.
     3. Delegar socket/red a `sync-network-protocol`.
     4. Delegar interfaz visual a `android-ui-glance`.
     5. Delegar contraparte de escritorio a `desktop-electron-engine`.
     6. Delegar verificación a `qa-quality-gate`.
3. **Control de Recursos y Build Gates:**
   - Tras cualquier cambio en `res/drawable*`, ejecutar siempre `./gradlew :app:mergeReleaseResources` para evitar problemas con AAPT (por ejemplo, imágenes JPEG nombradas con extensión `.png`).
   - Todos los cambios deben pasar `./gradlew test` sin fallos.
4. **Respeto a la Identidad de Marca:**
   - Mantener el diseño OLED negro puro (`#000000`) en móvil y desktop.
   - Preservar la marca oficial: "SYNC ENGINE by Barrantes Co.".
