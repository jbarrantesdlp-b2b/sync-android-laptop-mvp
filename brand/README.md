# Sync Engine — icon factory

Genera todos los iconos de Android, widgets y Electron a partir de los
masters oficiales de Barrantes Co.

## Una sola orden

```bash
python3 scripts/generate-icons.py
```

Requisitos: Python 3.10+, Pillow, numpy.

## Dónde poner tus PNG oficiales

| Archivo | Qué es | Tamaño ideal |
|---|---|---|
| `brand/source/mark.png` | SO 3D (sin texto) | 1024² o más |
| `brand/source/mono.png` | Line-art blanco/negro | 512² |
| `brand/source/maskable.png` | Recorte circular | 512² |

Si sueltas un PNG nuevo en `mark.png` y vuelves a correr el script,
se regeneran launcher, adaptive, Play Store, widgets, `icon.ico` y lockup.

`Paletadelogos.jpg` se guarda como referencia. El script también puede
recortar esa lámina (`brand/out/_board/`) si faltara un master.

## Qué escribe

- Paleta del board: 48 · 96 · 192 · 512 · 1024 + lockup + maskable + mono
- Android adaptive (safe zone 20 %) + mipmaps mdpi→xxxhdpi + monochrome
- Widgets Glance `ic_sync_engine_mark`
- Electron `icon.png` / `icon.ico` + `assets/mark.png`

Colores: OLED `#050811` · plasma `#00BFFF` · blanco `#FFFFFF`.
