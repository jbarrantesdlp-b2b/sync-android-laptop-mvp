# Sync Engine — icon factory

Dos masters oficiales, y nada más:

| Archivo | Uso |
|---|---|
| `brand/source/lockup.png` | Logo **completo**: SO + SYNC ENGINE + BY BARRANTES CO. |
| `brand/source/mark.png` | Logo **reducido**: solo el SO 3D (launcher, widgets, chrome) |

```bash
python3 scripts/generate-icons.py
```

Requisitos: Python 3.10+, Pillow, numpy.

Si reemplazas cualquiera de esos dos PNG y vuelves a correr el script,
se regeneran launcher adaptive, mipmaps, widgets, lockup in-app, `icon.ico`
y el kit en `brand/out/`.

El lockup **no se redibuja**. Se usa tu archivo. El mark reducido alimenta
todos los tamaños pequeños (48 · 96 · 192 · 512 · 1024) y el icono de Android.

Colores: OLED `#050811` · plasma `#00BFFF` · blanco `#FFFFFF`.
