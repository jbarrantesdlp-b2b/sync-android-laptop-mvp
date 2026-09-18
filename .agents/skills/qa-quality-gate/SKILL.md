---
name: qa-quality-gate
description: Guía especializada para el Agente QA & Quality Gate. Cubre suites de prueba unitarias en Gradle, validación de recursos de Android con AAPT, y verificación de empaquetado.
---

# Skill: QA & Quality Gate Specialist

Este rol gobierna la calidad, integración continua y verificación del software.

## Directrices Técnicas

### 1. Batería de Pruebas Unitarias
Ejecución obligatoria antes de cualquier commit o integración:
```bash
# Todos los tests
./gradlew test

# Tests por módulo crítico
./gradlew :sync:test
./gradlew :data:test
```

### 2. Validación de Recursos Android (AAPT Guard)
Para evitar que se introduzcan archivos con extensiones incorrectas (como imágenes JPEG con extensión `.png`):
```bash
./gradlew :app:mergeReleaseResources --dry-run
./gradlew :app:mergeReleaseResources
```
Validar que todos los archivos en `res/drawable*` sean formatos válidos (PNG de 8-byte header `137 80 78 71 13 10 26 10` o XML vectoriales).

### 3. Generación de APK de Release
- Script de copia y nombrado: [`CopyAPK.ps1`](../../../CopyAPK.ps1).
- Generación de build release:
```bash
./gradlew assembleRelease
```
