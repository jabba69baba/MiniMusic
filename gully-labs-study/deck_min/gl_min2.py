# -*- coding: utf-8 -*-
"""Gully Labs deck v2 — minimal editorial system per DESIGN.md (SlidesCarnival brown template)."""
import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from PIL import Image as PILImage

SW, SH = 13.3333, 7.5
M = 0.9; CW = SW - 2 * M
BG   = RGBColor(0xF4, 0xEF, 0xE6)
INK  = RGBColor(0x22, 0x1B, 0x15)
SOFT = RGBColor(0x4A, 0x44, 0x3C)
MUT  = RGBColor(0x8A, 0x81, 0x77)
LINE = RGBColor(0xCF, 0xC5, 0xB4)
BROWN= RGBColor(0x8C, 0x62, 0x48)
DISP = "Georgia"; BODY = "Calibri"
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

def ttl(s, x, y, text, size=29):
    tb(s, x, y, 11.6, 0.9, text, size=size, color=INK, font=DISP)

def folio(s, n):
    tb(s, M, 7.06, 7, 0.2, "GULLY LABS — CULTURE AS CAPITAL", size=8.5, color=MUT, spc=1.5)
    tb(s, SW - M - 1.4, 7.06, 1.4, 0.2, "%02d / 15" % n, size=8.5, color=MUT, align=PP_ALIGN.RIGHT, spc=1.5)

def head(s, n, k, t):
    kick(s, M, 0.72, k); ttl(s, M, 0.98, t); hair(s, M, 1.78, CW); folio(s, n)

def photo(s, fname, x, y, w, h, cap=None):
    src = os.path.join(ASSETS, fname); dst = os.path.join(ASSETS, "_crop_" + fname)
    im = PILImage.open(src); W, H = im.size; tar = w / h; cur = W / H
    if cur > tar:
        nw = int(H * tar); im = im.crop(((W - nw) // 2, 0, (W - nw) // 2 + nw, H))
    else:
        nh = int(W / tar); im = im.crop((0, (H - nh) // 2, W, (H - nh) // 2 + nh))
    im.convert("RGB").save(dst, quality=90)
    rect(s, x + 0.07, y + 0.07, w, h, line=BROWN, lw=1.0)
    s.shapes.add_picture(dst, Inches(x), Inches(y), Inches(w), Inches(h))
    if cap:
        tb(s, x, y + h + 0.10, w + 0.5, 0.4, cap, size=8.5, color=MUT, spacing=1.05)

def notes(s, t):
    s.notes_slide.notes_text_frame.text = t


# ---------------------------------------------------------------- S1 cover
def s01(prs):
    s = blank(prs)
    kick(s, M, 0.95, "Brand study & strategic analysis · Business policy")
    tb(s, M, 1.45, 8.2, 2.0, [[("Culture as capital.", {"size": 46, "font": DISP, "color": INK})]], )
    tb(s, M, 2.75, 7.4, 1.0,
       [[("How Gully Labs is trying to build India’s first globally aspirational sneaker house — ", {}),
         ("and whether the strategy can survive scale, cash and competition.", {"italic": True})]],
       size=13, color=MUT, font=DISP, spacing=1.25)
    stats = [("Aug 2023", "founded, Delhi NCR"), ("₹4k–15k", "price band / pair"),
             ("₹175 Cr", "Shark Tank valuation"), ("~25%", "revenue from exports")]
    x = M
    hair(s, M, 4.35, 7.4)
    for v, l in stats:
        tb(s, x, 4.5, 1.8, 0.4, v, size=20, color=INK, font=DISP)
        tb(s, x, 4.92, 1.8, 0.4, l, size=8.5, color=MUT)
        x += 1.9
    hair(s, M, 5.6, 7.4)
    tb(s, M, 5.75, 7.4, 0.3, "AISHA · ROHAN · MEERA · KABIR", size=9.5, color=SOFT, spc=2)
    tb(s, M, 6.05, 7.4, 0.3, "September 2026 · 15 slides · 11 minutes · sources on the final slide", size=8.5, color=MUT)
    photo(s, "gully-labs-sneakers-delhi-campaign-india-3.jpg", 9.0, 1.5, 3.5, 3.1,
          "Arjun Singh & Animesh Mishra — “unapologetically Indian” (Inc42).")
    folio(s, 1)
    notes(s, "OPEN (Aisha, 30 sec). Do not read the title. Say: 'Nike sells a swoosh. Gully Labs sells a street "
             "in Delhi. Same category, different strategy question - can culture be a defensible advantage?' "
             "Point at the photo: two founders, one manifesto. Hand to slide 2.")


# ---------------------------------------------------------------- S2 roadmap
def s02(prs):
    s = blank(prs)
    head(s, 2, "Roadmap & speaker map", "One brand, five lenses, eleven minutes.")
    tb(s, M, 2.0, 11.5, 0.3,
       [[("Before we start — hands up if you own sneakers above ", {}), ("₹5,000", {"bold": True, "color": INK}),
         ("; keep them up if they are ", {}), ("not", {"italic": True, "color": INK}), (" a global brand.", {})]],
       size=11.5, color=SOFT)
    rows = [("01", "Who & where", "Aisha · 0:00–2:30", "S1–S5",
             "Cover · brand snapshot · mission, values, culture · PESTEL"),
            ("02", "The arena", "Rohan · 2:30–5:15", "S6–S8",
             "Value chain · industry cycle · Porter · 4P · SWOT"),
            ("03", "The plan", "Meera · 5:15–8:30", "S9–S11",
             "Directional strategy & levels · BCG + GE · implementation & blue ocean"),
            ("04", "The verdict", "Kabir · 8:30–11:15", "S12–S15",
             "Balanced scorecard · control & contingency · verdict & recommendations")]
    y = 2.62
    for num, name, who, rng, desc in rows:
        tb(s, M, y, 0.8, 0.5, num, size=26, color=BROWN, font=DISP)
        tb(s, M + 0.95, y + 0.05, 3.2, 0.3, name.upper(), size=12, color=INK, bold=True, spc=2)
        tb(s, M + 0.95, y + 0.36, 4.5, 0.3, desc, size=9, color=MUT)
        tb(s, M + 6.4, y + 0.08, 2.6, 0.3, who, size=9.5, color=MUT)
        tb(s, M + 9.6, y + 0.02, 1.9, 0.4, rng, size=15, color=BROWN, font=DISP, italic=True, align=PP_ALIGN.RIGHT)
        hair(s, M, y + 0.78, CW)
        y += 0.93
    tb(s, M, 6.42, 11.5, 0.3,
       "SYLLABUS — PESTEL · VALUE CHAIN · INDUSTRY CYCLE · PORTER · 4P · SWOT · DIRECTIONAL · LEVELS · BCG · GE · "
       "DESIGN THINKING · RED/BLUE OCEAN · MODEL INNOVATION · BSC · AUDIT · CONTINGENCY · CONTROLS",
       size=8.5, color=MUT, spc=1.2)
    tb(s, M, 6.72, 11.5, 0.3,
       [[("The one question we answer: ", {"italic": True, "color": BROWN}),
         ("is “unapologetically Indian” a strategy, or just a marketing line — and what would it take to make it a moat?",
          {"italic": True, "color": BROWN})]], size=11.5, font=DISP)
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
    hair(s, M, 2.1, 7.6)
    for v, l in kpis:
        tb(s, x, 2.22, 1.9, 0.45, v, size=21, color=INK, font=DISP)
        tb(s, x, 2.68, 1.9, 0.5, l, size=8.3, color=MUT, spacing=1.05)
        x += 1.95
    hair(s, M, 3.3, 7.6)
    miles = [("AUG 23", "launch; 50 prepaid orders, basement shoemaker, 10–20 pairs a month"),
             ("DEC 23", "viral reel — 250 pairs sold out in two days"),
             ("APR 24", "first unit: 1,000 sq ft, six karigars, Noida Sec-10"),
             ("DEC 24", "₹8.7 Cr seed (Zeropearl); own factory; 600 pairs a month"),
             ("SEP 25", "flagship “Baithak” store, Panchsheel Park, Delhi"),
             ("JAN 26", "₹26.5 Cr Series A (Saama) · Shark Tank ₹1 Cr @ ₹175 Cr"),
             ("SEP 26", "Bengaluru store + Kaapi drop; three cities live")]
    y = 3.46
    for d, t in miles:
        tb(s, M, y, 0.95, 0.25, d, size=9.5, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 1.05, y, 6.5, 0.3, t, size=9.5, color=SOFT)
        y += 0.355
    tb(s, M, 6.1, 7.6, 0.8, [
        [("Social: ", {"bold": True, "color": INK, "size": 9}), ("X @GullyLabs, 3,640 followers (verified 19 Sep 2026) — a drop wire, not a reach channel; reach lives on reels (one 7M-view reel made the Dec-23 sellout) and on cause-marketing (15% of three days’ sales to Hemkunt Foundation).", {"size": 9})]],
        size=9, color=MUT, spacing=1.2)
    photo(s, "gully-labs-sneakers-delhi-campaign-india-4.jpg", 9.35, 2.1, 3.1, 3.9,
          "The flagship, Panchsheel Park — karigar studio, shoe museum, Baithak (AD India).")
    tb(s, 9.35, 6.35, 3.2, 0.7,
       "They own the factory, the store and the data: D2C India + global, Amazon store, curated partners; 10,000+ orders shipped.",
       size=8.8, color=MUT, spacing=1.2)
    notes(s, "Aisha, 60 sec. Three claims, not a node-by-node timeline: (1) 20 lakh to 2.9 crore to 16 crore "
             "expected - 80x in two years off a tiny base; (2) they own factory AND store, against everyone's "
             "advice; (3) their X following is 3,640 - reach is reels and word of mouth, which matters later. "
             "Flag the bestseller price Rs 8,900 - Air Force 1 territory.")


# ---------------------------------------------------------------- S4 mission
def s04(prs):
    s = blank(prs)
    head(s, 4, "Business policy", "A mission, four policies, one culture.")
    tb(s, M, 2.05, 0.6, 0.7, "“", size=44, color=BROWN, font=DISP)
    tb(s, M + 0.5, 2.2, 7.5, 1.2,
       "To build a household sneaker brand from Delhi that is truly and unapologetically Indian — shaped by "
       "local streets, crafts and culture, not borrowed from the West.",
       size=15, color=INK, font=DISP, italic=True, spacing=1.3)
    tb(s, M, 3.5, 7.9, 0.7,
       "Vision: put India’s authentic culture on the global sneaker map — “the next great global sneaker brand "
       "out of India, one culturally-charged collection at a time.” Values in behaviour: originality (never a "
       "compromise brand), craft (karigar-first, ~95% local), community (six months of content before product), "
       "story (every colourway named), value-not-price (“India is not price-conscious, it’s value-conscious”).",
       size=10.5, color=SOFT, spacing=1.25)
    pol = [("ORIGIN POLICY", "functional", "own factory; ~95% local sourcing"),
           ("DROP POLICY", "operating", "a themed collection every ~60 days, limited runs"),
           ("PRICE POLICY", "strategic", "premium held at ₹4–15k; no discount-led growth"),
           ("PEOPLE POLICY", "functional", "karigar retention; backend permissioning post-fraud")]
    y = 4.75
    hair(s, M, 4.62, 7.9)
    for a, b, c in pol:
        tb(s, M, y, 1.7, 0.25, a, size=9, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 1.85, y, 1.1, 0.25, b, size=9, color=MUT, italic=True)
        tb(s, M + 3.0, y, 4.9, 0.25, c, size=9, color=SOFT)
        y += 0.33
    tb(s, M, 6.2, 7.9, 0.7,
       "Corporate culture is the control system: the manifesto doubles as hiring filter and design brief; 100 "
       "karigars are the brand’s named heroes; a trust-based culture met its first governance wall in Feb 2026 (S13).",
       size=9.5, color=MUT, spacing=1.25)
    photo(s, "gully-labs-sneakers-delhi-campaign-india-2.jpg", 9.35, 2.1, 3.1, 3.5,
          "Co-founder Arjun Singh — r/Delhi AMA, 2025.")
    notes(s, "Aisha, 60 sec. Framing line: 'A mission is a statement; a policy is the rule that stops you "
             "breaking it.' Give one example per policy type, do not read all four. Land the culture point: "
             "the manifesto does the work an SOP manual does at a big firm - and slide 13 shows the day that "
             "stopped being enough.")


# ---------------------------------------------------------------- S5 PESTEL
def s05(prs):
    s = blank(prs)
    head(s, 5, "Environmental scanning · external", "PESTEL: a friendly climate, with one tax headwind.")
    rows = [("P", "Make in India; 35% BCD + surcharge + IGST on imported footwear (landed ≈45–60% of CIF); BIS/QCO norms; no footwear PLI notified — IFLDP (₹1,700 Cr) is the live scheme.",
             "Tailwind: the Noida factory is a tariff and quality advantage global rivals cannot match at this price."),
            ("E", "GST rationalisation (Sep ’25): footwear ≤₹2,500 at 5%, above ₹2,500 still 18%; category growing 12–15% p.a.; ~70M pairs by 2029.",
             "Headwind in a tailwind: 100% of their range sits in the 18% slab — a ~₹600–1,200/pair penalty vs mass rivals."),
            ("S", "Gen-Z identity buying; hype and resale culture; gully rap and streetwear; diaspora demand; festive spikes.",
             "Core demand: ~25% export share is diaspora + design-led, not price-led."),
            ("T", "D2C stack, drop mechanics, social commerce, creator economy, AI demand forecasting.",
             "Enabler: the 60-day calendar is a software problem as much as a craft one."),
            ("E", "Leather supply chain, tannery effluent norms, air-freight carbon on exports.",
             "Watch item: an ESG narrative they have not yet had to defend."),
            ("L", "Motif/trademark IP hard to protect; BIS and labelling compliance; internal-controls exposure.",
             "Live risk: IP defensibility, and the Feb ’26 fraud was a controls failure, not a market failure.")]
    y = 2.05
    for L, force, impl in rows:
        tb(s, M, y, 0.45, 0.45, L, size=17, color=BROWN, font=DISP)
        tb(s, M + 0.6, y, 5.5, 0.75, force, size=9.3, color=SOFT, spacing=1.15)
        tb(s, M + 6.35, y, 5.2, 0.75, impl, size=9.3, color=SOFT, spacing=1.15)
        hair(s, M, y + 0.78, CW)
        y += 0.83
    tb(s, M, 7.0 - 0.28, 11.5, 0.25,
       "Net read: four tailwinds, one tax penalty, one governance gap — the environment favours the strategy; the 18% slab attacks the price point.",
       size=9.5, color=BROWN, italic=True, font=DISP)
    notes(s, "Aisha, 60 sec. Do not read the rows. Deliver only the implication column for P, E and L. The "
             "killer fact is GST: Sep 2025 cut footwear under Rs 2,500 to 5% but left everything above at 18%, "
             "and 100% of Gully Labs' range is above. End on the italic net read.")


# ---------------------------------------------------------------- S6 value chain
def s06(prs):
    s = blank(prs)
    head(s, 6, "Environmental scanning · internal", "The value chain — and where the ₹100 goes.")
    steps = [("01", "DESIGN & STORY", "cultural archive; 60-day drop brief"),
             ("02", "SOURCING", "~95% local leather, rubber, thread"),
             ("03", "OPERATIONS", "hand-lasted in Noida, ~4 days a pair"),
             ("04", "FULFILMENT", "D2C India + global; free exchange"),
             ("05", "MARKETING & RETAIL", "reels, collabs, Baithak store")]
    x = M
    hair(s, M, 2.1, 8.1)
    w = (8.1 - 4 * 0.18) / 5
    for num, name, desc in steps:
        tb(s, x, 2.2, 0.5, 0.3, num, size=12, color=BROWN, font=DISP)
        tb(s, x, 2.52, w, 0.4, name, size=8.6, color=INK, bold=True, spc=1.2)
        tb(s, x, 2.92, w + 0.2, 0.6, desc, size=8.3, color=MUT, spacing=1.1)
        x += w + 0.18
    hair(s, M, 3.62, 8.1)
    tb(s, M, 3.72, 8.1, 0.3, "THE MARGIN LADDER — ₹100 OF REVENUE (FOUNDERS’ OWN NUMBERS)", size=9, color=BROWN, spc=1.5)
    bars = [("100", 1.0, "revenue"), ("56", 0.62, "after COGS"), ("46", 0.52, "after logistics"),
            ("11", 0.2, "after marketing"), ]
    x = M
    for v, frac, lab in bars:
        hgt = 0.35 + frac * 1.15
        rect(s, x, 5.75 - hgt, 1.5, hgt, line=BROWN, lw=1.0)
        tb(s, x, 5.75 - hgt - 0.32, 1.5, 0.3, v, size=15, color=INK, font=DISP, align=PP_ALIGN.CENTER)
        tb(s, x, 5.82, 1.5, 0.3, lab, size=8.3, color=MUT, align=PP_ALIGN.CENTER)
        x += 1.72
    tb(s, M, 6.35, 8.1, 0.6,
       "FY25 loss ₹1 Cr on ₹2.9 Cr; Delhi store at breakeven; cash ₹85 lakh. Gross margin proves the premium is "
       "accepted; the 35% marketing load proves the brand does not yet pull.",
       size=9.3, color=SOFT, spacing=1.2)
    photo(s, "gully-labs-sneakers-delhi-campaign-india-5.jpg", 9.35, 2.1, 3.1, 3.5,
          "The heel says गली — Devanagari branding, hand-lasted.")
    tb(s, 9.35, 6.0, 3.2, 1.0, [
        "Value is created at the two ends — story and community — not in the middle.",
        "The bottleneck is hands, not machines: demand hit 1,000 pairs/month against 600 of capacity.",
        "Backward integration was the bet: ₹1.2 Cr from college friends to own the line."],
       size=8.8, color=MUT, spacing=1.25, after=4)
    notes(s, "Rohan, 55 sec. Walk the five steps once, then the ladder: '100 rupees in, 11 left - the whole "
             "business case on one line.' Two claims: 56% gross proves pricing works; 35% marketing proves the "
             "brand doesn't pull. Remember-line: they ran out of karigars before they ran out of demand.")
