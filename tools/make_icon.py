"""
Regenerate the MiniMusic launcher and widget marks from the vinyl artwork.

    python3 tools/make_icon.py

Source artwork (kept out of the app's resource tree so it is never packaged):

    tools/artwork/ic_launcher_foreground_master.png   1920x1920, the vinyl record
                                                      photographed on a grey tile

The icon is an adaptive icon, so it is two layers:

    background   drawable/ic_launcher_background.xml — a hand-written vector, the
                 cream field. It has to reach every edge of the 108dp layer, so
                 it is a gradient rather than artwork; nothing of the source
                 image's square tile survives into the icon.

    foreground   drawable-nodpi/ic_launcher_vinyl_foreground.png — the record cut
                 from the artwork as a circle, centred on the layer, with a soft
                 shadow. Its diameter is 0.60 of the layer: the mask shows the
                 centre 72dp of 108, and the guaranteed-visible safe circle is
                 66dp (61%), so the record sits inside the safe circle with room
                 to spare and cannot be clipped by any launcher mask.

Also written:

    drawable-nodpi/ic_launcher_vinyl_glyph.png    white monochrome vinyl for the
                                                  themed icon (v33)
    drawable-nodpi/ic_launcher_vinyl_splash.png   the record alone, large, for the
                                                  splash screen, which masks the
                                                  drawable it is given
    drawable-nodpi/ic_launcher_vinyl_widget.png   the mark at the widget's weight

The record's geometry is measured from the artwork rather than eyeballed: centre
(960, 943) and radius 623px in the 1920px master — note the centre is 17px above
the tile's own centre, which is what made the earlier icon read tilted.
"""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ARTWORK = os.path.join(ROOT, "tools", "artwork", "ic_launcher_foreground_master.png")

SIZE = 1024                                   # layer canvas; 108dp at ~9.5x
DISC_CX, DISC_CY, DISC_R = 960.0, 943.0, 623.0

# Fractions of the layer. 0.60 keeps the record inside the 66dp safe circle.
RECORD_FRAC = 0.60
SPLASH_FRAC = 0.92
GLYPH_DISC_FRAC = 0.58
WIDGET_DISC_FRAC = 0.39

# The drawn mark's internal proportions, as fractions of the disc radius.
LABEL_CUT = 0.340
SPINDLE = 0.107
GROOVE_RINGS = [(0.53, 0.60), (0.79, 0.85)]

SS = 2


def cut_record(diameter_frac):
    """The record from the artwork, cropped and masked in master coordinates."""
    src = Image.open(ARTWORK).convert("RGB")
    pad = 6
    box = (int(DISC_CX - DISC_R - pad), int(DISC_CY - DISC_R - pad),
           int(DISC_CX + DISC_R + pad), int(DISC_CY + DISC_R + pad))
    crop = src.crop(box)

    n = crop.size[0]
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    c = (n - 1) / 2.0
    r = np.hypot(xx - c, yy - c)
    # Mask to the record's radius, not the crop's: masking to the crop would
    # leave a ring of the artwork's light tile around the record.
    alpha = np.clip(DISC_R + 0.5 - r, 0, 1)
    disc = crop.convert("RGBA")
    disc.putalpha(Image.fromarray((alpha * 255).astype(np.uint8), "L"))

    side = int(round(n * (diameter_frac * SIZE) / (2.0 * DISC_R)))
    disc = disc.resize((side, side), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    off = (SIZE - side) // 2
    out.paste(disc, (off, off), disc)
    return out


def record_foreground():
    """The record, centred, with the shadow the artwork implies."""
    d = int(RECORD_FRAC * SIZE)
    shadow = Image.new("L", (SIZE, SIZE), 0)
    x = (SIZE - d) // 2 - int(0.012 * SIZE)
    y = (SIZE - d) // 2 + int(0.015 * SIZE)
    ImageDraw.Draw(shadow).ellipse((x, y, x + d, y + d), fill=int(255 * 0.26))
    shadow = shadow.filter(ImageFilter.GaussianBlur(int(0.026 * SIZE)))

    layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    layer = Image.composite(Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255)), layer, shadow)
    layer.alpha_composite(cut_record(RECORD_FRAC))
    return layer


def glyph(size, disc_frac):
    """White vinyl on transparency: disc, groove cuts, spindle dot."""
    n = size * SS
    yy, xx = np.mgrid[0:n, 0:n]
    c = (n - 1) / 2.0
    r = np.hypot(xx - c, yy - c)
    R = n * disc_frac / 2.0

    covered = r <= R
    covered &= r >= LABEL_CUT * R
    for lo, hi in GROOVE_RINGS:
        covered &= ~((r >= lo * R) & (r <= hi * R))
    covered |= r <= SPINDLE * R

    alpha = Image.fromarray((covered * 255).astype(np.uint8), "L").resize((size, size), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    return Image.composite(Image.new("RGBA", (size, size), (255, 255, 255, 255)), out, alpha)


def save_png8(img, path):
    """256-colour PNG. The compositions are flat greys, so this is invisible and
    keeps the release APK from carrying megabytes of RGBA."""
    img.convert("RGBA").quantize(colors=256, method=Image.FASTOCTREE,
                                 dither=Image.FLOYDSTEINBERG).save(path, optimize=True)


def main():
    save_png8(record_foreground(),
              os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_foreground.png"))
    print("foreground: record at %.2f of the layer" % RECORD_FRAC)

    save_png8(cut_record(SPLASH_FRAC),
              os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_splash.png"))
    print("splash: record at %.2f of the drawable" % SPLASH_FRAC)

    glyph(1920, GLYPH_DISC_FRAC).save(
        os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_glyph.png"))
    glyph(1920, WIDGET_DISC_FRAC).save(
        os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_widget.png"))
    print("glyphs written")


if __name__ == "__main__":
    main()
