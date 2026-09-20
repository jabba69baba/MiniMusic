# -*- coding: utf-8 -*-
"""Gully Labs deck v3 — same minimal system, photo panel on EVERY slide + full-bleed cover (SlidesCarnival brown template)."""
import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from PIL import Image as PILImage

SW, SH = 13.3333, 7.5
M = 0.9
BG   = RGBColor(0xF4, 0xEF, 0xE6)
INK  = RGBColor(0x22, 0x1B, 0x15)
SOFT = RGBColor(0x4A, 0x44, 0x3C)
MUT  = RGBColor(0x8A, 0x81, 0x77)
LINE = RGBColor(0xCF, 0xC5, 0xB4)
BROWN= RGBColor(0x8C, 0x62, 0x48)
DISP = "Georgia"; BODY = "Calibri"
CR = 9.15            # content right edge
CW2 = CR - M         # 8.25 content width
PX, PY, PW, PH = 9.55, 2.0, 2.9, 4.7   # photo panel
ASSETS = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets"))


def new_deck():
    prs = Presentation(); prs.slide_width = Inches(SW); prs.slide_height = Inches(SH)
    return prs

def blank(prs):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    r = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(SW), Inches(SH))
    r.fill.solid(); r.fill.fore_color.rgb = BG; r.line.fill.background(); r.shadow.inherit = False
    return s

def rect(s, x, y, w, h, fill=None, line=None, lw=1.0, shape=MSO_SHAPE.RECTANGLE):
    e = s.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if fill is None: e.fill.background()
    else: e.fill.solid(); e.fill.fore_color.rgb = fill
    if line is None: e.line.fill.background()
    else: e.line.color.rgb = line; e.line.width = Pt(lw)
    e.shadow.inherit = False
    e.text_frame.word_wrap = True
    return e

def hair(s, x, y, w, col=LINE):
    rect(s, x, y, w, 0.011, fill=col)

def _setspc(r, v):
    rPr = r._r.get_or_add_rPr(); rPr.set('spc', str(int(v * 100)))

def tb(s, x, y, w, h, chunks, size=11, color=SOFT, bold=False, italic=False, align=PP_ALIGN.LEFT,
       anchor=MSO_ANCHOR.TOP, spacing=1.15, after=0, font=BODY, spc=None):
    b = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = b.text_frame; tf.word_wrap = True; tf.vertical_anchor = anchor
    tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
    if isinstance(chunks, str): chunks = [chunks]
    for i, par in enumerate(chunks):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align; p.line_spacing = spacing; p.space_after = Pt(after)
        if isinstance(par, str): par = [par]
        for ch in par:
            t, o = (ch, {}) if isinstance(ch, str) else ch
            r = p.add_run(); r.text = t
            r.font.name = o.get("font", font); r.font.size = Pt(o.get("size", size))
            r.font.bold = o.get("bold", bold); r.font.italic = o.get("italic", italic)
            r.font.color.rgb = o.get("color", color)
            sv = o.get("spc", spc)
            if sv: _setspc(r, sv)
    return b

def kick(s, x, y, text):
    tb(s, x, y, 11, 0.25, text.upper(), size=10, color=BROWN, bold=True, spc=2.5)

def folio(s, n):
    tb(s, M, 7.06, 7, 0.2, "GULLY LABS — CULTURE AS CAPITAL", size=8.5, color=MUT, spc=1.5)
    tb(s, SW - M - 1.4, 7.06, 1.4, 0.2, "%02d / 15" % n, size=8.5, color=MUT, align=PP_ALIGN.RIGHT, spc=1.5)

def head(s, n, k, t):
    kick(s, M, 0.72, k)
    tb(s, M, 0.98, 11.6, 0.8, t, size=29, color=INK, font=DISP)
    hair(s, M, 1.78, SW - 2 * M)
    folio(s, n)

def photoR(s, fname, cap):
    src = os.path.join(ASSETS, fname); dst = os.path.join(ASSETS, "_v3_" + fname)
    im = PILImage.open(src); W, H = im.size; tar = PW / PH; cur = W / H
    if cur > tar:
        nw = int(H * tar); im = im.crop(((W - nw) // 2, 0, (W - nw) // 2 + nw, H))
    else:
        nh = int(W / tar); im = im.crop((0, (H - nh) // 2, W, (H - nh) // 2 + nh))
    im.convert("RGB").save(dst, quality=90)
    rect(s, PX + 0.08, PY + 0.08, PW, PH, line=BROWN, lw=1.0)
    s.shapes.add_picture(dst, Inches(PX), Inches(PY), Inches(PW), Inches(PH))
    if cap:
        tb(s, PX, 6.78, PW + 0.4, 0.25, cap, size=7.6, color=MUT, spacing=1.0)

def notes(s, t):
    s.notes_slide.notes_text_frame.text = t


# ---------------------------------------------------------------- S1 COVER (full-bleed photo right)
def s01(prs):
    s = blank(prs)
    src = os.path.join(ASSETS, "gully-labs-sneakers-delhi-campaign-india-3.jpg")
    dst = os.path.join(ASSETS, "_v3_cover.jpg")
    im = PILImage.open(src); W, H = im.size; tar = 6.4333 / 7.5; cur = W / H
    if cur > tar:
        nw = int(H * tar); im = im.crop(((W - nw) // 2, 0, (W - nw) // 2 + nw, H))
    else:
        nh = int(W / tar); im = im.crop((0, (H - nh) // 2, W, (H - nh) // 2 + nh))
    im.convert("RGB").save(dst, quality=92)
    s.shapes.add_picture(dst, Inches(6.9), 0, Inches(6.4333), Inches(SH))
    rect(s, 6.98, 0.08, 6.27, 7.34, line=BROWN, lw=1.0)
    tb(s, M, 0.95, 5.6, 0.25, "A BRAND STUDY & STRATEGIC ANALYSIS · BUSINESS POLICY", size=10, color=BROWN, bold=True, spc=2.5)
    tb(s, M, 1.4, 6.0, 1.6, "Culture as capital.", size=44, color=INK, font=DISP)
    tb(s, M, 2.75, 5.6, 1.2, [
        [("How Gully Labs is trying to build India’s first globally aspirational sneaker house — ", {}),
         ("and whether the strategy can survive scale, cash and competition.", {"italic": True})]],
       size=12.5, color=MUT, font=DISP, spacing=1.3)
    hair(s, M, 4.3, 5.6)
    stats = [("Aug 2023", "founded, Delhi NCR"), ("₹4k–15k", "price band / pair"),
             ("₹175 Cr", "Shark Tank valuation"), ("~25%", "revenue from exports")]
    x = M
    for i, (v, l) in enumerate(stats):
        xx = M + (i % 2) * 2.9; yy = 4.45 + (i // 2) * 0.78
        tb(s, xx, yy, 2.7, 0.4, v, size=19, color=INK, font=DISP)
        tb(s, xx, yy + 0.4, 2.7, 0.3, l, size=8.3, color=MUT)
    hair(s, M, 6.05, 5.6)
    tb(s, M, 6.15, 5.6, 0.3, "AISHA · ROHAN · MEERA · KABIR", size=9.5, color=SOFT, spc=2)
    tb(s, M, 6.45, 5.6, 0.3, "September 2026 · 15 slides · 11 minutes · sources on the final slide", size=8.3, color=MUT)
    tb(s, M, 6.78, 5.6, 0.25, "Founders Arjun Singh & Animesh Mishra — “unapologetically Indian” (Inc42).", size=7.5, color=MUT)
    folio(s, 1)
    notes(s, "OPEN (Aisha, 30 sec). Do not read the title. Say: 'Nike sells a swoosh. Gully Labs sells a street "
             "in Delhi. Same category, different strategy question - can culture be a defensible advantage?' "
             "Point at the photo: two founders, one manifesto. Hand to slide 2.")


# ---------------------------------------------------------------- S2 roadmap
def s02(prs):
    s = blank(prs)
    head(s, 2, "Roadmap & speaker map", "One brand, five lenses, eleven minutes.")
    tb(s, M, 1.98, 8.25, 0.3, [
        [("Before we start — hands up if you own sneakers above ", {}), ("₹5,000", {"bold": True, "color": INK}),
         ("; keep them up if they are ", {}), ("not", {"italic": True, "color": INK}), (" a global brand.", {})]],
       size=11, color=SOFT)
    rows = [("01", "WHO & WHERE", "Aisha · 0:00–2:30", "S1–S5",
             "Cover · brand snapshot · mission, values, culture · PESTEL"),
            ("02", "THE ARENA", "Rohan · 2:30–5:15", "S6–S8",
             "Value chain · industry cycle · Porter · 4P · SWOT"),
            ("03", "THE PLAN", "Meera · 5:15–8:30", "S9–S11",
             "Directional strategy & levels · BCG + GE · implementation & blue ocean"),
            ("04", "THE VERDICT", "Kabir · 8:30–11:15", "S12–S15",
             "Balanced scorecard · control & contingency · verdict & recommendations")]
    y = 2.55
    for num, name, who, rng, desc in rows:
        tb(s, M, y, 0.8, 0.5, num, size=24, color=BROWN, font=DISP)
        tb(s, M + 0.85, y + 0.05, 3.4, 0.3, name, size=11.5, color=INK, bold=True, spc=2)
        tb(s, M + 0.85, y + 0.36, 4.6, 0.3, desc, size=8.8, color=MUT)
        tb(s, M + 5.5, y + 0.08, 1.9, 0.3, who, size=9, color=MUT)
        tb(s, M + 7.0, y + 0.02, 1.25, 0.4, rng, size=14, color=BROWN, font=DISP, italic=True, align=PP_ALIGN.RIGHT)
        hair(s, M, y + 0.76, CW2)
        y += 0.9
    tb(s, M, 6.28, 8.25, 0.3,
       "SYLLABUS — PESTEL · VALUE CHAIN · INDUSTRY CYCLE · PORTER · 4P · SWOT · DIRECTIONAL · LEVELS · BCG · GE · "
       "DESIGN THINKING · RED/BLUE OCEAN · MODEL INNOVATION · BSC · AUDIT · CONTINGENCY · CONTROLS",
       size=8.2, color=MUT, spc=1.1)
    tb(s, M, 6.62, 8.25, 0.4, [
        [("The one question we answer: ", {"italic": True, "color": BROWN}),
         ("is “unapologetically Indian” a strategy, or just a marketing line — and what would it take to make it a moat?",
          {"italic": True, "color": BROWN})]], size=10.5, font=DISP)
    photoR(s, "ph-gully-alley.jpg", "A gully in old Delhi — the brand’s namesake and archive.")
    notes(s, "Aisha, 45 sec. Run the show of hands FIRST, count out loud - that gap is the case. Then the four "
             "sections and who speaks when; do not read the syllabus line, say it is mapped in the written study. "
             "End on the italic question, read slowly.")


# ---------------------------------------------------------------- S3 snapshot
def s03(prs):
    s = blank(prs)
    head(s, 3, "Internal scan", "From a basement workshop to a ₹175 crore valuation.")
    kpis = [("₹2.9 Cr", "FY25 revenue · FY24 was ₹20 lakh"), ("5,000", "pairs a month by end-2025"),
            ("56 / 11", "gross margin % / contribution %"), ("100+50", "karigars + corporate team, Noida")]
    x = M
    hair(s, M, 2.1, CW2)
    for v, l in kpis:
        tb(s, x, 2.22, 2.0, 0.45, v, size=20, color=INK, font=DISP)
        tb(s, x, 2.66, 2.0, 0.5, l, size=8.2, color=MUT, spacing=1.05)
        x += 2.05
    hair(s, M, 3.28, CW2)
    miles = [("AUG 23", "launch; 50 prepaid orders, basement shoemaker, 10–20 pairs a month"),
             ("DEC 23", "viral reel — 250 pairs sold out in two days"),
             ("APR 24", "first unit: 1,000 sq ft, six karigars, Noida Sec-10"),
             ("DEC 24", "₹8.7 Cr seed (Zeropearl); own factory; 600 pairs a month"),
             ("SEP 25", "flagship “Baithak” store, Panchsheel Park, Delhi"),
             ("JAN 26", "₹26.5 Cr Series A (Saama) · Shark Tank ₹1 Cr @ ₹175 Cr"),
             ("SEP 26", "Bengaluru store + Kaapi drop; three cities live")]
    y = 3.44
    for d, t in miles:
        tb(s, M, y, 0.95, 0.25, d, size=9.3, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 1.0, y, 7.2, 0.3, t, size=9.3, color=SOFT)
        y += 0.34
    tb(s, M, 5.95, 8.25, 1.0, [
        [("Social: ", {"bold": True, "color": INK}), ("X @GullyLabs, 3,640 followers (verified 19 Sep 2026) — a drop wire, not a reach channel; reach lives on reels (one 7M-view reel made the Dec-23 sellout) and cause-marketing (15% of three days’ sales to Hemkunt Foundation).", {})],
        "They own the factory, the store and the data: D2C India + global, Amazon store, curated partners; 10,000+ orders shipped."],
       size=8.8, color=MUT, spacing=1.18, after=4)
    photoR(s, "gully-labs-sneakers-delhi-campaign-india-4.jpg", "The flagship, Panchsheel Park — karigar studio, shoe museum, Baithak (AD India).")
    notes(s, "Aisha, 60 sec. Three claims, not a node-by-node timeline: (1) 20 lakh to 2.9 crore to 16 crore "
             "expected - 80x in two years off a tiny base; (2) they own factory AND store, against everyone's "
             "advice; (3) their X following is 3,640 - reach is reels and word of mouth, which matters later. "
             "Flag the bestseller price Rs 8,900 - Air Force 1 territory.")


# ---------------------------------------------------------------- S4 mission
def s04(prs):
    s = blank(prs)
    head(s, 4, "Business policy", "A mission, four policies, one culture.")
    tb(s, M, 2.0, 0.6, 0.7, "“", size=42, color=BROWN, font=DISP)
    tb(s, M + 0.5, 2.15, 7.7, 1.2,
       "To build a household sneaker brand from Delhi that is truly and unapologetically Indian — shaped by "
       "local streets, crafts and culture, not borrowed from the West.",
       size=14.5, color=INK, font=DISP, italic=True, spacing=1.28)
    tb(s, M, 3.45, 8.25, 0.9,
       "Vision: put India’s authentic culture on the global sneaker map — “the next great global sneaker brand "
       "out of India, one culturally-charged collection at a time.” Values in behaviour: originality (never a "
       "compromise brand), craft (karigar-first, ~95% local), community (six months of content before product), "
       "story (every colourway named), value-not-price (“India is not price-conscious, it’s value-conscious”).",
       size=10, color=SOFT, spacing=1.22)
    pol = [("ORIGIN POLICY", "functional", "own factory; ~95% local sourcing"),
           ("DROP POLICY", "operating", "a themed collection every ~60 days, limited runs"),
           ("PRICE POLICY", "strategic", "premium held at ₹4–15k; no discount-led growth"),
           ("PEOPLE POLICY", "functional", "karigar retention; backend permissioning post-fraud")]
    y = 4.98
    hair(s, M, 4.85, CW2)
    for a, b, c in pol:
        tb(s, M, y, 1.6, 0.25, a, size=8.8, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 1.7, y, 1.0, 0.25, b, size=8.8, color=MUT, italic=True)
        tb(s, M + 2.75, y, 5.5, 0.25, c, size=8.8, color=SOFT)
        y += 0.31
    tb(s, M, 6.3, 8.25, 0.6,
       "Corporate culture is the control system: the manifesto doubles as hiring filter and design brief; 100 "
       "karigars are the brand’s named heroes; a trust-based culture met its first governance wall in Feb 2026 (S13).",
       size=9.2, color=MUT, spacing=1.22)
    photoR(s, "gully-labs-sneakers-delhi-campaign-india-2.jpg", "Co-founder Arjun Singh — r/Delhi AMA, 2025.")
    notes(s, "Aisha, 60 sec. Framing line: 'A mission is a statement; a policy is the rule that stops you "
             "breaking it.' Give one example per policy type, do not read all four. Land the culture point: "
             "the manifesto does the work an SOP manual does at a big firm - and slide 13 shows the day that "
             "stopped being enough.")


# ---------------------------------------------------------------- S5 PESTEL
def s05(prs):
    s = blank(prs)
    head(s, 5, "Environmental scanning · external", "PESTEL: a friendly climate, one tax headwind.")
    rows = [("P", "Make in India; 35% BCD + surcharge + IGST on imported footwear (landed ≈45–60% of CIF); BIS/QCO norms; IFLDP (₹1,700 Cr) is the live scheme.",
             "Tailwind: the Noida factory is a tariff and quality advantage global rivals cannot match at this price."),
            ("E", "GST rationalisation (Sep ’25): ≤₹2,500 at 5%, above still 18%; category growing 12–15% p.a.; ~70M pairs by 2029.",
             "Headwind in a tailwind: 100% of the range sits in the 18% slab — ~₹600–1,200/pair vs mass rivals."),
            ("S", "Gen-Z identity buying; hype and resale culture; gully rap and streetwear; diaspora demand; festive spikes.",
             "Core demand: ~25% export share is diaspora + design-led, not price-led."),
            ("T", "D2C stack, drop mechanics, social commerce, creator economy, AI demand forecasting.",
             "Enabler: the 60-day calendar is a software problem as much as a craft one."),
            ("E", "Leather supply chain, tannery effluent norms, air-freight carbon on exports.",
             "Watch item: an ESG narrative they have not yet had to defend."),
            ("L", "Motif/trademark IP hard to protect; BIS and labelling compliance; internal-controls exposure.",
             "Live risk: IP defensibility; the Feb ’26 fraud was a controls failure, not a market failure.")]
    y = 2.0
    for L, force, impl in rows:
        tb(s, M, y, 0.45, 0.45, L, size=16, color=BROWN, font=DISP)
        tb(s, M + 0.55, y, 3.6, 0.72, force, size=8.5, color=SOFT, spacing=1.12)
        tb(s, M + 4.3, y, 3.95, 0.72, impl, size=8.5, color=SOFT, spacing=1.12)
        hair(s, M, y + 0.75, CW2)
        y += 0.79
    tb(s, M, 6.78, 8.25, 0.25,
       "Net read: four tailwinds, one tax penalty, one governance gap — the 18% slab attacks the price point.",
       size=9.2, color=BROWN, italic=True, font=DISP)
    photoR(s, "ph-street-market.jpg", "Old Delhi commerce — the demand side of the PESTEL.")
    notes(s, "Aisha, 60 sec. Do not read the rows. Deliver only the implication column for P, E and L. The "
             "killer fact is GST: Sep 2025 cut footwear under Rs 2,500 to 5% but left everything above at 18%, "
             "and 100% of Gully Labs' range is above. End on the italic net read.")


# ---------------------------------------------------------------- S6 value chain
def s06(prs):
    s = blank(prs)
    head(s, 6, "Environmental scanning · internal", "The value chain — where the ₹100 goes.")
    steps = [("01", "DESIGN & STORY", "cultural archive; 60-day drop brief"),
             ("02", "SOURCING", "~95% local leather, rubber, thread"),
             ("03", "OPERATIONS", "hand-lasted in Noida, ~4 days a pair"),
             ("04", "FULFILMENT", "D2C India + global; free exchange"),
             ("05", "MARKETING & RETAIL", "reels, collabs, Baithak store")]
    x = M
    hair(s, M, 2.1, CW2)
    w = (CW2 - 4 * 0.15) / 5
    for num, name, desc in steps:
        tb(s, x, 2.2, 0.5, 0.3, num, size=12, color=BROWN, font=DISP)
        tb(s, x, 2.5, w, 0.4, name, size=8.2, color=INK, bold=True, spc=1.0)
        tb(s, x, 2.88, w + 0.2, 0.6, desc, size=8.0, color=MUT, spacing=1.1)
        x += w + 0.15
    hair(s, M, 3.55, CW2)
    tb(s, M, 3.65, 8.25, 0.3, "THE MARGIN LADDER — ₹100 OF REVENUE (FOUNDERS’ OWN NUMBERS)", size=8.8, color=BROWN, spc=1.5)
    bars = [("100", 1.0, "revenue"), ("56", 0.62, "after COGS"), ("46", 0.52, "after logistics"), ("11", 0.2, "after marketing")]
    x = M
    for v, frac, lab in bars:
        hgt = 0.32 + frac * 1.05
        rect(s, x, 5.62 - hgt, 1.45, hgt, line=BROWN, lw=1.0)
        tb(s, x, 5.62 - hgt - 0.3, 1.45, 0.3, v, size=14, color=INK, font=DISP, align=PP_ALIGN.CENTER)
        tb(s, x, 5.7, 1.45, 0.3, lab, size=8.0, color=MUT, align=PP_ALIGN.CENTER)
        x += 1.62
    tb(s, M, 6.15, 8.25, 0.8, [
        "FY25 loss ₹1 Cr on ₹2.9 Cr; Delhi store at breakeven; cash ₹85 lakh. 56% gross proves the premium is accepted; 35% marketing proves the brand does not yet pull.",
        "Value is created at the two ends — story and community — not in the middle; the bottleneck is hands, not machines (1,000 wanted vs 600 made).",
        "Backward integration was the bet: ₹1.2 Cr from college friends to own the line."],
       size=8.6, color=SOFT, spacing=1.16, after=3)
    photoR(s, "gully-labs-sneakers-delhi-campaign-india-5.jpg", "The heel says गली — Devanagari branding, hand-lasted.")
    notes(s, "Rohan, 55 sec. Walk the five steps once, then the ladder: '100 rupees in, 11 left - the whole "
             "business case on one line.' Two claims: 56% gross proves pricing works; 35% marketing proves the "
             "brand doesn't pull. Remember-line: they ran out of karigars before they ran out of demand.")


# ---------------------------------------------------------------- S7 industry & porter
def s07(prs):
    s = blank(prs)
    head(s, 7, "Industrial analysis · Porter", "A growth-stage industry; two high forces.")
    comps = [("Nike / Adidas / Puma / NB", "₹4k–20k+", "HIGH — own the aspiration being attacked"),
             ("Comet", "$6.6M", "HIGH — same buyer, ~10× their FY25 revenue"),
             ("CHK (Accel)", "$3.8M", "MEDIUM-HIGH — closest design rival"),
             ("RapidBox", "$9.5M", "MEDIUM — a price tier below"),
             ("Neemans / Solethreads / Zeesh", "seed–A", "LOW-MEDIUM — different promise")]
    tb(s, M, 1.98, 5.0, 0.25, "THE COMPETITIVE SET", size=8.8, color=BROWN, spc=1.5)
    y = 2.24
    for name, fund, threat in comps:
        tb(s, M, y, 2.35, 0.3, name, size=8.8, color=INK, bold=True)
        tb(s, M + 2.4, y, 0.9, 0.3, fund, size=8.6, color=MUT)
        tb(s, M + 3.35, y, 4.9, 0.3, threat, size=8.6, color=SOFT)
        hair(s, M, y + 0.36, CW2)
        y += 0.44
    tb(s, M, 4.55, 8.25, 0.25, "INDUSTRY CYCLE — EMBRYONIC · GROWTH · SHAKEOUT · MATURITY · DECLINE",
       size=8.4, color=MUT, spc=1.1)
    rect(s, M + 2.35, 4.52, 1.05, 0.03, fill=BROWN)
    tb(s, M, 4.85, 8.25, 0.6,
       "They are in the growth stage: position-buying, losses tolerated (₹1 Cr on ₹2.9 Cr is that logic); the "
       "shakeout will punish players with neither scale nor an uncopyable story.",
       size=8.8, color=SOFT, spacing=1.15)
    forces = [("BUYER POWER", "HIGH", "zero switching cost, infinite choice, hype moves fast"),
              ("RIVALRY", "HIGH", "global giants + funded D2C + marketplaces"),
              ("NEW ENTRANTS", "MODERATE", "cheap to start, years to craft a brand"),
              ("SUBSTITUTES", "MODERATE", "resale Jordans, copies, apparel for the same rupee"),
              ("SUPPLIER POWER", "MODERATE", "leather is easy; skilled karigars are the scarce input")]
    y = 5.62
    for name, rating, why in forces:
        tb(s, M, y, 8.25, 0.25, [
            [(name + "  ", {"bold": True, "color": INK, "size": 8.4, "spc": 1}),
             (rating + "  ", {"color": BROWN, "font": DISP, "italic": True, "size": 9}),
             ("· " + why, {"size": 8.2, "color": MUT})]])
        y += 0.245
    photoR(s, "ph-sneaker-wall.jpg", "Verdict: the moat cannot be design — it is factory + karigars + community + store.")
    notes(s, "Rohan, 80 sec. Move 1 - the orange-underline fact: they are the most expensive Indian sneaker "
             "company that is not a volume player. Move 2 - ask the room which force is strongest, wait, reveal "
             "buyer power: switching cost is literally zero. Close on the italic verdict.")


# ---------------------------------------------------------------- S8 4P + SWOT
def s08(prs):
    s = blank(prs)
    head(s, 8, "Situational analysis", "Four consistent Ps; an honest SWOT.")
    ps = [("PRODUCT", ["4 silhouettes, ~50 SKUs, Indian lexicon — Baaz, Kulfi, Kaapi",
                       "hand-lasted; Kantha, Rangoli, Devanagari signatures",
                       "scarcity: 60-day drops, collabs (RAGA, Nivia, CMF)"]),
          ("PRICE", ["₹3,790–15,000; bestseller Calico White ₹8,900",
                     "value-based: “not price-conscious, value-conscious”",
                     "no discount-led growth; global $140–220"]),
          ("PLACE", ["D2C-first India + global; Amazon store",
                     "own flagship + Bengaluru; Mumbai next; curated partners",
                     "~25% exports — US, UK, UAE, SG, AU, CA"]),
          ("PROMOTION", ["community before product; 6 months of content",
                         "creator spikes (7M-view reel); Shark Tank as brand event",
                         "cause-marketing; X as the drop wire"])]
    for i, (name, items) in enumerate(ps):
        x = M + (i % 2) * 4.2; y = 2.0 + (i // 2) * 1.12
        tb(s, x, y, 3.9, 0.25, name, size=9.5, color=BROWN, bold=True, spc=2)
        hair(s, x, y + 0.27, 3.9)
        tb(s, x, y + 0.34, 4.0, 0.8, items, size=8.0, color=SOFT, spacing=1.1, after=2)
    hair(s, M, 4.3, CW2)
    quads = [("S", 0, 0, ["own factory → quality, flexibility, duty shield",
                          "56% gross margin; ~8× revenue growth",
                          "unmatched cultural IP and content engine",
                          "a named craft workforce competitors lack"]),
             ("W", 1, 0, ["CM2 ~11%; marketing 35% of revenue",
                          "cash ₹85 lakh vs a retail build-out",
                          "capacity capped by trained hands",
                          "100% of SKUs in the 18% GST slab"]),
             ("O", 0, 1, ["category +12–15%; premium band expanding",
                          "diaspora + a US store in 12–18 months",
                          "apparel and accessories lift AOV",
                          "tier-2 aspiration; experiential retail"]),
             ("T", 1, 1, ["global brands out-spend and out-collab overnight",
                          "funded domestic rivals (Comet, CHK, RapidBox)",
                          "motifs are copyable; authenticity backlash",
                          "valuation far ahead of revenue"])]
    for L, ci, ri, items in quads:
        x = M + ci * 4.2; y = 4.42 + ri * 1.24
        tb(s, x, y, 0.4, 0.35, L, size=15, color=BROWN, font=DISP)
        tb(s, x + 0.45, y, 3.7, 1.15, ["—  " + i for i in items], size=7.9, color=SOFT, spacing=1.08)
    hair(s, M + 4.05, 4.42, 0.011); rect(s, M + 4.07, 4.42, 0.011, 2.48, fill=LINE)
    hair(s, M, 5.62, CW2)
    photoR(s, "ph-leather-brogues.jpg", "Hand-finished leather — the product half of the 4Ps.")
    notes(s, "Rohan, 80 sec. 4Ps in one breath: premium product, premium price, owned place, community "
             "promotion - four Ps pointing the same way is what consistency looks like. SWOT: one item per "
             "quadrant out loud - factory; 11%; diaspora; Comet's balance sheet. Hand to Meera.")


# ---------------------------------------------------------------- S9 levels
def s09(prs):
    s = blank(prs)
    head(s, 9, "Strategy formulation", "Three growth fronts at once — and one refusal.")
    dirs = [("MARKET PENETRATION", "NOW", "deepen Delhi NCR; repeat buyers; accessories lift AOV"),
            ("MARKET DEVELOPMENT", "NOW", "Bengaluru, Mumbai, tier-2; exports 25%; US store in 12–18 months"),
            ("PRODUCT DEVELOPMENT", "NOW", "002/003/004, apparel, jerseys, slides, collabs"),
            ("DIVERSIFICATION", "LATER", "experience retail and community programming; nothing unrelated"),
            ("RETRENCHMENT", "NEVER", "no discounting, no marketplace volume, no contract manufacturing")]
    y = 2.02
    for name, tag, txt in dirs:
        tb(s, M, y, 2.35, 0.3, name, size=8.6, color=INK, bold=True, spc=1.0)
        col = BROWN if tag == "NOW" else (MUT if tag == "LATER" else RGBColor(0xA0, 0x52, 0x3C))
        tb(s, M + 2.4, y, 0.75, 0.3, tag, size=8.6, color=col, font=DISP, italic=True)
        tb(s, M + 3.2, y, 5.05, 0.45, txt, size=8.6, color=SOFT, spacing=1.1)
        hair(s, M, y + 0.46, CW2)
        y += 0.55
    tb(s, M, 4.9, 8.25, 0.7,
       "“They would rather stay small than cheap.” Read that row out loud: retrenchment refused as policy is "
       "the clearest statement of what this brand is.", size=10, color=BROWN, font=DISP, italic=True, spacing=1.22)
    lv = [("CORPORATE — where", "culture-led premium footwear + apparel; vertically integrated; equity-financed"),
          ("BUSINESS — how", "focused differentiation in the ₹4–15k niche; never cost leadership"),
          ("FUNCTIONAL — who", "60-day drops · karigar capacity · community marketing · membership retail")]
    y = 5.75
    for name, txt in lv:
        tb(s, M, y, 8.25, 0.25, [
            [(name + "  ", {"bold": True, "color": INK, "size": 8.4, "spc": 1}),
             (txt, {"size": 8.2, "color": MUT})]])
        y += 0.28
    tb(s, M, 6.62, 8.25, 0.3,
       "The tension: three growth vectors on ₹85 lakh of cash and a workforce that must be trained, not hired.",
       size=8.6, color=MUT, spacing=1.15)
    photoR(s, "ph-street-energy.jpg", "Growth fronts: the street is the market, the market is the street.")
    notes(s, "Meera, 55 sec. One idea: three growth strategies at once and one refused - read the red row "
             "aloud. Levels in one breath: where / how / who. Finish on the tension box; it sets up evaluation.")


# ---------------------------------------------------------------- S10 BCG + GE
def s10(prs):
    s = blank(prs)
    head(s, 10, "Portfolio tools", "One star, two question marks, and a dog to refuse.")
    bx, by, bs = M, 2.05, 2.6
    rect(s, bx, by, bs, bs, line=LINE, lw=1.0)
    rect(s, bx + bs / 2, by, 0.011, bs, fill=LINE); rect(s, bx, by + bs / 2, bs, 0.011, fill=LINE)
    tb(s, bx + 0.06, by + 0.05, 1.2, 0.2, "QUESTION MARKS", size=6.8, color=MUT, spc=0.8)
    tb(s, bx + bs / 2 + 0.06, by + 0.05, 1.0, 0.2, "STARS", size=6.8, color=BROWN, spc=0.8)
    tb(s, bx + 0.06, by + bs / 2 + 0.05, 0.8, 0.2, "DOGS", size=6.8, color=MUT, spc=0.8)
    tb(s, bx + bs / 2 + 0.06, by + bs / 2 + 0.05, 1.2, 0.2, "CASH COWS", size=6.8, color=MUT, spc=0.8)
    def circ(x, y, d, lab):
        rect(s, x, y, d, d, line=BROWN, lw=1.2, shape=MSO_SHAPE.OVAL)
        tb(s, x - 0.35, y + d + 0.01, d + 0.7, 0.35, lab, size=6.8, color=SOFT, align=PP_ALIGN.CENTER, spacing=0.95)
    circ(bx + bs / 2 + 0.7, by + 0.4, 0.45, "GL 001")
    circ(bx + 0.4, by + 0.45, 0.34, "collabs ·\nUS/diaspora")
    circ(bx + 0.45, by + bs / 2 + 0.35, 0.3, "apparel")
    circ(bx + 0.2, by + bs / 2 + 0.9, 0.24, "discount\nvolume")
    tb(s, bx, by + bs + 0.06, bs, 0.2, "BCG — GROWTH ↑ · SHARE →", size=7.2, color=MUT, spc=1)
    gx, gy, gs = M + 3.0, 2.05, 2.6
    rect(s, gx, gy, gs, gs, line=LINE, lw=1.0)
    for k in (1, 2):
        rect(s, gx + k * gs / 3, gy, 0.011, gs, fill=LINE); rect(s, gx, gy + k * gs / 3, gs, 0.011, fill=LINE)
    tb(s, gx + 0.05, gy + 0.04, 1.2, 0.2, "INVEST / GROW", size=6.8, color=BROWN, spc=0.8)
    tb(s, gx + gs - 1.4, gy + gs - 0.2, 1.35, 0.2, "HARVEST / EXIT", size=6.8, color=MUT, spc=0.8, align=PP_ALIGN.RIGHT)
    def gc(ci, ri, d, lab):
        cx = gx + ci * gs / 3 + gs / 6 - d / 2; cy = gy + ri * gs / 3 + gs / 6 - d / 2
        rect(s, cx, cy, d, d, line=BROWN, lw=1.2, shape=MSO_SHAPE.OVAL)
        tb(s, cx - 0.35, cy + d + 0.01, d + 0.7, 0.35, lab, size=6.8, color=SOFT, align=PP_ALIGN.CENTER, spacing=0.95)
    gc(2, 0, 0.42, "India D2C")
    gc(2, 1, 0.34, "exports")
    gc(1, 1, 0.32, "apparel")
    gc(0, 2, 0.28, "mass basics")
    tb(s, gx, gy + gs + 0.06, gs, 0.2, "GE — ATTRACTIVENESS ↑ · STRENGTH →", size=7.2, color=MUT, spc=1)
    tb(s, M + 6.0, 2.05, 3.15, 0.25, "THE VERDICT, TWICE", size=8.8, color=BROWN, spc=1.5)
    tb(s, M + 6.0, 2.35, 3.15, 1.7, [
        "Grow — core India D2C: fund capacity and stores.",
        "Build selectively — exports: prove unit economics first.",
        "Selectivity — apparel: fund only what lifts AOV.",
        "Refuse — discount-led volume: absent today; must stay absent."],
       size=8.2, color=SOFT, spacing=1.22, after=4)
    tb(s, M, 5.1, 8.25, 1.6,
       "“BCG alone would call a 30-month-old brand a dog — it only scores share and growth. GE is the honest "
       "tool for Gully Labs because it scores brand, craft and factory: what they actually own. Used together "
       "both say the same thing: concentrate on the star, cap the question marks, refuse the dog.”",
       size=9.4, color=BROWN, font=DISP, italic=True, spacing=1.25)
    photoR(s, "ph-portfolio-wall.jpg", "A wall of single shoes — the industry's own portfolio matrix.")
    notes(s, "Meera, 65 sec. Quiz first: 'Where do socks and jerseys sit?' - question mark now, dog if "
             "unfocused; accessories only work if they lift AOV. Then the marks-line about BCG vs GE. Don't "
             "explain the matrices; the room knows them.")


# ---------------------------------------------------------------- S11 implementation
def s11(prs):
    s = blank(prs)
    head(s, 11, "Strategy implementation", "Where strategies like this die.")
    ch = [("SCALING CRAFT", "capacity is trained hands, not machines — 1,000 wanted vs 600 made"),
          ("FUNDING THE BUILD", "₹85 lakh of cash against three stores and a factory expansion"),
          ("HOLDING THE LINE", "marketplaces and festivals push discounting; the brand cannot"),
          ("GOVERNANCE", "founder-trust culture meeting payroll-scale risk (S13)"),
          ("CROSS-BORDER OPS", "6–8 day global delivery, returns, duties, 25% of revenue")]
    y = 2.0
    for name, txt in ch:
        tb(s, M, y, 6.0, 0.24, name, size=8.6, color=INK, bold=True, spc=1.2)
        tb(s, M, y + 0.24, 6.0, 0.24, txt, size=8.2, color=SOFT)
        hair(s, M, y + 0.52, 6.2)
        y += 0.56
    tb(s, M, 4.9, 6.2, 0.25, "THE FRESH ₹27.5 Cr — CAPACITY FIRST, BRAND SECOND", size=8.4, color=BROWN, spc=1.3)
    alloc = [("production & karigar skilling", 0.34), ("brand & content", 0.26), ("retail build-out", 0.24),
             ("senior talent & controls", 0.10), ("working capital", 0.06)]
    y = 5.22
    for lab, pct in alloc:
        tb(s, M, y + 0.02, 2.3, 0.22, lab, size=7.8, color=SOFT)
        rect(s, M + 2.35, y, 2.6 * pct / 0.34, 0.2, line=BROWN, lw=1.0)
        tb(s, M + 5.1, y, 0.8, 0.22, "%d%%" % int(pct * 100), size=9, color=INK, font=DISP)
        y += 0.26
    tb(s, M, 6.6, 6.2, 0.2, "illustrative split, inferred from stated uses of proceeds — not a disclosure",
       size=7.2, color=MUT, italic=True)
    tb(s, M + 6.5, 1.98, 2.65, 0.25, "FOUR ACTIONS · BLUE OCEAN", size=8.4, color=BROWN, spc=1.3)
    acts = [("ELIMINATE", "celebrity-athlete deals; discount-led growth"),
            ("REDUCE", "SKU sprawl; marketplace dependence"),
            ("RAISE", "craft depth, storytelling, store experience, export service"),
            ("CREATE", "the drop ritual, the karigar celebrity, the Baithak, India-origin sneaker IP")]
    y = 2.28
    for name, txt in acts:
        tb(s, M + 6.5, y, 2.65, 0.22, name, size=8.0, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 6.5, y + 0.22, 2.65, 0.5, txt, size=7.8, color=SOFT, spacing=1.1)
        y += 0.72
    tb(s, M + 6.5, 5.25, 2.65, 0.22, "DESIGN THINKING, LIVED — 2022–23", size=7.6, color=MUT, spc=1)
    tb(s, M + 6.5, 5.5, 2.65, 1.2,
       "Six months of community posts → “India needs its own sneaker” → Baithak sessions → 50 pairs at ₹1,000 "
       "(worth ₹6,000) → the 250-pair sellout. The method is in the name: Labs.",
       size=7.8, color=SOFT, spacing=1.15)
    photoR(s, "ph-karigar.jpg", "Scaling craft means scaling hands — the karigar is the bottleneck and the moat.")
    notes(s, "Meera, 65 sec. Three beats: name two challenges (craft scaling, Rs 85 lakh); design thinking - "
             "first product was a Rs 1,000 prototype of a Rs 6,000 shoe; blue ocean - read the four actions. "
             "If long, skip the bars and say 'capacity first, brand second'.")


# ---------------------------------------------------------------- S12 BSC
def s12(prs):
    s = blank(prs)
    head(s, 12, "Evaluation · measuring performance", "Profit alone would have killed it in 2023.")
    quads = [("FINANCIAL", [("revenue", "₹2.9 Cr", "₹16–30 Cr"), ("CM2 margin", "~11%", "18%"),
                            ("marketing / rev", "~35%", "22%"), ("cash", "₹85 lakh", "12-mo runway")]),
             ("CUSTOMER", [("repeat rate", "n.d.", "25–35%"), ("7-day sell-through", "variable", ">80%"),
                           ("export share", "~25%", "35%"), ("community", "3.6k", "100k")]),
             ("INTERNAL PROCESS", [("pairs / month", "~5,000", "12,000"), ("days per pair", "~4", "2.5"),
                                   ("returns", "n.d.", "<5%"), ("on-time drops", "60-day", "100%")]),
             ("LEARNING & GROWTH", [("karigars", "~100", "220"), ("attrition", "n.d.", "<7%"),
                                    ("silhouettes / yr", "2–3", "4"), ("senior hires", "started", "6")])]
    x = M
    w = (CW2 - 3 * 0.15) / 4
    for name, rows in quads:
        tb(s, x, 2.02, w, 0.25, name, size=8.6, color=BROWN, bold=True, spc=1.5)
        hair(s, x, 2.28, w)
        yy = 2.4
        for m, a, b in rows:
            tb(s, x, yy, w * 0.5, 0.3, m, size=7.6, color=INK, bold=True)
            tb(s, x + w * 0.5, yy, w * 0.25, 0.3, a, size=7.2, color=MUT)
            tb(s, x + w * 0.77, yy, w * 0.23, 0.3, b, size=7.6, color=BROWN, font=DISP)
            yy += 0.36
        x += w + 0.15
    hair(s, M, 4.3, CW2)
    tb(s, M, 4.42, 8.25, 0.9,
       "“A financial-only scorecard would have told them to shut the factory in 2023. The balanced one is what "
       "justifies investing in brand, craft and community before profit.” Muted numbers are disclosed baselines; "
       "the Georgia targets are our proposed FY27 scoreboard — say that distinction out loud.",
       size=9.6, color=SOFT, font=DISP, italic=True, spacing=1.22)
    tb(s, M, 5.5, 8.25, 0.3, "READ ONE ROW PER QUADRANT ALOUD: revenue · repeat purchase · pairs per month · karigar attrition.",
       size=8.4, color=MUT, spc=1.1)
    photoR(s, "ph-store-community.jpg", "The customer perspective, live: the store wall and its community.")
    notes(s, "Kabir, 55 sec. Open with the why-BSC line. Read one row per quadrant. Be explicit: disclosed "
             "baselines vs our proposed targets - examiners reward that distinction.")


# ---------------------------------------------------------------- S13 control
def s13(prs):
    s = blank(prs)
    head(s, 13, "Evaluation · control & contingency", "The week that cost ₹2 lakh.")
    ctl = [("QUESTION", "are we on the right course?", "is today’s task done right?"),
           ("HORIZON", "1–3 yrs, per drop cycle", "daily–monthly"),
           ("OWNER", "founders + board", "function heads"),
           ("EXAMPLE", "Mumbai or margin first?", "stock counts, discount permissions"),
           ("FAILURE", "drift into discount volume", "the Feb ’26 fraud")]
    tb(s, M, 1.98, 6.0, 0.25, "STRATEGIC  vs  OPERATIONAL  CONTROL", size=8.8, color=BROWN, spc=1.5)
    y = 2.24
    for a, b, c in ctl:
        tb(s, M, y, 1.15, 0.3, a, size=8.2, color=INK, bold=True, spc=1)
        tb(s, M + 1.2, y, 3.4, 0.4, b, size=8.2, color=SOFT)
        tb(s, M + 4.7, y, 3.55, 0.4, c, size=8.2, color=SOFT)
        hair(s, M, y + 0.42, CW2)
        y += 0.5
    tb(s, M, 4.85, 8.25, 0.25, "STRATEGY AUDIT — SIX QUESTIONS", size=8.8, color=BROWN, spc=1.5)
    tb(s, M, 5.12, 8.25, 0.7,
       "01 mission still relevant? · 02 is the appraisal honest (56% GM, 11% CM2)? · 03 objectives vs resources "
       "(₹85 lakh vs 3 stores)? · 04 4Ps internally consistent? · 05 risk acceptable to the board? · 06 are the "
       "controls adequate — and independent?",
       size=8.2, color=SOFT, spacing=1.2)
    tb(s, M, 5.95, 8.25, 0.5, [
        [("The fraud: ", {"bold": True, "color": INK}), ("a new CS hire used admin access to create 100%-discount codes, shipped ₹2 lakh of sneakers to friends, quit in a week. Fixes: permissioning done; four-eyes on discounts; weekly anomaly reports; written incident protocol.", {})]],
       size=8.2, color=SOFT, spacing=1.15)
    tb(s, M, 6.55, 8.25, 0.4,
       "Triggers pre-planned: 2 drops <70% sell-through · karigar utilisation >90% a quarter · runway <6 mo → "
       "pause store #3, not marketing · sentiment spike → founder reply in 24 h.",
       size=8.0, color=MUT, spacing=1.12)
    photoR(s, "ph-workbench.jpg", "Operational control lives at the bench — where the orders are 'paid'.")
    notes(s, "Kabir, 65 sec. Open with the fraud story - true and memorable. Syllabus point: that was an "
             "operational-control failure; the strategic question is whether trust-culture survives 150 "
             "employees - you need both at different frequencies. Skim the audit; land the triggers line.")


# ---------------------------------------------------------------- S14 verdict
def s14(prs):
    s = blank(prs)
    head(s, 14, "Conclusion", "A real strategy; three things between it and scale.")
    cols = [("WHAT IS WORKING", ["positioning no one else occupies",
                                 "56% gross margin — the price is accepted",
                                 "vertical integration = quality + duty shield",
                                 "real growth: 20 lakh → 2.9 Cr → 16 Cr"]),
            ("WHAT IS NOT", ["11% CM2 — marketing buys every order",
                             "capacity capped by trained hands",
                             "₹85 lakh vs a three-city build-out",
                             "governance proven fragile once"]),
            ("OUR VERDICT", ["culture is a real advantage — but only",
                             "welded to factory, karigars and store;",
                             "remove one and it is a marketing line",
                             "a better-funded rival copies in a season"])]
    x = M
    w = (CW2 - 2 * 0.2) / 3
    for name, items in cols:
        tb(s, x, 2.02, w, 0.25, name, size=8.8, color=BROWN, bold=True, spc=1.5)
        hair(s, x, 2.28, w)
        tb(s, x, 2.4, w, 1.6, ["—  " + i for i in items], size=8.2, color=SOFT, spacing=1.25)
        x += w + 0.2
    hair(s, M, 4.25, CW2)
    recs = [("01", "TREAT CRAFT LIKE TECHNOLOGY", "a karigar academy: certification, retention-linked pay, a documented pipeline — capacity is the ceiling, and the only lever that doesn’t dilute the hand-made claim"),
            ("02", "MAKE EXPORTS THE MARGIN ENGINE", "the diaspora pays $140–220 with no 18% slab and lower CAC; target 35% exports; a US pop-up before a permanent store"),
            ("03", "PROTECT THE PREMIUM WITH CONTROLS", "formal discount authority, an independent audit-minded director, a sell-through-based drop calendar — scarcity only works if nobody believes a sale is coming")]
    y = 4.4
    for num, t, d in recs:
        tb(s, M, y, 0.6, 0.4, num, size=18, color=BROWN, font=DISP)
        tb(s, M + 0.7, y + 0.02, 4.5, 0.25, t, size=9.2, color=INK, bold=True, spc=1.3)
        tb(s, M + 0.7, y + 0.3, 7.55, 0.45, d, size=8.4, color=SOFT, spacing=1.12)
        hair(s, M, y + 0.78, CW2)
        y += 0.83
    photoR(s, "ph-hero-pair.jpg", "The verdict, in leather: premium held, craft kept, controls added.")
    notes(s, "Kabir, 60 sec. Vote first, count, say 'yes, conditionally'. One sentence per recommendation. "
             "Closing argument from the verdict column: culture is a moat only because of factory + karigars + "
             "store. That is the line the examiner remembers.")


# ---------------------------------------------------------------- S15 close
def s15(prs):
    s = blank(prs)
    tb(s, M, 1.0, 8.0, 1.0, "Questions?", size=42, color=INK, font=DISP)
    hair(s, M, 2.2, SW - 2 * M)
    tb(s, M, 2.4, 8.25, 0.25, "IF ASKED ONLY ONE THING", size=8.8, color=BROWN, spc=1.5)
    tb(s, M, 2.68, 8.25, 1.4,
       "“Is the strategy sustainable?” — The strategy is coherent and the environment favourable; the risk is "
       "not strategic but financial and organisational. A brand with 56% gross margin, 11% contribution and "
       "₹85 lakh of cash is one bad quarter from choosing between its stores and its positioning. Evaluation "
       "exists to catch that moment early.",
       size=10, color=SOFT, font=DISP, italic=True, spacing=1.25)
    tb(s, M, 4.35, 8.25, 0.25, "BACKUP, READY TO JUMP TO", size=8.8, color=BROWN, spc=1.5)
    tb(s, M, 4.62, 8.25, 0.8,
       "unit economics (100→56→46→11) · funding & cap table (₹8.7 Cr seed · ₹26.5 Cr A @ ₹147 Cr · Shark Tank ₹175 Cr) "
       "· GST math (~₹600–1,200/pair) · competitor deep-dive (Comet ₹29 Cr FY25) · why own the factory (35% duty shield)",
       size=8.6, color=SOFT, spacing=1.2)
    tb(s, M, 5.5, 8.25, 0.25, "SOURCES CHECKED FOR THIS STUDY", size=8.8, color=BROWN, spc=1.5)
    tb(s, M, 5.76, 8.25, 1.1, [
        "gullylabs.com · global.gullylabs.com (story, pricing, ranges) · @GullyLabs on X — 3,640 followers, to 19 Sep 2026",
        "Financial Express, 28 Jan 2026 — FY24/25 financials, unit economics · Inc42, 23 Jan 2026 — founding, capacity, FY26/27 targets",
        "Indian Retailer — seed, Series A, flagship store · Mint, 22 Feb 2026 — the ₹2 lakh fraud",
        "PIB, Sep 2025 — GST rationalisation · Tracxn — competitor set · AD India — flagship interior (photo)"],
       size=8.0, color=SOFT, spacing=1.2, after=2)
    tb(s, M, 6.78, 8.25, 0.25, "Thank you — Aisha · Rohan · Meera · Kabir · Business Policy & Strategic Management, September 2026",
       size=8.6, color=MUT)
    photoR(s, "ph-sneaker-store.jpg", "Every wall in this category is a portfolio — yours must be a story.")
    folio(s, 15)
    notes(s, "Kabir, 15 sec + Q&A. Thank the room, invite questions; if silent, ask the verdict question "
             "yourself. Keep the five backups ready - funding and GST are the likeliest probes.")


OUT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Gully_Labs_Strategy_Deck.pptx"))
BUILDERS = [s01, s02, s03, s04, s05, s06, s07, s08, s09, s10, s11, s12, s13, s14, s15]

if __name__ == "__main__":
    prs = new_deck()
    for b in BUILDERS:
        b(prs)
    prs.save(OUT)
    print("slides:", len(prs.slides._sldIdLst), "->", OUT)
