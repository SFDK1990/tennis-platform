#!/usr/bin/env python3
"""Generates the ten MAS Tennis Academy logo concepts.

    pip install -r requirements.txt
    python build.py

Writes, for every concept, a symbol ("mark", square canvas, the favicon/app-icon source) and a
lockup (symbol plus name) in three colourways: blue on light, solid black, solid white for dark
backgrounds. Units are arbitrary; the geometry is designed on a cap height of 100.
"""

import math
import os

from geometry import (
    Font,
    arc_to,
    bounds,
    circle,
    diff,
    dilate,
    erode,
    fetch_fonts,
    inter,
    move,
    outline,
    polygon,
    polyline,
    rect,
    rrect,
    shear_x,
    stroke,
    tapered,
    tennis_ball,
    union,
    write_svg,
)

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "conceptos")
FONTS = os.path.join(HERE, ".fonts")

# Hard-court palette. "a" is the structural colour, "b" the accent.
NAVY = "#0B1E3F"  # the outer run-off of a blue hard court, almost black
COURT = "#1F5FB8"  # the playing surface
COLOURWAYS = {
    "blue": {"a": NAVY, "b": COURT},
    "black": {"a": "#000000", "b": "#000000"},
    "white": {"a": "#FFFFFF", "b": "#FFFFFF"},
}

NAME = "MARCOS ASENCIO SCATTOLO"


def load_fonts():
    fetch_fonts(FONTS)
    f = lambda name, w=None: Font(os.path.join(FONTS, name), w)  # noqa: E731
    return {
        "mont500": f("Montserrat[wght].ttf", 500),
        "mont600": f("Montserrat[wght].ttf", 600),
        "mont700": f("Montserrat[wght].ttf", 700),
        "mont800": f("Montserrat[wght].ttf", 800),
        "corm500": f("CormorantGaramond[wght].ttf", 500),
        "corm600": f("CormorantGaramond[wght].ttf", 600),
        "corm700": f("CormorantGaramond[wght].ttf", 700),
        "barlow": f("BarlowCondensed-Medium.ttf"),
        "barlowIt": f("BarlowCondensed-SemiBoldItalic.ttf"),
        "out400": f("Outfit[wght].ttf", 400),
        "out500": f("Outfit[wght].ttf", 500),
        "out700": f("Outfit[wght].ttf", 700),
    }


# --- layout helpers ---------------------------------------------------------------------------


def line(font, text, size, role="a", tracking=0, gap=0, fit=None):
    """One line of a text block. `fit` stretches the tracking until the line is that wide."""
    if fit is not None and len(text) > 1:
        natural = font.width(text, size, 0)
        tracking = (fit - natural) / (size / 1000 * (len(text) - 1))
    return dict(font=font, text=text, size=size, role=role, tracking=tracking, gap=gap)


def text_block(lines, x, top, align="left"):
    layers, y = [], top
    for ln in lines:
        y += ln["gap"] + ln["font"].cap * ln["size"]
        anchor = {"left": "start", "center": "middle"}[align]
        layers.append((ln["font"].text(ln["text"], ln["size"], x, y, ln["tracking"], anchor), ln["role"]))
    return layers, y - top


def block_width(lines):
    return max(ln["font"].width(ln["text"], ln["size"], ln["tracking"]) for ln in lines)


def lockup_side(mark, lines, gap):
    """Symbol on the left, text block to its right, both centred on the same axis."""
    x0, y0, x1, y1 = bounds(*[p for p, _ in mark])
    _, h = text_block(lines, 0, 0)
    text, _ = text_block(lines, x1 + gap, (y0 + y1) / 2 - h / 2)
    return mark + text


def lockup_stack(mark, lines, gap, align="center"):
    x0, y0, x1, y1 = bounds(*[p for p, _ in mark])
    x = (x0 + x1) / 2 if align == "center" else x0
    text, _ = text_block(lines, x, y1 + gap, align)
    return mark + text


def knockout(base, *holes):
    return diff(base, *holes)


def fit_into(layers, box):
    """Scales and centres layers into (x, y, w, h)."""
    x0, y0, x1, y1 = bounds(*[p for p, _ in layers])
    bx, by, bw, bh = box
    s = min(bw / (x1 - x0), bh / (y1 - y0))
    dx = bx + (bw - (x1 - x0) * s) / 2 - x0 * s
    dy = by + (bh - (y1 - y0) * s) / 2 - y0 * s
    return [(p.transform(s, 0, 0, s, dx, dy), r) for p, r in layers]


def filled_ranges(shape, x, y0, y1, step=0.1):
    """Vertical runs of `shape` along the line at x, top to bottom."""
    runs, start, y = [], None, y0
    while y <= y1:
        inside = shape.contains((x, y))
        if inside and start is None:
            start = y
        if not inside and start is not None:
            runs.append((start, y))
            start = None
        y += step
    return runs


def leftmost(shape, y, x0, x1, step=0.1):
    x = x0
    while x <= x1:
        if shape.contains((x, y)):
            return x
        x += step
    return None


# --- 01 Monogram: the ball is the crossbar ----------------------------------------------------


def ballbar_letters():
    w = 12
    band = rect(-50, 0, 400, 100)
    m = inter(polyline([(6, 100), (6, 0), (37, 64), (68, 0), (68, 100)], w), band)

    # Apex placed so the pointed tip overshoots the flat tops by 2 units, as round and pointed
    # letters must to look the same height as flat ones.
    half, ya = 35.0, 10.0
    for _ in range(50):
        alpha = math.atan(half / (100 - ya))
        ya = -2 + (w / 2) / math.sin(alpha)
    alpha = math.atan(half / (100 - ya))
    ax = 68 + 6 + 14 + (w / 2) / math.cos(alpha)
    apex = ax + half
    legs = [(apex - half * 1.2, ya + (100 - ya) * 1.2), (apex, ya), (apex + half * 1.2, ya + (100 - ya) * 1.2)]
    a = inter(polyline(legs, w), rect(-50, -10, 400, 110))

    yb = 66
    inner = half * (yb - ya) / (100 - ya) - (w / 2) / math.cos(alpha)
    ball = circle(apex, yb, inner - 4.2)

    r = 22.5
    sx = apex + half + (w / 2) / math.cos(alpha) + 14 + w / 2 + r

    def s_line(p):
        arc_to(p, sx, 5 + r, r, -35, -270, move_first=True)
        arc_to(p, sx, 5 + 3 * r, r, -90, 145)

    s = stroke(s_line, w)
    return [(union(m, a, s), "a"), (ball, "b")]


def c01(F):
    letters = ballbar_letters()
    x0, _, x1, _ = bounds(*[p for p, _ in letters])
    width = x1 - x0
    lockup = lockup_stack(
        letters,
        [
            line(F["mont600"], "TENNIS ACADEMY", 17, fit=width, gap=0),
            line(F["mont500"], NAME, 9.2, "b", fit=width, gap=11),
        ],
        gap=30,
    )
    tile = rrect(0, 0, 300, 300, 66)
    inside = fit_into(letters, (45, 45, 210, 210))
    mark = [(knockout(tile, *[p for p, _ in inside]), "b")]
    return mark, lockup


# --- 02 Monogram: seal ------------------------------------------------------------------------


def c02(F):
    ring = diff(circle(0, 0, 100), circle(0, 0, 62))
    top = F["mont600"].on_circle(NAME, 10.4, 77.4, -90, tracking=240)
    bottom = F["mont600"].on_circle("TENNIS ACADEMY", 10.4, 84.8, 90, tracking=240, bottom=True)
    dots = union(circle(-81.1, 0, 2.5), circle(81.1, 0, 2.5))
    ring = knockout(ring, top, bottom, dots)

    core = circle(0, 0, 58)
    mas = F["mont700"].text("MAS", 37, 0, 8, tracking=40, anchor="middle")
    net = rect(-24, 19, 48, 2.6)
    lockup = [(ring, "a"), (knockout(core, mas, net), "b")]

    disc = circle(0, 0, 100)
    big = F["mont700"].text("MAS", 62, 0, 14, tracking=40, anchor="middle")
    mark = [(knockout(disc, big, rect(-40, 33, 80, 4.4)), "a")]
    return mark, lockup


# --- 03 Trajectory: the S is the flight of the ball -------------------------------------------


def flight_s():
    r = 30.0
    upper, lower = (0.0, -r), (0.0, r)
    # Normal writing order: from the top-right terminal over the top, through the spine and
    # under the bottom. The width grows along the way, so the stroke reads as a ball speeding up.
    a_up = (-35.0, -270.0)
    a_lo = (-90.0, 150.0)
    len_up = abs(a_up[1] - a_up[0])
    len_lo = abs(a_lo[1] - a_lo[0])
    total = len_up + len_lo

    def point(t):
        d = t * total
        if d <= len_up:
            a, c = a_up[0] - d, upper
        else:
            a, c = a_lo[0] + (d - len_up), lower
        return c[0] + r * math.cos(math.radians(a)), c[1] + r * math.sin(math.radians(a))

    body = tapered(point, 2.4, 17.0, samples=360, ease=1.25)
    end = math.radians(a_lo[1])
    ex, ey = point(1.0)
    tx, ty = -math.sin(end), math.cos(end)
    rb = 10.5
    ball = circle(ex + tx * (4.5 + rb), ey + ty * (4.5 + rb), rb)
    return [(shear_x(body, 11), "a"), (shear_x(ball, 11), "b")]


def c03(F):
    s = fit_into(flight_s(), (0, 0, 200, 110))
    mark = s
    wordmark = F["mont700"].width("MAS", 56, 30)
    lockup = lockup_side(
        s,
        [
            line(F["mont700"], "MAS", 56, tracking=30),
            line(F["mont600"], "TENNIS ACADEMY", 12.6, fit=wordmark, gap=12),
            line(F["mont500"], NAME, 7.3, "b", fit=wordmark, gap=9),
        ],
        gap=26,
    )
    return mark, lockup


# --- 04 Trajectory: ball-tracking M -----------------------------------------------------------


def tracking_m():
    pts = [(0, 100), (0, 0), (42, 72), (84, 0), (84, 100)]
    widths = [5.0, 8.5, 12.0, 15.5]
    band = rect(-50, 0, 300, 100)
    segs = []
    for (p, q), w in zip(zip(pts, pts[1:]), widths):
        dx, dy = q[0] - p[0], q[1] - p[1]
        n = math.hypot(dx, dy)
        ux, uy = dx / n, dy / n
        # Ends that touch the top or the baseline run long and are cut flat by the band; the
        # ends at the inner vertex stay square to the stroke.
        ext_p = 20 if p[1] in (0, 100) else 0
        ext_q = 20 if q[1] in (0, 100) else 0
        a = (p[0] - ux * ext_p, p[1] - uy * ext_p)
        b = (q[0] + ux * ext_q, q[1] + uy * ext_q)
        segs.append(inter(polyline([a, b], w), band))
    # Each stroke gives way to the ones drawn after it: the latest, thickest stroke is the ball's
    # most recent position and stays whole.
    out = []
    for i, seg in enumerate(segs):
        later = union(*segs[i + 1 :]) if i + 1 < len(segs) else None
        out.append(diff(seg, dilate(later, 3.6)) if later is not None else seg)
    rb = 11.0
    ball = circle(84 + 15.5 / 2 + 5 + rb, 100 - rb, rb)
    return [(union(*out), "a"), (ball, "b")]


def c04(F):
    m = tracking_m()
    wordmark = F["out700"].width("MAS", 58, 20)
    lockup = lockup_side(
        m,
        [
            line(F["out700"], "MAS", 58, tracking=20),
            line(F["out500"], "TENNIS ACADEMY", 13.2, fit=wordmark, gap=12),
            line(F["out400"], NAME, 7.6, "b", fit=wordmark, gap=9),
        ],
        gap=28,
    )
    return m, lockup


# --- 05 Court geometry: letters painted as court lines ----------------------------------------


def court_letter(ch, x0, y0, w, h, lw):
    def hbar(y, xa, xb):
        return rect(xa, y, xb - xa, lw)

    def vbar(x, ya, yb):
        return rect(x, ya, lw, yb - ya)

    mid = y0 + (h - lw) / 2
    left, right, top, bottom = x0, x0 + w - lw, y0, y0 + h - lw
    if ch == "M":
        return union(hbar(top, x0, x0 + w), vbar(left, y0, y0 + h), vbar(x0 + (w - lw) / 2, y0, y0 + h * 0.62), vbar(right, y0, y0 + h))
    if ch == "A":
        return union(hbar(top, x0, x0 + w), hbar(mid, x0, x0 + w), vbar(left, y0, y0 + h), vbar(right, y0, y0 + h))
    if ch == "S":
        return union(
            hbar(top, x0, x0 + w),
            vbar(left, y0, mid + lw),
            hbar(mid, x0, x0 + w),
            vbar(right, mid, y0 + h),
            hbar(bottom, x0, x0 + w),
        )
    raise ValueError(ch)


def court_frame(w, h, inset=7, lw=4.5):
    return diff(rect(inset, inset, w - 2 * inset, h - 2 * inset), rect(inset + lw, inset + lw, w - 2 * (inset + lw), h - 2 * (inset + lw)))


def c05(F):
    # 240 x 110 is the proportion of a doubles court seen from above (23.77 m x 10.97 m).
    W, H, lw = 240.0, 110.0, 7.0
    lw_, lh, gap = 46.0, 52.0, 16.0
    x = (W - (3 * lw_ + 2 * gap)) / 2
    letters = [court_letter(ch, x + i * (lw_ + gap), (H - lh) / 2, lw_, lh, lw) for i, ch in enumerate("MAS")]
    court = knockout(rrect(0, 0, W, H, 8), court_frame(W, H), *letters)
    lockup = lockup_side(
        [(court, "b")],
        [
            line(F["barlow"], "MAS TENNIS ACADEMY", 31, tracking=70),
            line(F["barlow"], NAME, 15.4, "b", fit=F["barlow"].width("MAS TENNIS ACADEMY", 31, 70), gap=11),
        ],
        gap=24,
    )
    S = 110.0
    tile = knockout(rrect(0, 0, S, S, 10), court_frame(S, S), court_letter("M", 25, 26, 60, 58, 9))
    return [(tile, "b")], lockup


# --- 06 Court geometry: a court in perspective is an A ----------------------------------------


def perspective_a():
    apex, base = (60.0, 0.0), 112.0

    def side(foot_x, w0, w1):
        def curve(t):
            return apex[0] + (foot_x - apex[0]) * t * 1.08, apex[1] + (base - apex[1]) * t * 1.08

        return tapered(curve, w0, w1, samples=80)

    def x_at(foot_x, y):
        return apex[0] + (foot_x - apex[0]) * y / base

    clip = rect(-40, -40, 200, base + 40)
    lines = inter(union(side(0, 1.2, 9.0), side(120, 1.2, 9.0), side(16, 0.8, 5.6), side(104, 0.8, 5.6)), clip)

    # Seen from behind the near baseline, the far service box sits inside the counter of the A:
    # the service line and the centre line make the inverted T every player recognises.
    ys, yn = 40.0, 66.0
    service = rect(x_at(16, ys), ys - 1.5, x_at(104, ys) - x_at(16, ys), 3.0)
    centre = rect(60 - 1.3, ys, 2.6, yn - ys)
    lines = union(lines, service, centre)

    # The net overhangs the doubles lines, as the real one does to reach its posts.
    net = rect(x_at(0, yn) - 9, yn - 3.4, x_at(120, yn) - x_at(0, yn) + 18, 6.8)
    lines = diff(lines, dilate(net, 2.8))
    return [(lines, "a"), (net, "b")]


def c06(F):
    a = perspective_a()
    wordmark = F["mont700"].width("MAS", 56, 60)
    lockup = lockup_side(
        a,
        [
            line(F["mont700"], "MAS", 56, tracking=60),
            line(F["mont600"], "TENNIS ACADEMY", 12.4, fit=wordmark, gap=12),
            line(F["mont500"], NAME, 7.2, "b", fit=wordmark, gap=9),
        ],
        gap=30,
    )
    return a, lockup


# --- 07 Luxury: the net line ------------------------------------------------------------------


def crossbar(glyph):
    x0, y0, x1, y1 = glyph.bounds
    runs = filled_ranges(glyph, (x0 + x1) / 2, y0, y1)
    return runs[-1]


def net_line_word(F, size=110, reach=64, gap=5.0):
    f = F["corm500"]
    runs = f.glyph_runs("MAS", size, 60)
    word = f.text("MAS", size, 0, 0, 60)
    s = size / f.upm
    glyphs = [f._glyph(name, s, x, 0) for name, x, _ in runs]
    m, a, sg = glyphs
    ya, yb = crossbar(a)
    mx0, _, mx1, _ = m.bounds
    sx0, _, sx1, _ = sg.bounds
    net = union(
        rect(mx0 - reach, ya, reach - gap, yb - ya),
        rect(mx1 + gap, ya, sx0 - gap - mx1 - gap, yb - ya),
        rect(sx1 + gap, ya, reach - gap, yb - ya),
    )
    net = diff(net, dilate(union(m, sg), gap))
    return [(net, "b"), (word, "a")]


def c07(F):
    word = net_line_word(F)
    x0, _, x1, _ = bounds(*[p for p, _ in word])
    lockup = lockup_stack(
        word,
        [
            line(F["corm600"], NAME, 15.5, fit=x1 - x0 - 40, gap=0),
            line(F["mont500"], "TENNIS ACADEMY", 7.6, "b", fit=(x1 - x0) * 0.42, gap=12),
        ],
        gap=28,
    )

    f = F["corm500"]
    a = f.text("A", 96, 0, 34, anchor="middle")
    ya, yb = crossbar(a)
    ring = diff(circle(0, 0, 66), circle(0, 0, 63.6))
    inner = 63.6 - 6
    half = math.sqrt(inner**2 - ((ya + yb) / 2) ** 2)
    net = diff(rect(-half, ya, 2 * half, yb - ya), rect(-200, -200, 0.01, 0.01))
    mark = [(net, "b"), (union(ring, a), "a")]
    return mark, lockup


# --- 08 Luxury: club crest --------------------------------------------------------------------


def shield(w=100.0, h=124.0):
    from pathops import Path

    p = Path()
    p.moveTo(0, 0)
    p.lineTo(w, 0)
    p.lineTo(w, h * 0.52)
    p.cubicTo(w, h * 0.76, w * 0.78, h * 0.9, w / 2, h)
    p.cubicTo(w * 0.22, h * 0.9, 0, h * 0.76, 0, h * 0.52)
    p.close()
    return p


def c08(F):
    base = shield()
    inset = erode(base, 5.4)
    border = diff(inset, erode(inset, 1.5))
    ball = tennis_ball(50, 30, 11, 1.9)
    mas = F["corm700"].text("MAS", 37, 50, 79, tracking=50, anchor="middle")
    rule = rect(40, 88, 20, 1.5)
    crest = knockout(base, border, ball, mas, rule)
    lockup = lockup_stack(
        [(crest, "a")],
        [
            line(F["corm600"], "MAS TENNIS ACADEMY", 19, tracking=160, gap=0),
            line(F["mont500"], NAME, 7.4, "b", fit=F["corm600"].width("MAS TENNIS ACADEMY", 19, 160), gap=12),
        ],
        gap=22,
    )
    m = F["corm700"].text("M", 74, 50, 84, anchor="middle")
    small = knockout(base, border, m)
    return [(small, "a")], lockup


# --- 09 High performance: speed cuts ----------------------------------------------------------


def speed_word(F, size=130):
    f = F["barlowIt"]
    word = f.text("MAS", size, 0, 0, tracking=10)
    t = 5.0
    bands = [(-30.0, 88.0), (-16.0, 58.0)]
    cuts, trails = [], []
    for y, length in bands:
        cuts.append(rect(-500, y - t / 2, 1500, t))
        x = leftmost(word, y, -50, 200)
        # Trails lean with the italic (about 12 degrees) so their ends look cut by the same blade.
        trail = shear_x(rect(x - 9 - length, y - t / 2, length, t), 12, pivot_y=y)
        trails.append(trail)
    return [(diff(word, *cuts), "a"), (union(*trails), "b")]


def c09(F):
    word = speed_word(F)
    x0, _, x1, _ = bounds(word[0][0])
    lockup = lockup_stack(
        word,
        [
            line(F["barlowIt"], "TENNIS ACADEMY", 25, fit=x1 - x0, gap=0),
            line(F["barlowIt"], NAME, 13, "b", fit=x1 - x0, gap=9),
        ],
        gap=18,
        align="left",
    )
    lockup = [(move(p, dx=0), r) for p, r in lockup]
    f = F["barlowIt"]
    m = f.text("M", 128, 0, 0, anchor="middle")
    mx0, my0, mx1, my1 = m.bounds
    m = move(m, dy=-(my0 + my1) / 2)
    cuts = [rect(-200, y - 2.8, 400, 5.6) for y in (22,)]
    tile = shear_x(rect(-66, -58, 132, 116), 12)
    mark = [(knockout(tile, diff(m, *cuts)), "b")]
    return mark, lockup


# --- 10 High performance: two peaks -----------------------------------------------------------


def rise_m():
    w = 18.0
    clip = rect(-100, -100, 400, 200)
    c1 = inter(polyline([(0, 120), (38, 28), (76, 120)], w), clip)
    c2 = inter(polyline([(76, 120), (114, 0), (152, 120)], w), clip)
    c1 = inter(c1, rect(-100, -100, 400, 200))
    base = rect(-100, -100, 400, 200)
    c1, c2 = inter(c1, base), inter(c2, base)
    c1 = diff(c1, dilate(c2, 4.5))
    t1 = c1.bounds
    t2 = c2.bounds
    rb = 11.0
    ball = circle(t2[2] - 4, t2[1] + 2 - rb * 0.2 - 16, rb)
    return [(c1, "b"), (c2, "a"), (ball, "b")], (t1, t2)


def c10(F):
    layers, _ = rise_m()
    clip = rect(-100, -100, 400, 200)
    layers = [(inter(p, clip), r) for p, r in layers]
    wordmark = F["mont800"].width("MAS", 60, 10)
    lockup = lockup_side(
        layers,
        [
            line(F["mont800"], "MAS", 60, tracking=10),
            line(F["barlow"], "TENNIS ACADEMY", 17, fit=wordmark, gap=10),
            line(F["barlow"], NAME, 10, "b", fit=wordmark, gap=7),
        ],
        gap=24,
    )
    return layers, lockup


CONCEPTS = [
    ("01-monogram-ballbar", "Ball-Bar Monogram", c01),
    ("02-monogram-seal", "Seal Monogram", c02),
    ("03-trajectory-flight-s", "Flight S", c03),
    ("04-trajectory-tracking-m", "Tracking M", c04),
    ("05-court-line-letters", "Court-Line Letters", c05),
    ("06-court-perspective-a", "Perspective Court A", c06),
    ("07-luxury-net-line", "The Net Line", c07),
    ("08-luxury-crest", "Club Crest", c08),
    ("09-performance-speed-cut", "Speed Cut", c09),
    ("10-performance-two-peaks", "Two Peaks", c10),
]


def main():
    F = load_fonts()
    for slug, title, build in CONCEPTS:
        folder = os.path.join(OUT, slug)
        os.makedirs(folder, exist_ok=True)
        mark, lockup = build(F)
        for way, colours in COLOURWAYS.items():
            name = f"MAS Tennis Academy - {title}"
            write_svg(os.path.join(folder, f"mark-{way}.svg"), [(p, colours[r]) for p, r in mark], f"{name} (symbol, {way})", square=True)
            write_svg(os.path.join(folder, f"lockup-{way}.svg"), [(p, colours[r]) for p, r in lockup], f"{name} ({way})")
        print("built", slug)


if __name__ == "__main__":
    main()
