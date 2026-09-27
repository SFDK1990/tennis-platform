"""Vector primitives for the logo concepts.

Every shape is a skia-pathops Path and every overlap is resolved with boolean operations, so
the SVGs that come out are plain filled outlines: no strokes, no masks, no live text. That is
what a printer, an embroidery digitiser or a vinyl cutter expects, and it is why a "knockout"
here is a real hole instead of a white shape that would show up on a coloured background.
"""

import io
import math
import os
import urllib.request

import pathops as po
import uharfbuzz as hb
from fontTools.pens.recordingPen import DecomposingRecordingPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

# Bezier handle length that makes four cubics indistinguishable from a circle.
KAPPA = 0.5522847498

_CAPS = {"butt": po.LineCap.BUTT_CAP, "round": po.LineCap.ROUND_CAP, "square": po.LineCap.SQUARE_CAP}
_JOINS = {"miter": po.LineJoin.MITER_JOIN, "round": po.LineJoin.ROUND_JOIN, "bevel": po.LineJoin.BEVEL_JOIN}


# --- boolean operations -----------------------------------------------------------------------


def union(*paths):
    out = po.Path()
    for p in paths:
        if p is not None:
            out = po.op(out, p, po.PathOp.UNION)
    return out


def diff(a, *bs):
    for b in bs:
        a = po.op(a, b, po.PathOp.DIFFERENCE)
    return a


def inter(a, b):
    return po.op(a, b, po.PathOp.INTERSECTION)


def outline(p, width):
    """A band of `width` centred on the contours of a filled shape."""
    ring = po.Path()
    p.draw(ring.getPen())
    ring.stroke(width, po.LineCap.BUTT_CAP, po.LineJoin.MITER_JOIN, 4)
    ring.convertConicsToQuads()
    ring.simplify()
    return ring


def erode(p, amount):
    return diff(p, outline(p, 2 * amount))


def dilate(p, amount):
    """Grows a shape by `amount` on every side; used to open the gaps between overlapping parts."""
    ring = po.Path()
    p.draw(ring.getPen())
    ring.stroke(2 * amount, po.LineCap.ROUND_CAP, po.LineJoin.ROUND_JOIN, 4)
    # Round joins come back as conics, which the boolean operations do not accept.
    ring.convertConicsToQuads()
    return union(p, ring)


def move(p, dx=0.0, dy=0.0, s=1.0):
    return p.transform(s, 0, 0, s, dx, dy)


def shear_x(p, angle_deg, pivot_y=0.0):
    """Italic slant: positive angles lean the top of the shape to the right (y grows downwards)."""
    t = math.tan(math.radians(angle_deg))
    return p.transform(1, 0, -t, 1, t * pivot_y, 0)


def bounds(*paths):
    boxes = [p.bounds for p in paths if p is not None and len(list(p.contours))]
    return (
        min(b[0] for b in boxes),
        min(b[1] for b in boxes),
        max(b[2] for b in boxes),
        max(b[3] for b in boxes),
    )


# --- primitive shapes -------------------------------------------------------------------------


def polygon(points):
    p = po.Path()
    p.moveTo(*points[0])
    for pt in points[1:]:
        p.lineTo(*pt)
    p.close()
    return p


def rect(x, y, w, h):
    return polygon([(x, y), (x + w, y), (x + w, y + h), (x, y + h)])


def rrect(x, y, w, h, r):
    k = r * (1 - KAPPA)
    p = po.Path()
    p.moveTo(x + r, y)
    p.lineTo(x + w - r, y)
    p.cubicTo(x + w - k, y, x + w, y + k, x + w, y + r)
    p.lineTo(x + w, y + h - r)
    p.cubicTo(x + w, y + h - k, x + w - k, y + h, x + w - r, y + h)
    p.lineTo(x + r, y + h)
    p.cubicTo(x + k, y + h, x, y + h - k, x, y + h - r)
    p.lineTo(x, y + r)
    p.cubicTo(x, y + k, x + k, y, x + r, y)
    p.close()
    return p


def arc_to(p, cx, cy, r, a0, a1, move_first=False):
    """Appends a circular arc from angle a0 to a1 (degrees, y down, either direction)."""
    steps = max(1, math.ceil(abs(a1 - a0) / 90))
    da = (a1 - a0) / steps
    start = (cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0)))
    if move_first:
        p.moveTo(*start)
    for i in range(steps):
        t0 = math.radians(a0 + i * da)
        t1 = math.radians(a0 + (i + 1) * da)
        h = 4 / 3 * math.tan((t1 - t0) / 4) * r
        p.cubicTo(
            cx + r * math.cos(t0) - h * math.sin(t0),
            cy + r * math.sin(t0) + h * math.cos(t0),
            cx + r * math.cos(t1) + h * math.sin(t1),
            cy + r * math.sin(t1) - h * math.cos(t1),
            cx + r * math.cos(t1),
            cy + r * math.sin(t1),
        )


def circle(cx, cy, r):
    p = po.Path()
    arc_to(p, cx, cy, r, 0, 360, move_first=True)
    p.close()
    return p


def ellipse(cx, cy, rx, ry):
    return move(circle(0, 0, 1).transform(rx, 0, 0, ry, 0, 0), cx, cy)


def stroke(build, width, cap="butt", join="miter", miter_limit=40):
    """Outlines an open path. `build` receives an empty Path and draws the centre line."""
    p = po.Path()
    build(p)
    # Path.stroke replaces the centre line with its outline in place.
    p.stroke(width, _CAPS[cap], _JOINS[join], miter_limit)
    p.convertConicsToQuads()
    p.simplify()
    return p


def polyline(points, width, **kw):
    def build(p):
        p.moveTo(*points[0])
        for pt in points[1:]:
            p.lineTo(*pt)

    return stroke(build, width, **kw)


def tapered(curve, w0, w1, samples=240, ease=1.0):
    """Filled outline of a centre line whose width goes from w0 to w1.

    `curve(t)` returns the point at t in [0, 1]. The outline is a dense polygon: at the sizes a
    logo is ever printed the facets stay far below what anyone can see.
    """
    pts = [curve(i / samples) for i in range(samples + 1)]
    left, right = [], []
    for i, (x, y) in enumerate(pts):
        ax, ay = pts[max(i - 1, 0)]
        bx, by = pts[min(i + 1, samples)]
        dx, dy = bx - ax, by - ay
        n = math.hypot(dx, dy) or 1
        nx, ny = -dy / n, dx / n
        w = (w0 + (w1 - w0) * (i / samples) ** ease) / 2
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    shape = polygon(left + right[::-1])
    shape.simplify()
    return shape


def tennis_ball(cx, cy, r, seam):
    """A ball with its two seam curves cut out, as one filled shape."""
    s = r * 1.02
    seams = union(
        inter(stroke(lambda p: arc_to(p, cx - r * 1.38, cy, s, -60, 60, True), seam), circle(cx, cy, r)),
        inter(stroke(lambda p: arc_to(p, cx + r * 1.38, cy, s, 120, 240, True), seam), circle(cx, cy, r)),
    )
    return diff(circle(cx, cy, r), seams)


# --- type -------------------------------------------------------------------------------------

FONT_SOURCES = {
    "Montserrat[wght].ttf": "ofl/montserrat/Montserrat%5Bwght%5D.ttf",
    "CormorantGaramond[wght].ttf": "ofl/cormorantgaramond/CormorantGaramond%5Bwght%5D.ttf",
    "BarlowCondensed-Medium.ttf": "ofl/barlowcondensed/BarlowCondensed-Medium.ttf",
    "BarlowCondensed-SemiBoldItalic.ttf": "ofl/barlowcondensed/BarlowCondensed-SemiBoldItalic.ttf",
    "Outfit[wght].ttf": "ofl/outfit/Outfit%5Bwght%5D.ttf",
}


def fetch_fonts(folder):
    """Downloads the OFL fonts from the google/fonts repository the first time they are needed."""
    os.makedirs(folder, exist_ok=True)
    for name, remote in FONT_SOURCES.items():
        target = os.path.join(folder, name)
        if not os.path.exists(target):
            urllib.request.urlretrieve("https://raw.githubusercontent.com/google/fonts/main/" + remote, target)


class Font:
    def __init__(self, path, wght=None):
        tt = TTFont(path)
        if wght is not None and "fvar" in tt:
            # Variable fonts draw a glyph as overlapping strokes. Removing the overlaps is what
            # lets text take part in knockouts without leaving slivers where contours cross.
            tt = instancer.instantiateVariableFont(tt, {"wght": wght}, overlap=instancer.OverlapMode.REMOVE)
        buf = io.BytesIO()
        tt.save(buf)
        data = buf.getvalue()
        self.tt = TTFont(io.BytesIO(data))
        self.glyphs = self.tt.getGlyphSet()
        self.order = self.tt.getGlyphOrder()
        self.upm = self.tt["head"].unitsPerEm
        self.cap = self.tt["OS/2"].sCapHeight / self.upm
        self.hb = hb.Font(hb.Face(data))

    def _glyph(self, name, s, x, y):
        rec = DecomposingRecordingPen(self.glyphs)
        self.glyphs[name].draw(rec)
        p = po.Path()
        rec.replay(TransformPen(p.getPen(), (s, 0, 0, -s, x, y)))
        return p

    def glyph_runs(self, text, size, tracking=0):
        """[(glyph name, x offset, advance)] in output units, kerning applied by HarfBuzz."""
        buf = hb.Buffer()
        buf.add_str(text)
        buf.guess_segment_properties()
        hb.shape(self.hb, buf, {"kern": True, "liga": False})
        s = size / self.upm
        runs, x = [], 0.0
        for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
            runs.append((self.order[info.codepoint], x + pos.x_offset * s, pos.x_advance * s))
            x += pos.x_advance * s + tracking * size / 1000
        return runs

    def width(self, text, size, tracking=0):
        runs = self.glyph_runs(text, size, tracking)
        name, x, adv = runs[-1]
        return x + adv

    def text(self, text, size, x=0.0, y=0.0, tracking=0, anchor="start"):
        """Outlines `text` with its baseline at y. Tracking is in thousandths of an em."""
        w = self.width(text, size, tracking)
        x0 = x - {"start": 0, "middle": w / 2, "end": w}[anchor]
        s = size / self.upm
        out = po.Path()
        for name, gx, _ in self.glyph_runs(text, size, tracking):
            out = union(out, self._glyph(name, s, x0 + gx, y))
        return out

    def on_circle(self, text, size, radius, center_deg, tracking=0, bottom=False):
        """Sets text along a circle centred on the origin.

        By default the letters stand on the circle reading clockwise (top of a seal); with
        bottom=True they hang from it towards the centre reading anticlockwise (bottom of a
        seal), so both halves read left to right the right way up.
        """
        runs = self.glyph_runs(text, size, tracking)
        total = runs[-1][1] + runs[-1][2]
        s = size / self.upm
        out = po.Path()
        for name, gx, adv in runs:
            if name == "space":
                continue
            offset = (gx + adv / 2 - total / 2) / radius
            glyph = self._glyph(name, s, -adv / 2, 0)
            if bottom:
                ang = math.radians(center_deg) - offset
                rot = ang - math.pi / 2
            else:
                ang = math.radians(center_deg) + offset
                rot = ang + math.pi / 2
            c, sn = math.cos(rot), math.sin(rot)
            px, py = radius * math.cos(ang), radius * math.sin(ang)
            out = union(out, glyph.transform(c, sn, -sn, c, px, py))
        return out


# --- output -----------------------------------------------------------------------------------


def _num(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def path_data(p):
    pen = SVGPathPen(None, ntos=_num)
    p.draw(pen)
    return pen.getCommands()


def write_svg(filename, layers, title, pad=0.08, square=False):
    """Writes [(Path, colour)] as one SVG, merging layers that end up with the same colour."""
    merged = {}
    for p, colour in layers:
        merged[colour] = union(merged.get(colour, po.Path()), p)
    x0, y0, x1, y1 = bounds(*merged.values())
    w, h = x1 - x0, y1 - y0
    if square:
        side = max(w, h)
        x0, y0, w, h = x0 - (side - w) / 2, y0 - (side - h) / 2, side, side
    m = pad * max(w, h)
    vb = [x0 - m, y0 - m, w + 2 * m, h + 2 * m]
    body = "\n".join(f'  <path fill="{c}" d="{path_data(p)}"/>' for c, p in merged.items())
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{" ".join(_num(v) for v in vb)}" '
        f'role="img" aria-labelledby="t">\n  <title id="t">{title}</title>\n{body}\n</svg>\n'
    )
    with open(filename, "w", encoding="utf-8") as fh:
        fh.write(svg)
