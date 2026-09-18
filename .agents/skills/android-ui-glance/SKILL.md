---
name: android-ui-glance
description: Guía especializada para el Agente Android UI & Glance. Cubre Jetpack Compose, Material3, Jetpack Glance widgets (1x1, 2x2, 4x2), diseño OLED negro puro y wallpapers de marca.
---

# Skill: Android UI & Glance Specialist

Este rol gobierna la capa visual y de experiencia de usuario en los módulos `:app` y `:widget`.

## Directrices Técnicas

### 1. Jetpack Glance (Widgets Reactivos)
- Ubicación: `widget/src/main/java/com/example/syncwidget/`
- Arquitectura:
  - Clase principal: `SyncGlanceWidget` con `SizeMode.Responsive` (CompactClock 1x1, MediumStatus 2x2, LargeHero 4x2).
  - Pinning al launcher: Usar `WidgetPinner.pin()` para anclar widgets programáticamente.
  - Actualizaciones en tiempo real: `WidgetUpdateObserver` con debounce para no saturar el sistema operativo.
  - Periodic Fallback: `WidgetUpdateWorker` mediante WorkManager.
- Restricciones críticas en Glance:
  - Usar únicamente componentes de Glance (`androidx.glance.*`), NUNCA componentes estándar de Compose (`androidx.compose.material3.*`) dentro de los composables del Widget.
  - Utilizar `GlanceModifier` y no `Modifier`.

### 2. UI Principal en Jetpack Compose
- Ubicación: `app/src/main/java/com/example/syncapp/ui/`
- Estética obligatoria:
  - Fondo negro puro OLED (`#000000`).
  - Acentos de neón / cian / violeta acordes al `ThemePack`.
  - Iconografía corporativa: Usar los drawables de marca oficiales (`sync_engine_lockup`, `sync_engine_mark`).

### 3. Wallpapers de Sistema
- Integración en `BrandWallpaper.kt`:
  - `WallpaperManager.FLAG_SYSTEM` (pantalla de inicio).
  - `WallpaperManager.FLAG_LOCK` (pantalla de bloqueo).
