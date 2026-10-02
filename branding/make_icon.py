"""
Cuadre logo: a rubber "paid" stamp — rounded square frame + bold check, slightly rotated,
with uneven rubber-stamp ink. Redrawn at high resolution from the 48px concept
(branding/concept_48px.png) so it stays crisp at every launcher size.

Outputs (deterministic, seed fixed):
  branding/cuadre_mark_1024.png      transparent mark, master
  branding/cuadre_icon_1024.png      mark on cream, square (store / docs)
  branding/play_store_512.png        Play Store listing icon
  app/src/main/res/mipmap-*/ic_launcher_foreground.png   adaptive icon foreground (108dp canvas)
  app/src/main/res/mipmap-*/ic_launcher_monochrome.png   themed icon (Android 13+)

Run from the repo root:  python branding/make_icon.py
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
TERRACOTTA = (180, 83, 42)   # #B4532A, CuadreColors.primary
CREAM = (242, 230, 213)      # #F2E6D5, sampled from the concept
SEED = 7
ROTATION = -9                # degrees, the "slammed by hand" tilt


def draw_mark(size: int) -> Image.Image:
    """Clean vector-like mark on a transparent canvas (alpha = ink)."""
    s = size * 4  # supersample
    ink = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(ink)
    side = s * 0.74
    x0 = (s - side) / 2
    stroke = s * 0.085
    radius = s * 0.07
    d.rounded_rectangle([x0, x0, x0 + side, x0 + side], radius=radius, outline=255, width=int(stroke))
    # Check: short leg down-right, long leg up-right, chunky with round joints.
    w = int(s * 0.115)
    p1 = (s * 0.31, s * 0.53)
    p2 = (s * 0.45, s * 0.665)
    p3 = (s * 0.70, s * 0.33)
    d.line([p1, p2, p3], fill=255, width=w, joint="curve")
    for (cx, cy) in (p1, p3):
        d.ellipse([cx - w / 2, cy - w / 2, cx + w / 2, cy + w / 2], fill=255)
    ink = ink.rotate(ROTATION, resample=Image.BICUBIC, center=(s / 2, s / 2))
    return ink.resize((size, size), Image.LANCZOS)


def stamp_texture(alpha: Image.Image, seed: int = SEED) -> Image.Image:
    """Rubber-stamp ink: rough edges, uneven density, a few dry gaps."""
    size = alpha.size[0]
    rnd = random.Random(seed)

    def noise(scale: float, blur: float) -> Image.Image:
        small = max(8, int(size / scale))
        n = Image.new("L", (small, small))
        n.putdata([rnd.randint(0, 255) for _ in range(small * small)])
        return n.resize((size, size), Image.BICUBIC).filter(ImageFilter.GaussianBlur(blur))

    # Rough edges: blur the shape, shift it by centred noise (-128..+127), re-threshold.
    # Only the soft edge band (~128) moves; the solid interior (255) stays solid.
    soft = alpha.filter(ImageFilter.GaussianBlur(size * 0.006))
    edge_noise = noise(size / 160, size * 0.0015)
    rough = ImageChops.add(soft, edge_noise, scale=1.0, offset=-128)
    rough = rough.point(lambda v: 255 if v > 128 else 0)

    # Dry gaps: a few small blotches where ink didn't transfer.
    speck = noise(size / 70, size * 0.002).point(lambda v: 0 if v > 214 else 255)
    # Uneven density: ink 82–100%.
    density = noise(size / 14, size * 0.012).point(lambda v: int(210 + v * 45 / 255))

    out = ImageChops.multiply(rough, speck)
    out = ImageChops.multiply(out, density)
    return out.filter(ImageFilter.GaussianBlur(size * 0.0008))


def colored(alpha: Image.Image, rgb) -> Image.Image:
    img = Image.new("RGBA", alpha.size, rgb + (0,))
    img.putalpha(alpha)
    return img


def on_background(mark: Image.Image, bg, scale: float) -> Image.Image:
    size = mark.size[0]
    canvas = Image.new("RGBA", (size, size), bg + (255,))
    inner = mark.resize((int(size * scale),) * 2, Image.LANCZOS)
    off = (size - inner.size[0]) // 2
    canvas.alpha_composite(inner, (off, off))
    return canvas


def main() -> None:
    master_alpha = stamp_texture(draw_mark(1024))
    mark = colored(master_alpha, TERRACOTTA)
    out = ROOT / "branding"
    mark.save(out / "cuadre_mark_1024.png")
    on_background(mark, CREAM, 0.92).convert("RGB").save(out / "cuadre_icon_1024.png")
    on_background(mark, CREAM, 0.92).convert("RGB").resize((512, 512), Image.LANCZOS).save(out / "play_store_512.png")

    # Adaptive icon: 108dp canvas, only the central 66dp circle is guaranteed visible.
    # The rotated stamp's corners must stay inside it -> mark spans ~61% of the canvas so the circle mask never clips the stamp corners.
    densities = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    for name, px in densities.items():
        d = ROOT / "app/src/main/res" / f"mipmap-{name}"
        d.mkdir(parents=True, exist_ok=True)
        fg = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        inner_px = int(px * 0.61)
        inner = mark.resize((inner_px, inner_px), Image.LANCZOS)
        fg.alpha_composite(inner, ((px - inner_px) // 2,) * 2)
        fg.save(d / "ic_launcher_foreground.png")
        # Themed icon: the system tints the alpha; keep it the same shape, solid ink.
        mono = colored(fg.getchannel("A"), (0, 0, 0))
        mono.save(d / "ic_launcher_monochrome.png")
    print("ok")


if __name__ == "__main__":
    main()
