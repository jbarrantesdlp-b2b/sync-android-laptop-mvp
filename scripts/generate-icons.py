#!/usr/bin/env python3
"""Sync Engine icon factory.

Reads the official masters in brand/source/ (or extracts them from
brand/Paletadelogos.jpg) and writes every size the Android launcher,
Glance widgets and Electron desktop app need.

Palette sizes from the Barrantes Co. brand board:
  48 · 96 · 192 · 512 · 1024   + maskable + monochrome

Usage (from repo root):
  python3 scripts/generate-icons.py
  python3 scripts/generate-icons.py --source brand/source/mark.png
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

OLED = (5, 8, 17, 255)
CYAN = (0, 191, 255, 255)
WHITE = (255, 255, 255, 255)
MUTED = (226, 232, 240, 230)

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

PALETTE_SIZES = (48, 96, 192, 512, 1024)
ELECTRON_ICO_SIZES = (16, 24, 32, 48, 64, 128, 256)
FONT_BOLD = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
FONT_REG = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"

BOARD_CROPS = {
    "mark": (0.337, 0.139, 0.613, 0.634),
    "lockup": (0.093, 0.104, 0.303, 0.536),
    "mono": (0.708, 0.664, 0.809, 0.889),
    "maskable": (0.847, 0.670, 0.962, 0.911),
}


def repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def load_rgba(path: Path) -> Image.Image:
    return Image.open(path).convert("RGBA")


def flood_knockout(im: Image.Image, luma: int = 28, chroma: int = 16) -> Image.Image:
    """Make the flat OLED backdrop transparent. Keeps plasma / metal."""
    arr = np.array(im.convert("RGBA"))
    h, w = arr.shape[:2]
    rgb = arr[:, :, :3].astype(np.int16)
    lum = (0.2126 * rgb[:, :, 0] + 0.7152 * rgb[:, :, 1] + 0.0722 * rgb[:, :, 2])
    spread = rgb.max(axis=2) - rgb.min(axis=2)
    is_bg = (lum <= luma) & (spread <= chroma)

    visited = np.zeros((h, w), dtype=bool)
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        if is_bg[0, x]:
            q.append((x, 0))
        if is_bg[h - 1, x]:
            q.append((x, h - 1))
    for y in range(h):
        if is_bg[y, 0]:
            q.append((0, y))
        if is_bg[y, w - 1]:
            q.append((w - 1, y))

    while q:
        x, y = q.popleft()
        if x < 0 or y < 0 or x >= w or y >= h or visited[y, x] or not is_bg[y, x]:
            continue
        visited[y, x] = True
        arr[y, x, 3] = 0
        q.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))

    # Feather the cut so JPEG ringing does not leave a dark halo.
    alpha = arr[:, :, 3].astype(np.float32)
    for _ in range(2):
        padded = np.pad(alpha, 1, mode="edge")
        neigh = (
            padded[:-2, 1:-1]
            + padded[2:, 1:-1]
            + padded[1:-1, :-2]
            + padded[1:-1, 2:]
        ) / 4.0
        alpha = np.where(alpha == 0, np.minimum(neigh, 40), alpha)
    arr[:, :, 3] = alpha.astype(np.uint8)
    return Image.fromarray(arr)


def trim(im: Image.Image, padding: int = 8) -> Image.Image:
    bbox = im.split()[-1].getbbox()
    if not bbox:
        return im
    l, t, r, b = bbox
    l = max(0, l - padding)
    t = max(0, t - padding)
    r = min(im.width, r + padding)
    b = min(im.height, b + padding)
    return im.crop((l, t, r, b))


def fit_square(im: Image.Image, size: int, bg: tuple[int, int, int, int] | None, safe: float = 0.20) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), bg if bg is not None else (0, 0, 0, 0))
    inner = max(1, int(size * (1.0 - 2.0 * safe)))
    src = im.copy()
    src.thumbnail((inner, inner), Image.Resampling.LANCZOS)
    x = (size - src.width) // 2
    y = (size - src.height) // 2
    canvas.alpha_composite(src, (x, y))
    return canvas


def circle_mask(size: int) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    ImageDraw.Draw(m).ellipse((0, 0, size - 1, size - 1), fill=255)
    return m


def rounded_mask(size: int, radius: float = 0.22) -> Image.Image:
    m = Image.new("L", (size, size), 0)
    r = int(size * radius)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size - 1, size - 1), radius=r, fill=255)
    return m


def apply_mask(im: Image.Image, mask: Image.Image) -> Image.Image:
    out = im.copy()
    a = out.split()[-1]
    out.putalpha(Image.fromarray(np.minimum(np.array(a), np.array(mask.convert("L")))))
    return out


def to_white_silhouette(im: Image.Image) -> Image.Image:
    arr = np.array(im.convert("RGBA"))
    lum = 0.2126 * arr[:, :, 0] + 0.7152 * arr[:, :, 1] + 0.0722 * arr[:, :, 2]
    alpha = arr[:, :, 3].astype(np.float32) * np.clip(lum / 180.0, 0.0, 1.0)
    out = np.zeros_like(arr)
    out[:, :, 0] = 255
    out[:, :, 1] = 255
    out[:, :, 2] = 255
    out[:, :, 3] = np.clip(alpha, 0, 255).astype(np.uint8)
    return Image.fromarray(out)


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


def draw_tracked(draw: ImageDraw.ImageDraw, text: str, y: int, font_obj, fill, tracking: float, canvas_w: int) -> None:
    widths = [font_obj.getlength(ch) + tracking for ch in text]
    total = sum(widths) - tracking
    x = (canvas_w - total) / 2.0
    for ch, w in zip(text, widths):
        draw.text((x, y), ch, font=font_obj, fill=fill)
        x += w


def make_lockup(mark: Image.Image, size: int = 1024) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), OLED)
    mark_area = int(size * 0.62)
    fitted = fit_square(mark, mark_area, bg=None, safe=0.04)
    canvas.alpha_composite(fitted, ((size - mark_area) // 2, int(size * 0.06)))
    draw = ImageDraw.Draw(canvas)
    title = font(FONT_BOLD, max(18, int(size * 0.072)))
    sub = font(FONT_BOLD, max(10, int(size * 0.028)))
    draw_tracked(draw, "SYNC ENGINE", int(size * 0.72), title, CYAN, size * 0.012, size)
    draw_tracked(draw, "BY BARRANTES CO.", int(size * 0.84), sub, MUTED, size * 0.018, size)
    return canvas


def write_png(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.suffix.lower() != ".png":
        path = path.with_suffix(".png")
    im.save(path, "PNG")
    try:
        print(f"  {path.relative_to(repo_root())}")
    except ValueError:
        print(f"  {path}")


def extract_from_board(board: Path, dest: Path) -> None:
    src = load_rgba(board)
    w, h = src.size
    dest.mkdir(parents=True, exist_ok=True)
    for name, (l, t, r, b) in BOARD_CROPS.items():
        crop = src.crop((int(l * w), int(t * h), int(r * w), int(b * h)))
        write_png(crop, dest / f"{name}_boardcrop.png")


def write_ico(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    master = fit_square(im, 256, OLED, safe=0.18)
    master.save(
        path,
        format="ICO",
        sizes=[(s, s) for s in ELECTRON_ICO_SIZES],
    )
    try:
        print(f"  {path.relative_to(repo_root())}")
    except ValueError:
        print(f"  {path}")


def write_xml(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")
    try:
        print(f"  {path.relative_to(repo_root())}")
    except ValueError:
        print(f"  {path}")


def generate_preview_html(out: Path, files: list[tuple[str, str]]) -> None:
    cards = "\n".join(
        f'<figure><img src="{src}" alt="{label}"><figcaption>{label}</figcaption></figure>'
        for label, src in files
    )
    html = f"""<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8"/>
  <title>SYNC ENGINE — icon kit</title>
  <style>
    :root {{ color-scheme: dark; }}
    body {{
      margin: 0; font-family: ui-sans-serif, system-ui, sans-serif;
      background: #050811; color: #E2E8F0;
    }}
    header {{
      padding: 28px 32px 8px; display: flex; gap: 16px; align-items: center;
    }}
    header img {{ width: 72px; height: 72px; }}
    h1 {{ margin: 0; letter-spacing: 3px; font-size: 22px; }}
    p {{ margin: 4px 0 0; color: #94A3B8; font-size: 12px; letter-spacing: 1.4px; }}
    .grid {{
      display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 18px; padding: 24px 32px 48px;
    }}
    figure {{
      margin: 0; background: #0C1220; border: 1px solid #1E293B;
      border-radius: 18px; padding: 16px; text-align: center;
    }}
    img {{
      width: 100%; height: 140px; object-fit: contain; image-rendering: auto;
      background: #050811; border-radius: 12px;
    }}
    figcaption {{
      margin-top: 10px; font-size: 11px; letter-spacing: .4px; color: #7DD3FC;
    }}
  </style>
</head>
<body>
  <header>
    <img src="mark-512.png" alt="mark"/>
    <div>
      <h1>SYNC ENGINE</h1>
      <p>BY BARRANTES CO. · kit de iconos</p>
    </div>
  </header>
  <section class="grid">{cards}</section>
</body>
</html>
"""
    (out / "preview.html").write_text(html, encoding="utf-8")
    print("  brand/out/preview.html")


def generate(root: Path, source_mark: Path | None) -> None:
    brand = root / "brand"
    src_dir = brand / "source"
    out = brand / "out"
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)

    board = brand / "Paletadelogos.jpg"
    if board.exists():
        extract_from_board(board, out / "_board")

    mark_path = source_mark or (src_dir / "mark.png")
    if not mark_path.exists():
        sys.exit(f"missing master mark: {mark_path}")

    mark_raw = load_rgba(mark_path)
    mark = trim(flood_knockout(mark_raw), padding=12)

    mono_path = src_dir / "mono.png"
    mask_path = src_dir / "maskable.png"
    mono_raw = load_rgba(mono_path) if mono_path.exists() else None
    mask_raw = load_rgba(mask_path) if mask_path.exists() else None

    silhouette = to_white_silhouette(mark)
    lockup_path = src_dir / "lockup.png"
    if lockup_path.exists():
        official = load_rgba(lockup_path)
        lockup = Image.new("RGBA", (1024, 1024), OLED)
        fitted = official.copy()
        fitted.thumbnail((1024, 1024), Image.Resampling.LANCZOS)
        lockup.alpha_composite(fitted, ((1024 - fitted.width) // 2, (1024 - fitted.height) // 2))
    else:
        lockup = make_lockup(mark, 1024)

    print("\n[palette]")
    preview_cards: list[tuple[str, str]] = []
    for size in PALETTE_SIZES:
        bg = fit_square(mark, size, OLED, safe=0.20)
        write_png(bg, out / f"mark-{size}.png")
        preview_cards.append((f"mark {size}×{size}", f"mark-{size}.png"))
        if size <= 48:
            simple = fit_square(silhouette, size, OLED, safe=0.22)
            write_png(simple, out / f"mark-{size}-simple.png")

    write_png(lockup, out / "lockup-1024.png")
    write_png(lockup.resize((512, 512), Image.Resampling.LANCZOS), out / "lockup-512.png")
    preview_cards.append(("lockup 1024", "lockup-1024.png"))
    preview_cards.append(("lockup 512", "lockup-512.png"))

    maskable = fit_square(mark, 512, OLED, safe=0.22)
    maskable = apply_mask(maskable, circle_mask(512))
    write_png(maskable, out / "maskable-512.png")
    preview_cards.append(("maskable 512", "maskable-512.png"))

    if mono_raw is not None:
        mono_sheet = fit_square(trim(flood_knockout(mono_raw, luma=18, chroma=20), 6), 512, (0, 0, 0, 255), safe=0.02)
        write_png(mono_sheet, out / "mono-512.png")
        preview_cards.append(("mono sheet 512", "mono-512.png"))
    write_png(fit_square(silhouette, 512, (0, 0, 0, 0), safe=0.20), out / "mono-silhouette-512.png")
    preview_cards.append(("mono silhouette", "mono-silhouette-512.png"))

    generate_preview_html(out, preview_cards)

    print("\n[android launcher]")
    app_res = root / "app" / "src" / "main" / "res"
    for name, scale in DENSITIES.items():
        px = int(48 * scale)
        legacy = fit_square(mark, px, OLED, safe=0.18)
        round_ic = apply_mask(legacy.copy(), circle_mask(px))
        write_png(legacy, app_res / f"mipmap-{name}" / "ic_launcher.png")
        write_png(round_ic, app_res / f"mipmap-{name}" / "ic_launcher_round.png")

        fg_px = int(108 * scale)
        fg = fit_square(mark, fg_px, None, safe=0.20)
        write_png(fg, app_res / f"drawable-{name}" / "ic_launcher_foreground.png")

    write_png(fit_square(mark, 512, OLED, safe=0.18), app_res / "drawable-nodpi" / "sync_engine_mark.png")
    write_png(lockup.resize((512, 512), Image.Resampling.LANCZOS), app_res / "drawable-nodpi" / "sync_engine_lockup.png")
    write_png(fit_square(silhouette, 432, None, safe=0.22), app_res / "drawable-nodpi" / "ic_launcher_monochrome.png")
    write_png(fit_square(mark, 512, OLED, safe=0.18), app_res / "drawable-nodpi" / "ic_launcher-playstore.png")

    write_xml(
        app_res / "drawable" / "ic_launcher_background.xml",
        """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#050811" />
</shape>
""",
    )
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    write_xml(app_res / "mipmap-anydpi-v26" / "ic_launcher.xml", adaptive)
    write_xml(app_res / "mipmap-anydpi-v26" / "ic_launcher_round.xml", adaptive)

    fg_vector = app_res / "drawable" / "ic_launcher_foreground.xml"
    if fg_vector.exists():
        fg_vector.unlink()
        print("  removed app/.../ic_launcher_foreground.xml")

    print("\n[android widgets]")
    widget_res = root / "widget" / "src" / "main" / "res"
    for name, scale in DENSITIES.items():
        px = int(48 * scale)
        write_png(fit_square(mark, px, None, safe=0.08), widget_res / f"drawable-{name}" / "ic_sync_engine_mark.png")
    write_png(fit_square(mark, 256, None, safe=0.08), widget_res / "drawable-nodpi" / "ic_sync_engine_mark.png")
    old_mark = widget_res / "drawable" / "ic_sync_engine_mark.xml"
    if old_mark.exists():
        old_mark.unlink()
        print("  removed widget/.../ic_sync_engine_mark.xml")

    print("\n[electron]")
    electron = root / "desktop-electron"
    write_png(fit_square(mark, 512, OLED, safe=0.18), electron / "icon.png")
    write_png(fit_square(mark, 256, None, safe=0.10), electron / "assets" / "mark.png")
    write_png(lockup.resize((512, 512), Image.Resampling.LANCZOS), electron / "assets" / "lockup.png")
    write_ico(mark, electron / "icon.ico")

    spec = {
        "safe_zone": 0.20,
        "colors": {"oled": "#050811", "cyan": "#00BFFF", "white": "#FFFFFF"},
        "palette_sizes": list(PALETTE_SIZES),
        "source": str(mark_path.relative_to(root)),
    }
    (brand / "icon-spec.json").write_text(json.dumps(spec, indent=2) + "\n", encoding="utf-8")
    print("\nOK — re-run after dropping a new PNG into brand/source/mark.png")


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Sync Engine icon set")
    parser.add_argument("--source", type=Path, default=None, help="Override brand/source/mark.png")
    parser.add_argument("--repo", type=Path, default=None)
    args = parser.parse_args()
    root = args.repo.resolve() if args.repo else repo_root()
    generate(root, args.source.resolve() if args.source else None)


if __name__ == "__main__":
    main()
