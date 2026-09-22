# -*- coding: utf-8 -*-
"""Slides 7-15 of the minimal deck."""
from gl_min2 import *


# ---------------------------------------------------------------- S7 industry & porter
def s07(prs):
    s = blank(prs)
    head(s, 7, "Industrial analysis · Porter", "A growth-stage industry; two high forces.")
    comps = [("Nike / Adidas / Puma / NB", "₹4k–20k+", "HIGH — own the aspiration being attacked"),
             ("Comet", "$6.6M", "HIGH — same buyer, ~10× their FY25 revenue"),
             ("CHK (Accel)", "$3.8M", "MEDIUM-HIGH — closest design rival"),
             ("RapidBox", "$9.5M", "MEDIUM — a price tier below"),
             ("Neemans / Solethreads / Zeesh", "seed–A", "LOW-MEDIUM — different promise")]
    y = 2.05
    tb(s, M, 2.0, 5.0, 0.25, "THE COMPETITIVE SET", size=9, color=BROWN, spc=1.5)
    for name, fund, threat in comps:
        tb(s, M, y + 0.28, 2.6, 0.3, name, size=9.3, color=INK, bold=True)
        tb(s, M + 2.7, y + 0.28, 1.1, 0.3, fund, size=9.3, color=MUT)
        tb(s, M + 3.85, y + 0.28, 4.3, 0.3, threat, size=9.3, color=SOFT)
        hair(s, M, y + 0.62, 8.15)
        y += 0.62
    tb(s, M, 5.55, 8.1, 0.25, "INDUSTRY CYCLE — EMBRYONIC · GROWTH · SHAKEOUT · MATURITY · DECLINE",
       size=8.8, color=MUT, spc=1.2)
    tb(s, M + 2.35, 5.78, 1.35, 0.02, "", size=1)
    rect(s, M + 2.42, 5.52, 1.15, 0.03, fill=BROWN)
    tb(s, M, 5.95, 8.15, 0.8,
       "They are in the growth stage: position-buying, losses tolerated (₹1 Cr on ₹2.9 Cr is that logic); the "
       "shakeout will punish players with neither scale nor an uncopyable story.",
       size=9.3, color=SOFT, spacing=1.2)
    forces = [("BUYER POWER", "HIGH", "zero switching cost, infinite choice, hype moves fast"),
              ("RIVALRY", "HIGH", "global giants + funded D2C + marketplaces"),
              ("NEW ENTRANTS", "MODERATE", "cheap to start, years to craft a brand"),
              ("SUBSTITUTES", "MODERATE", "resale Jordans, copies, apparel as the same rupee"),
              ("SUPPLIER POWER", "MODERATE", "leather is easy; skilled karigars are the scarce input")]
    y = 2.05
    tb(s, M + 8.55, 2.0, 3.0, 0.25, "FIVE FORCES", size=9, color=BROWN, spc=1.5)
    for name, rating, why in forces:
        tb(s, M + 8.55, y + 0.28, 2.0, 0.3, name, size=9, color=INK, bold=True, spc=1)
        tb(s, M + 10.6, y + 0.26, 1.0, 0.3, rating, size=9.5, color=BROWN, font=DISP, italic=True,
           align=PP_ALIGN.RIGHT)
        tb(s, M + 8.55, y + 0.56, 3.05, 0.3, why, size=8.3, color=MUT)
        hair(s, M + 8.55, y + 0.9, 3.0)
        y += 0.9
    tb(s, M + 8.55, 6.6, 3.05, 0.5,
       "Verdict: with two HIGH forces, the moat cannot be the design — it is factory + karigars + community + store.",
       size=9.3, color=BROWN, italic=True, font=DISP, spacing=1.2)
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
    x = M
    w = (CW - 3 * 0.25) / 4
    for name, items in ps:
        tb(s, x, 2.05, w, 0.25, name, size=10, color=BROWN, bold=True, spc=2)
        hair(s, x, 2.32, w)
        yy = 2.44
        for it in items:
            tb(s, x, yy, w, 0.7, it, size=8.6, color=SOFT, spacing=1.12)
            yy += 0.62
        x += w + 0.25
    hair(s, M, 4.42, CW)
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
        x = M + ci * 5.9
        y = 4.6 + ri * 1.18
        tb(s, x, y, 0.4, 0.4, L, size=16, color=BROWN, font=DISP)
        yy = y + 0.02
        for it in items:
            tb(s, x + 0.55, yy, 5.3, 0.28, "—  " + it, size=8.4, color=SOFT)
            yy += 0.27
    hair(s, M + 5.75, 4.6, 0.011)
    rect(s, M + 5.77, 4.6, 0.011, 2.36, fill=LINE)
    hair(s, M, 5.72, CW)
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
    y = 2.05
    for name, tag, txt in dirs:
        tb(s, M, y, 2.6, 0.3, name, size=9.3, color=INK, bold=True, spc=1.2)
        col = BROWN if tag == "NOW" else (MUT if tag == "LATER" else RGBColor(0xA0, 0x52, 0x3C))
        tb(s, M + 2.7, y, 0.8, 0.3, tag, size=9, color=col, font=DISP, italic=True)
        tb(s, M + 3.6, y, 4.55, 0.45, txt, size=9, color=SOFT, spacing=1.12)
        hair(s, M, y + 0.5, 8.15)
        y += 0.62
    tb(s, M, 5.35, 8.15, 0.9,
       "“They would rather stay small than cheap.” Read that row out loud: retrenchment refused as policy is "
       "the clearest statement of what this brand is.", size=10.5, color=BROWN, font=DISP, italic=True, spacing=1.25)
    lv = [("CORPORATE — where", "culture-led premium footwear + apparel; vertically integrated; equity-financed"),
          ("BUSINESS — how", "focused differentiation in the ₹4–15k niche; never cost leadership"),
          ("FUNCTIONAL — who", "design: 60-day drops · ops: karigar capacity · marketing: community · retail: membership")]
    y = 2.05
    tb(s, M + 8.55, 2.0, 3.0, 0.25, "LEVELS OF STRATEGY", size=9, color=BROWN, spc=1.5)
    for name, txt in lv:
        tb(s, M + 8.55, y + 0.3, 3.0, 0.3, name, size=9.3, color=INK, bold=True, spc=1)
        tb(s, M + 8.55, y + 0.6, 3.0, 0.8, txt, size=8.6, color=SOFT, spacing=1.15)
        y += 1.25
    tb(s, M + 8.55, 5.95, 3.05, 1.0,
       "The tension: three growth vectors on ₹85 lakh of cash and a workforce that must be trained, not hired. "
       "Textbooks call it over-extension; founders call it momentum. Slides 12–13 tell the difference.",
       size=9.3, color=MUT, spacing=1.25)
    notes(s, "Meera, 55 sec. One idea: three growth strategies at once and one refused - read the red row "
             "aloud. Levels in one breath: where / how / who. Finish on the tension box; it sets up evaluation.")


# ---------------------------------------------------------------- S10 BCG + GE
def s10(prs):
    s = blank(prs)
    head(s, 10, "Portfolio tools", "One star, two question marks, and a dog to refuse.")
    # BCG
    bx, by, bs = M, 2.15, 3.5
    hair(s, bx + bs / 2, by, 0.011); rect(s, bx + bs / 2, by, 0.011, bs, fill=LINE)
    rect(s, bx, by, bs, bs, line=LINE, lw=1.0)
    tb(s, bx + 0.08, by + 0.06, 1.6, 0.2, "QUESTION MARKS", size=7.5, color=MUT, spc=1)
    tb(s, bx + bs / 2 + 0.08, by + 0.06, 1.2, 0.2, "STARS", size=7.5, color=BROWN, spc=1)
    tb(s, bx + 0.08, by + bs / 2 + 0.06, 1.0, 0.2, "DOGS", size=7.5, color=MUT, spc=1)
    tb(s, bx + bs / 2 + 0.08, by + bs / 2 + 0.06, 1.5, 0.2, "CASH COWS", size=7.5, color=MUT, spc=1)
    def circ(x, y, d, lab):
        rect(s, x, y, d, d, line=BROWN, lw=1.2, shape=MSO_SHAPE.OVAL)
        tb(s, x - 0.4, y + d + 0.02, d + 0.8, 0.4, lab, size=7.5, color=SOFT, align=PP_ALIGN.CENTER, spacing=1.0)
    circ(bx + bs / 2 + 0.85, by + 0.5, 0.55, "GL 001")
    circ(bx + 0.5, by + 0.55, 0.42, "collabs ·\nUS/diaspora")
    circ(bx + 0.6, by + bs / 2 + 0.45, 0.36, "apparel")
    circ(bx + 0.28, by + bs / 2 + 1.15, 0.3, "discount\nvolume")
    tb(s, bx, by + bs + 0.08, bs, 0.2, "BCG — GROWTH ↑ · SHARE →", size=7.8, color=MUT, spc=1)
    # GE
    gx, gy, gs = M + 4.15, 2.15, 3.5
    rect(s, gx, gy, gs, gs, line=LINE, lw=1.0)
    for k in (1, 2):
        rect(s, gx + k * gs / 3, gy, 0.011, gs, fill=LINE)
        rect(s, gx, gy + k * gs / 3, gs, 0.011, fill=LINE)
    tb(s, gx + 0.06, gy + 0.05, 1.4, 0.2, "INVEST / GROW", size=7.5, color=BROWN, spc=1)
    tb(s, gx + gs - 1.6, gy + gs - 0.24, 1.55, 0.2, "HARVEST / EXIT", size=7.5, color=MUT, spc=1, align=PP_ALIGN.RIGHT)
    def gc(ci, ri, d, lab):
        cx = gx + ci * gs / 3 + gs / 6 - d / 2; cy = gy + ri * gs / 3 + gs / 6 - d / 2
        rect(s, cx, cy, d, d, line=BROWN, lw=1.2, shape=MSO_SHAPE.OVAL)
        tb(s, cx - 0.4, cy + d + 0.02, d + 0.8, 0.4, lab, size=7.5, color=SOFT, align=PP_ALIGN.CENTER, spacing=1.0)
    gc(2, 0, 0.5, "India D2C")
    gc(2, 1, 0.42, "exports")
    gc(1, 1, 0.4, "apparel")
    gc(0, 2, 0.36, "mass basics")
    tb(s, gx, gy + gs + 0.08, gs, 0.2, "GE — ATTRACTIVENESS ↑ · STRENGTH →", size=7.8, color=MUT, spc=1)
    # right
    tb(s, M + 8.1, 2.1, 3.5, 0.25, "THE VERDICT, TWICE", size=9, color=BROWN, spc=1.5)
    tb(s, M + 8.1, 2.42, 3.5, 1.6, [
        "Grow — core India D2C: fund capacity and stores.",
        "Build selectively — exports: prove unit economics first.",
        "Selectivity — apparel: fund only what lifts AOV.",
        "Refuse — discount-led volume: absent today; must stay absent."],
       size=9, color=SOFT, spacing=1.3, after=6)
    tb(s, M + 8.1, 4.5, 3.5, 1.6,
       "“BCG alone would call a 30-month-old brand a dog — it only scores share and growth. GE is the honest "
       "tool for Gully Labs because it scores brand, craft and factory: what they actually own. Used together "
       "both say the same thing: concentrate on the star, cap the question marks, refuse the dog.”",
       size=9.8, color=BROWN, font=DISP, italic=True, spacing=1.3)
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
    y = 2.05
    for name, txt in ch:
        tb(s, M, y, 2.2, 0.3, name, size=9, color=INK, bold=True, spc=1.2)
        tb(s, M + 2.3, y, 5.85, 0.35, txt, size=9, color=SOFT)
        hair(s, M, y + 0.42, 8.15)
        y += 0.5
    tb(s, M, 4.75, 8.1, 0.25, "THE FRESH ₹27.5 Cr, ROUGHLY — CAPACITY FIRST, BRAND SECOND", size=9, color=BROWN, spc=1.5)
    alloc = [("production & karigar skilling", 0.34), ("brand & content", 0.26), ("retail build-out", 0.24),
             ("senior talent & controls", 0.10), ("working capital", 0.06)]
    y = 5.1
    for lab, pct in alloc:
        tb(s, M, y + 0.03, 2.6, 0.25, lab, size=8.6, color=SOFT)
        rect(s, M + 2.7, y, 4.4 * pct / 0.34, 0.22, line=BROWN, lw=1.0)
        tb(s, M + 7.3, y, 0.8, 0.25, "%d%%" % int(pct * 100), size=9.5, color=INK, font=DISP)
        y += 0.3
    tb(s, M, 6.75, 8.1, 0.2, "illustrative split, inferred from stated uses of proceeds — not a company disclosure",
       size=7.8, color=MUT, italic=True)
    tb(s, M + 8.55, 2.0, 3.0, 0.25, "FOUR ACTIONS · BLUE OCEAN", size=9, color=BROWN, spc=1.5)
    acts = [("ELIMINATE", "celebrity-athlete deals; discount-led growth"),
            ("REDUCE", "SKU sprawl; marketplace dependence"),
            ("RAISE", "craft depth, storytelling, store experience, export service"),
            ("CREATE", "the drop ritual, the karigar celebrity, the Baithak, India-origin sneaker IP")]
    y = 2.35
    for name, txt in acts:
        tb(s, M + 8.55, y, 1.35, 0.25, name, size=8.6, color=BROWN, bold=True, spc=1.5)
        tb(s, M + 8.55, y + 0.28, 3.05, 0.5, txt, size=8.6, color=SOFT, spacing=1.12)
        y += 0.85
    tb(s, M + 8.55, 5.7, 3.05, 0.25, "DESIGN THINKING, LIVED — 2022–23", size=8, color=MUT, spc=1)
    tb(s, M + 8.55, 5.98, 3.05, 1.0,
       "Six months of community posts → “India needs its own sneaker” → Baithak sessions → 50 pairs at ₹1,000 "
       "(worth ₹6,000) → the 250-pair sellout. The method is in the name: Labs.",
       size=8.8, color=SOFT, spacing=1.22)
    notes(s, "Meera, 65 sec. Three beats: name two challenges (craft scaling, Rs 85 lakh); design thinking - "
             "first product was a Rs 1,000 prototype of a Rs 6,000 shoe; blue ocean - read the four actions. "
             "If long, skip the bars and say 'capacity first, brand second'.")


# ---------------------------------------------------------------- S12 BSC
def s12(prs):
    s = blank(prs)
    head(s, 12, "Evaluation · measuring performance", "Profit alone would have killed it in 2023.")
    quads = [("FINANCIAL", [("revenue", "₹2.9 Cr", "₹16→30 Cr"), ("CM2 margin", "~11%", "18%"),
                            ("marketing / rev", "~35%", "22%"), ("cash", "₹85 lakh", "12-mo runway")]),
             ("CUSTOMER", [("repeat rate", "n.d.", "25–35%"), ("7-day sell-through", "variable", ">80%"),
                           ("export share", "~25%", "35%"), ("owned community", "3.6k", "100k")]),
             ("INTERNAL PROCESS", [("pairs / month", "~5,000", "12,000"), ("days per pair", "~4", "2.5"),
                                   ("returns", "n.d.", "<5%"), ("on-time drops", "60-day", "100%")]),
             ("LEARNING & GROWTH", [("karigars", "~100", "220"), ("attrition", "n.d.", "<7%"),
                                    ("new silhouettes / yr", "2–3", "4"), ("senior hires", "started", "6")])]
    x = M
    w = (CW - 3 * 0.25) / 4
    for name, rows in quads:
        tb(s, x, 2.05, w, 0.25, name, size=9.5, color=BROWN, bold=True, spc=1.8)
        hair(s, x, 2.32, w)
        yy = 2.45
        for m, a, b in rows:
            tb(s, x, yy, w * 0.5, 0.3, m, size=8.6, color=INK, bold=True)
            tb(s, x + w * 0.5, yy, w * 0.24, 0.3, a, size=8.4, color=MUT)
            tb(s, x + w * 0.74, yy, w * 0.26, 0.3, b, size=8.6, color=BROWN, font=DISP)
            yy += 0.42
        x += w + 0.25
    hair(s, M, 4.35, CW)
    tb(s, M, 4.5, 11.5, 0.8,
       "“A financial-only scorecard would have told them to shut the factory in 2023. The balanced one is what "
       "justifies investing in brand, craft and community before profit.” Red-ish numbers above are disclosed "
       "baselines; the Georgia targets are our proposed FY27 scoreboard — say that distinction out loud.",
       size=10.5, color=SOFT, font=DISP, italic=True, spacing=1.3)
    tb(s, M, 5.6, 11.5, 0.3, "READ ONE ROW PER QUADRANT ALOUD: revenue · repeat purchase · pairs per month · karigar attrition.",
       size=9, color=MUT, spc=1.2)
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
    y = 2.05
    tb(s, M, 2.0, 5.0, 0.25, "STRATEGIC  vs  OPERATIONAL  CONTROL", size=9, color=BROWN, spc=1.5)
    for a, b, c in ctl:
        tb(s, M, y + 0.3, 1.3, 0.3, a, size=8.8, color=INK, bold=True, spc=1)
        tb(s, M + 1.4, y + 0.3, 3.2, 0.4, b, size=8.8, color=SOFT)
        tb(s, M + 4.7, y + 0.3, 3.4, 0.4, c, size=8.8, color=SOFT)
        hair(s, M, y + 0.72, 8.15)
        y += 0.66
    tb(s, M, 5.5, 8.15, 0.25, "STRATEGY AUDIT — SIX QUESTIONS", size=9, color=BROWN, spc=1.5)
    tb(s, M, 5.8, 8.15, 1.1,
       "01 mission still relevant? · 02 is the appraisal honest (56% GM, 11% CM2)? · 03 objectives vs resources "
       "(₹85 lakh vs 3 stores)? · 04 4Ps internally consistent? · 05 risk acceptable to the board? · 06 are the "
       "controls adequate — and independent?",
       size=8.8, color=SOFT, spacing=1.3)
    tb(s, M + 8.55, 2.0, 3.05, 1.1,
       "“A new customer-service hire used admin access to create 100%-discount codes, shipped ₹2 lakh of "
       "sneakers to friends, and quit in a week. The system showed the orders as paid.”",
       size=10.5, color=INK, font=DISP, italic=True, spacing=1.3)
    tb(s, M + 8.55, 3.35, 3.05, 1.45, [
        "done — role-based permissioning, discount limits",
        "add — segregation of duties; four-eyes on discounts",
        "add — weekly anomaly report; surprise stock counts",
        "add — written incident protocol; one accountable owner"],
       size=8.8, color=SOFT, spacing=1.3, after=5)
    tb(s, M + 8.55, 5.02, 3.05, 0.25, "PRE-PLANNED SCENARIOS, WITH TRIGGERS", size=9, color=BROWN, spc=1.5)
    tb(s, M + 8.55, 5.3, 3.05, 1.5,
       "hero-SKU fatigue (2 drops <70% sell-through) · karigar supply shock (utilisation >90% a quarter) · cash "
       "crunch (runway <6 mo → pause store #3, not marketing) · authenticity attack (sentiment spike → "
       "founder-led reply in 24 h). Triggers are what turn a plan into a control.",
       size=8.8, color=SOFT, spacing=1.28)
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
    w = (CW - 2 * 0.3) / 3
    for name, items in cols:
        tb(s, x, 2.05, w, 0.25, name, size=9.5, color=BROWN, bold=True, spc=1.8)
        hair(s, x, 2.32, w)
        yy = 2.45
        for it in items:
            tb(s, x, yy, w, 0.35, "—  " + it, size=9, color=SOFT)
            yy += 0.4
        x += w + 0.3
    hair(s, M, 4.35, CW)
    recs = [("01", "TREAT CRAFT LIKE TECHNOLOGY", "a karigar academy: certification, retention-linked pay, a documented pipeline — capacity is the ceiling, and the only lever that doesn’t dilute the hand-made claim"),
            ("02", "MAKE EXPORTS THE MARGIN ENGINE", "the diaspora pays $140–220 with no 18% slab and lower CAC; target 35% exports; a US pop-up before a permanent store"),
            ("03", "PROTECT THE PREMIUM WITH CONTROLS", "formal discount authority, an independent audit-minded director, a sell-through-based drop calendar — scarcity only works if nobody believes a sale is coming")]
    y = 4.5
    for num, t, d in recs:
        tb(s, M, y, 0.7, 0.5, num, size=20, color=BROWN, font=DISP)
        tb(s, M + 0.85, y + 0.04, 4.0, 0.3, t, size=10, color=INK, bold=True, spc=1.5)
        tb(s, M + 0.85, y + 0.34, 10.6, 0.4, d, size=9, color=SOFT, spacing=1.12)
        hair(s, M, y + 0.76, CW)
        y += 0.82
    notes(s, "Kabir, 60 sec. Vote first, count, say 'yes, conditionally'. One sentence per recommendation. "
             "Closing argument from the verdict column: culture is a moat only because of factory + karigars + "
             "store. That is the line the examiner remembers.")


# ---------------------------------------------------------------- S15 close
def s15(prs):
    s = blank(prs)
    tb(s, M, 1.1, 8.0, 1.0, "Questions?", size=46, color=INK, font=DISP)
    hair(s, M, 2.35, CW)
    tb(s, M, 2.55, 6.5, 0.25, "IF ASKED ONLY ONE THING", size=9, color=BROWN, spc=1.5)
    tb(s, M, 2.85, 6.5, 1.6,
       "“Is the strategy sustainable?” — The strategy is coherent and the environment favourable; the risk is "
       "not strategic but financial and organisational. A brand with 56% gross margin, 11% contribution and "
       "₹85 lakh of cash is one bad quarter from choosing between its stores and its positioning. Evaluation "
       "exists to catch that moment early.",
       size=10.5, color=SOFT, font=DISP, italic=True, spacing=1.3)
    tb(s, M, 4.7, 6.5, 0.25, "BACKUP, READY TO JUMP TO", size=9, color=BROWN, spc=1.5)
    tb(s, M, 5.0, 6.5, 1.2,
       "unit economics (100→56→46→11) · funding & cap table (₹8.7 Cr seed · ₹26.5 Cr A @ ₹147 Cr · Shark Tank ₹175 Cr) "
       "· GST math (~₹600–1,200/pair) · competitor deep-dive (Comet ₹29 Cr FY25) · why own the factory (35% duty shield)",
       size=9, color=SOFT, spacing=1.3)
    tb(s, M + 7.0, 2.55, 4.6, 0.25, "SOURCES CHECKED FOR THIS STUDY", size=9, color=BROWN, spc=1.5)
    tb(s, M + 7.0, 2.85, 4.6, 2.6, [
        "gullylabs.com · global.gullylabs.com (story, pricing, ranges)",
        "@GullyLabs on X — 3,640 followers, posts to 19 Sep 2026",
        "Financial Express, 28 Jan 2026 — FY24/25 financials, unit economics",
        "Inc42, 23 Jan 2026 — founding, capacity, FY26/27 targets",
        "Indian Retailer — seed, Series A, flagship store",
        "Mint, 22 Feb 2026 — the ₹2 lakh fraud",
        "PIB, Sep 2025 — GST rationalisation · Tracxn — competitor set",
        "Architectural Digest India — flagship interior (photo)"],
       size=8.8, color=SOFT, spacing=1.35, after=3)
    photo(s, "gully-labs-sneakers-delhi-campaign-india-5.jpg", 9.6, 5.0, 2.0, 1.6)
    tb(s, M, 6.68, 11.5, 0.3, "Thank you — Aisha · Rohan · Meera · Kabir · Business Policy & Strategic Management, September 2026",
       size=9, color=MUT)
    folio(s, 15)
    notes(s, "Kabir, 15 sec + Q&A. Thank the room, invite questions; if silent, ask the verdict question "
             "yourself. Keep the five backups ready - funding and GST are the likeliest probes.")
