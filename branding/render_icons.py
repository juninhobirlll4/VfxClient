"""Renderiza os SVGs de branding/svg em PNG/ICO.

Uso (uma vez):  pip install playwright pillow  &&  python -m playwright install chromium
Depois:         python branding/render_icons.py

Gera branding/generated/* e copia os ícones para os módulos core e launcher.
"""
from pathlib import Path
import shutil

from PIL import Image
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
SVG = ROOT / "branding" / "svg"
OUT = ROOT / "branding" / "generated"
SIZES = [16, 32, 48, 64, 128, 256, 512]


def render(page, svg_path: Path, width: int, height: int, dest: Path) -> None:
    svg = svg_path.read_text(encoding="utf-8")
    page.set_viewport_size({"width": width, "height": height})
    page.set_content(
        "<html><body style='margin:0;background:transparent'>"
        f"<div style='width:{width}px;height:{height}px'>{svg}</div></body></html>"
    )
    page.eval_on_selector("svg", f"e => {{ e.setAttribute('width', {width}); e.setAttribute('height', {height}); }}")
    page.locator("svg").screenshot(path=str(dest), omit_background=True)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        for size in SIZES:
            render(page, SVG / "logo.svg", size, size, OUT / f"vfxclient-{size}.png")
        render(page, SVG / "wordmark.svg", 1024, 256, OUT / "wordmark.png")
        browser.close()

    big = Image.open(OUT / "vfxclient-256.png").convert("RGBA")
    big.save(OUT / "vfxclient.ico", sizes=[(s, s) for s in (16, 24, 32, 48, 64, 128, 256)])

    shutil.copy(OUT / "vfxclient-128.png", ROOT / "core/src/main/resources/assets/vfx/icon.png")
    shutil.copy(OUT / "wordmark.png", ROOT / "core/src/main/resources/assets/vfx/textures/gui/wordmark.png")
    shutil.copy(OUT / "vfxclient-256.png", ROOT / "launcher/src/main/resources/com/vfxclient/launcher/logo-256.png")
    print("Ícones gerados em", OUT)


if __name__ == "__main__":
    main()
