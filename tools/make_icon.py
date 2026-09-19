"""
Regenerate the MiniMusic launcher and widget marks from the vinyl artwork.

    python3 tools/make_icon.py

Source artwork (kept out of the app's resource tree so it is never packaged):

    tools/artwork/ic_launcher_foreground_master.png   1920x1920, the vinyl tile
                                                      inset in a black frame

Derived resources it writes:

    app/src/main/res/drawable-nodpi/ic_launcher_vinyl_foreground.png
        Adaptive-icon foreground. The artwork cropped to its tile, refilled to
        the full canvas and slightly overscaled — the layer *is* the mask, so
        no launcher shape can reach the artwork's black frame — with the frame
        keyed out to transparency. 1024px, 8-bit palette: the composition is
        flat greys, so the reduction is invisible and it keeps the release APK
        from growing by the couple of MB an uncompressed 1920px RGBA copy adds.

    app/src/main/res/drawable-nodpi/ic_launcher_vinyl_glyph.png
        White-on-transparent vinyl — disc, two groove cuts, floating spindle —
        for the themed (monochrome) icon.

    app/src/main/res/drawable-nodpi/ic_launcher_vinyl_widget.png
        The same glyph drawn at the visual weight the widget's idle slot used
        before, so the home-screen widget keeps its proportions.

    app/src/main/res/mipmap-*/ic_launcher.png, ic_launcher_round.png
        Legacy density icons. Unused at minSdk 26 but kept consistent so
        nothing in the build resolves to a stale mark.

The tile geometry is measured from the artwork rather than eyeballed: the disc
is 0.3203 of the canvas in radius, the label cut-out 0.340 of the disc radius,
the spindle 0.107, and the two groove cuts 0.53-0.60 and 0.79-0.85.
"""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ARTWORK = os.path.join(ROOT, "tools", "artwork", "ic_launcher_foreground_master.png")

# Fractions of the disc radius, measured off the artwork.
LABEL_CUT = 0.340
SPINDLE = 0.107
GROOVE_RINGS = [(0.53, 0.60), (0.79, 0.85)]

# Disc diameter as a fraction of the glyph's own canvas.
GLYPH_DISC_FRAC = 0.58
# ...and for the widget, matched to the mark the widget used to draw.
WIDGET_DISC_FRAC = 0.39

SS = 2  # supersampling for the glyph


def key_out_frame(master):
    """Transparent over the artwork's black frame, opaque over the tile."""
    frame = master.convert("L").point(lambda v: 255 if v < 24 else 0)
    ImageDraw.floodfill(frame, (0, 0), 128)
    for corner in ((1919, 0), (0, 1919), (1919, 1919)):
        if frame.getpixel(corner) == 255:
            ImageDraw.floodfill(frame, corner, 128)
    mask = frame.point(lambda v: 0 if v == 128 else 255).filter(ImageFilter.GaussianBlur(0.8))
    out = master.convert("RGBA")
    out.putalpha(mask)
    return out


def reframe(fg, size, overscale=1.06):
    """Crop to the tile and refill the canvas.

    An adaptive-icon layer is the mask, so the layer itself should be the tile:
    cropping to it means no launcher shape can reach the artwork's black frame,
    and the overscale gives the mask an edge to bite into.
    """
    tile = fg.crop(fg.getchannel("A").getbbox())
    side = int(round(size * overscale))
    tile = tile.resize((side, side), Image.LANCZOS)
    off = (side - size) // 2
    return tile.crop((off, off, off + size, off + size))


def make_glyph(size, disc_frac):
    """White vinyl glyph on transparency: disc, groove cuts, spindle dot."""
    n = size * SS
    yy, xx = np.mgrid[0:n, 0:n]
    c = (n - 1) / 2.0
    r = np.hypot(xx - c, yy - c)
    R = n * disc_frac / 2.0

    covered = r <= R                       # the disc
    covered &= r >= LABEL_CUT * R          # minus the label hole
    for lo, hi in GROOVE_RINGS:
        covered &= ~((r >= lo * R) & (r <= hi * R))
    covered |= r <= SPINDLE * R            # spindle dot

    alpha = Image.fromarray((covered * 255).astype(np.uint8), "L").resize((size, size), Image.LANCZOS)
    glyph = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    return Image.composite(Image.new("RGBA", (size, size), (255, 255, 255, 255)), glyph, alpha)


def main():
    master = Image.open(ARTWORK).convert("RGB")

    fg = reframe(key_out_frame(master), size=1024)
    fg = fg.quantize(colors=256, method=Image.FASTOCTREE, dither=Image.FLOYDSTEINBERG)
    fg.save(os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_foreground.png"), optimize=True)
    check = fg.convert("RGBA")
    n = check.size[0] - 1
    print("foreground", fg.size, "corner alpha",
          [check.getpixel(p)[3] for p in ((0, 0), (n, 0), (0, n), (n, n))],
          "centre alpha", check.getpixel((n // 2, n // 2))[3])

    make_glyph(1920, GLYPH_DISC_FRAC).save(
        os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_glyph.png"))
    make_glyph(1920, WIDGET_DISC_FRAC).save(
        os.path.join(RES, "drawable-nodpi/ic_launcher_vinyl_widget.png"))
    print("glyphs written")

    for density, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)):
        master.resize((px, px), Image.LANCZOS).save(
            os.path.join(RES, f"mipmap-{density}/ic_launcher.png"))

        big = master.resize((px * 4, px * 4), Image.LANCZOS).convert("RGBA")
        mask = Image.new("L", (px * 4, px * 4), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
        big.putalpha(mask)
        big.resize((px, px), Image.LANCZOS).save(
            os.path.join(RES, f"mipmap-{density}/ic_launcher_round.png"))
    print("legacy mipmaps written")


if __name__ == "__main__":
    main()
