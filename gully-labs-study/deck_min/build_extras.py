# -*- coding: utf-8 -*-
"""Companion deliverables: speaker script (md), interactive viewer (html), assignment report (docx)."""
import os, json, html
from pptx import Presentation

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
PPTX = os.path.join(ROOT, "Gully_Labs_Strategy_Deck.pptx")

prs = Presentation(PPTX)
notes = []
for sl in prs.slides:
    txt = ""
    if sl.has_notes_slide and sl.notes_slide.notes_text_frame:
        txt = sl.notes_slide.notes_text_frame.text.strip()
    notes.append(txt)

SEGS = [("Aisha", 1, 5, "0:00–2:30"), ("Rohan", 6, 8, "2:30–5:15"),
        ("Meera", 9, 11, "5:15–8:30"), ("Kabir", 12, 15, "8:30–11:15")]

# ---------------- speaker script ----------------
md = []
md.append("# Gully Labs — Culture as Capital · Speaker Script")
md.append("")
md.append("15 slides · 11 minutes 15 seconds · four speakers. Times are cumulative cues; each speaker owns the "
          "clicker for their segment. Interactive beats are marked **INTERACT**. All figures are sourced on the "
          "final slide; numbers marked *proposed* are our targets, not company disclosures.")
md.append("")
for name, a, b, rng in SEGS:
    md.append("## %s — slides %d–%d (%s)" % (name, a, b, rng))
    md.append("")
    for i in range(a, b + 1):
        md.append("### Slide %d — %s" % (i, "notes"))
        md.append("")
        md.append(notes[i - 1] or "(no notes)")
        md.append("")
md.append("---")
md.append("")
md.append("**INTERACT checklist:** S2 hands-up opener · S7 force poll · S10 sock-quiz · S12 read-aloud rows · "
          "S14 show-of-hands verdict · S15 Q&A with five backups.")
open(os.path.join(ROOT, "speaker_script.md"), "w").write("\n".join(md))

# ---------------- viewer html ----------------
slides_json = []
for i, t in enumerate(notes, 1):
    slides_json.append({"n": i, "img": "deck_min/preview/slide_%02d.png" % i, "notes": t})
viewer = """<!doctype html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Gully Labs — Culture as Capital · Deck</title>
<style>
 body{margin:0;background:#221B15;font:15px/1.55 Calibri,Segoe UI,sans-serif;color:#F4EFE6}
 header{position:fixed;top:0;left:0;right:0;padding:10px 18px;background:#171009;display:flex;gap:14px;align-items:center;z-index:2}
 header b{font:italic 20px Georgia,serif}
 header span{color:#8A8177;font-size:12px;letter-spacing:1px;text-transform:uppercase}
 main{max-width:1150px;margin:64px auto 30px;padding:0 16px}
 img{width:100%;border:1px solid #8C6248;display:block}
 #notes{background:#2b221a;border-left:3px solid #8C6248;padding:12px 16px;margin-top:12px;color:#d8cfc2;min-height:60px}
 #notes i{color:#C9A227}
 nav{display:flex;gap:10px;align-items:center;margin:12px 0}
 button{background:none;border:1px solid #8C6248;color:#F4EFE6;padding:6px 14px;cursor:pointer;font:12px Calibri;letter-spacing:1px}
 button:hover{background:#8C6248}
 #cnt{color:#8A8177;letter-spacing:1px}
 #dots{margin-left:auto;display:flex;gap:5px;flex-wrap:wrap}
 #dots a{width:10px;height:10px;border:1px solid #8C6248;display:inline-block;cursor:pointer}
 #dots a.on{background:#8C6248}
</style></head><body>
<header><b>Gully Labs</b><span>culture as capital · brand study &amp; strategic analysis · 4 speakers · 11 min</span></header>
<main><img id="im" alt="slide"><div id="notes"></div>
<nav><button id="p">← PREV</button><span id="cnt"></span><button id="n">NEXT →</button><span id="dots"></span></nav>
</main>
<script>
const S=__DATA__;let i=0;
function esc(t){const d=document.createElement('div');d.textContent=t;return d.innerHTML}
function show(){const s=S[i];document.getElementById('im').src=s.img;
document.getElementById('notes').innerHTML='<i>'+s.n+' / 15 · speaker notes</i><br>'+esc(s.notes);
document.getElementById('cnt').textContent=(i+1)+' / 15';
[...document.querySelectorAll('#dots a')].forEach((a,k)=>a.className=k===i?'on':'');}
S.forEach((s,k)=>{const a=document.createElement('a');a.title='slide '+(k+1);a.onclick=()=>{i=k;show()};document.getElementById('dots').appendChild(a)});
document.getElementById('p').onclick=()=>{i=(i+14)%15;show()};
document.getElementById('n').onclick=()=>{i=(i+1)%15;show()};
document.addEventListener('keydown',e=>{if(e.key==='ArrowRight'){i=(i+1)%15;show()}if(e.key==='ArrowLeft'){i=(i+14)%15;show()}});
show();
</script></body></html>"""
viewer = viewer.replace("__DATA__", json.dumps(slides_json))
open(os.path.join(ROOT, "viewer.html"), "w").write(viewer)

# ---------------- report docx ----------------
from docx import Document
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH

doc = Document()
st = doc.styles["Normal"]
st.font.name = "Calibri"
st.font.size = Pt(11)

def h(t, sz=14, before=10):
    p = doc.add_paragraph()
    r = p.add_run(t)
    r.bold = True
    r.font.size = Pt(sz)
    r.font.color.rgb = RGBColor(0x8C, 0x62, 0x48)
    p.paragraph_format.space_before = Pt(before)
    return p

def body(t, italic=False):
    p = doc.add_paragraph(t)
    p.paragraph_format.space_after = Pt(4)
    if italic:
        for r in p.runs:
            r.italic = True
    return p

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("GULLY LABS — CULTURE AS CAPITAL")
r.bold = True; r.font.size = Pt(18)
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("A Brand Study & Strategic Analysis · Business Policy and Strategic Management")
r.font.size = Pt(11); r.italic = True
p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("Team: Aisha · Rohan · Meera · Kabir · September 2026")
r.font.size = Pt(10)

h("0. Executive summary", 13)
body("Gully Labs (Delhi NCR, est. August 2023) sells hand-lasted, culturally themed sneakers at ₹3,790–15,000 "
     "and is the most expensive Indian sneaker company that is not a volume player: ₹20 lakh (FY24) → ₹2.9 Cr "
     "(FY25) → ₹16 Cr projected (FY26), with a ₹1 Cr FY25 loss that is position-buying, not failure. Its strategy "
     "is focused differentiation — culture as capital — enforced by backward integration (own Noida factory, ~100 "
     "karigars), scarcity mechanics (60-day drops), and community-led promotion. This report applies the course "
     "toolkit — PESTEL, value chain, industry-cycle and Porter analysis, 4P/SWOT, directional and portfolio "
     "strategy, implementation, balanced scorecard and control — and concludes the strategy is coherent and the "
     "environment favourable, while the binding constraints are financial (CM2 ~11%, cash ₹85 lakh) and "
     "organisational (craft capacity, controls). Three recommendations follow: treat craft like technology, make "
     "exports the margin engine, and protect the premium with formal controls.")

h("1. Introduction, scope and method", 13)
body("The study treats the brand primarily through its most active social channel, X (@GullyLabs, 3,640 "
     "followers, posting verified to 19 Sep 2026), triangulated with gullylabs.com and global.gullylabs.com, "
     "Financial Express (28 Jan 2026), Inc42 (23 Jan 2026), Indian Retailer, Mint (22 Feb 2026), PIB (Sep 2025) "
     "and Tracxn. Where figures are company-disclosed they are labelled as such; where we infer (capital "
     "allocation, FY27 scorecard targets) it is marked as proposed. The companion deck (15 slides, four speakers, "
     "11 minutes) follows the same evidence base.")

h("2. Brand snapshot and timeline", 13)
body("Founded August 2023 by Arjun Singh and Animesh Mishra with 50 prepaid orders and a basement shoemaker; "
     "December 2023 viral reel (7M views) sells out 250 pairs in two days; April 2024 first 1,000 sq ft unit with "
     "six karigars in Noida Sec-10; December 2024 ₹8.7 Cr seed (Zeropearl) funds the own factory (600 pairs/month); "
     "September 2025 flagship “Baithak” store, Panchsheel Park, Delhi; January 2026 ₹26.5 Cr Series A (Saama) at "
     "₹147 Cr plus Shark Tank India deal at ₹175 Cr; by September 2026 three cities live (Delhi, Bengaluru, "
     "Mumbai next) and ~25% of revenue from exports (US, UK, UAE, SG, AU, CA). Product: four silhouettes (GL 001 "
     "Low/High, 003, 004), ~50 SKUs named in an Indian lexicon (Baaz, Kulfi, Kaapi, Calico White bestseller at "
     "₹8,900), signatures in Kantha, Rangoli and Devanagari.")

h("3. Environmental scanning — PESTEL", 13)
body("Political: Make in India; 35% BCD + surcharge + IGST on imported footwear (landed ≈45–60% of CIF) shields "
     "the Noida factory; no footwear PLI notified (IFLDP ₹1,700 Cr is the live scheme). Economic: GST "
     "rationalisation (Sep 2025) left footwear above ₹2,500 at 18% — 100% of Gully Labs’ range sits in that slab, a "
     "≈₹600–1,200/pair penalty versus mass rivals; category still grows 12–15% p.a. Social: Gen-Z identity "
     "buying, resale/hype culture, gully rap and streetwear, diaspora demand — the ~25% export share is design-"
     "led, not price-led. Technological: D2C stack, drop mechanics, social commerce, creator economy, AI demand "
     "forecasting; the 60-day calendar is a software problem as much as a craft one. Environmental: leather "
     "supply chain, tannery effluent norms, air-freight carbon — a narrative not yet defended. Legal: motif/"
     "trademark IP hard to protect; BIS/labeling compliance; internal-controls exposure (the Feb 2026 fraud was a "
     "controls failure, not a market failure). Net read: four tailwinds, one tax penalty, one governance gap.")

h("4. Internal analysis — value chain and unit economics", 13)
body("Primary activities: design and story (cultural archive, 60-day drop brief) → sourcing (~95% local leather, "
     "rubber, thread) → operations (hand-lasted in Noida, ~4 days a pair) → fulfilment (D2C India + global, free "
     "exchange) → marketing and retail (reels, collabs, Baithak store). The margin ladder on ₹100 of revenue "
     "(founders’ own numbers): 100 → 56 after COGS → 46 after logistics → 11 after marketing. FY25 loss ₹1 Cr on "
     "₹2.9 Cr; Delhi store at breakeven; cash ₹85 lakh. Interpretation: the 56% gross margin proves the premium is "
     "accepted; the 35% marketing load proves the brand does not yet pull. Value is created at the two ends — "
     "story and community — while the bottleneck is hands, not machines: demand hit 1,000 pairs/month against 600 "
     "of capacity, which is why backward integration (₹1.2 Cr from college friends to own the line) was the "
     "defining bet.")

h("5. Industry, competitive and Porter analysis", 13)
body("Industry cycle: the Indian premium-sneaker segment is in the growth stage — position-buying with tolerated "
     "losses; the coming shakeout will punish players with neither scale nor an uncopyable story. Competitive "
     "set: Nike/Adidas/Puma/NB (₹4k–20k+, HIGH threat — they own the aspiration being attacked), Comet ($6.6M "
     "raised, HIGH — same buyer at ~10× Gully Labs’ FY25 revenue), CHK/Accel ($3.8M, MEDIUM-HIGH, closest design "
     "rival), RapidBox ($9.5M, MEDIUM, a price tier below), Neemans/Solethreads/Zeesh (LOW-MEDIUM, different "
     "promise). Five forces: buyer power HIGH (zero switching cost), rivalry HIGH, new entrants MODERATE (cheap "
     "to start, years to craft a brand), substitutes MODERATE (resale Jordans, copies, apparel for the same "
     "rupee), supplier power MODERATE (leather easy; skilled karigars scarce). With two HIGH forces, the moat "
     "cannot be design — it is factory + karigars + community + store.")

h("6. Situational analysis — 4P and SWOT", 13)
body("Product: 4 silhouettes, ~50 SKUs, hand-lasted, scarcity drops and collabs (RAGA, Nivia, CMF). Price: "
     "₹3,790–15,000, value-based (“not price-conscious, value-conscious”), no discount-led growth, global "
     "$140–220. Place: D2C-first India + global, Amazon store, own flagship + Bengaluru, curated partners, ~25% "
     "exports. Promotion: community before product, creator spikes, Shark Tank as brand event, cause-marketing "
     "(15% of three days’ sales to Hemkunt Foundation), X as the drop wire. The four Ps point the same way — that "
     "consistency is the strategy. SWOT — S: own factory (quality, flexibility, duty shield), 56% gross margin, "
     "unmatched cultural IP, named craft workforce. W: CM2 ~11%, marketing 35% of revenue, cash ₹85 lakh versus a "
     "retail build-out, capacity capped by trained hands, 100% of SKUs in the 18% GST slab. O: category +12–15%, "
     "premium band expanding, diaspora and a US store in 12–18 months, apparel/accessories lifting AOV, tier-2 "
     "aspiration, experiential retail. T: global brands out-spend and out-collab overnight, funded domestic "
     "rivals, motifs copyable plus authenticity backlash, valuation far ahead of revenue.")

h("7. Strategy formulation — directions, levels, portfolio", 13)
body("Directional: market penetration NOW (deepen Delhi NCR, repeat buyers, AOV), market development NOW "
     "(Bengaluru, Mumbai, tier-2; exports; US store in 12–18 months), product development NOW (002/003/004, "
     "apparel, jerseys, slides, collabs), diversification LATER (experience retail; nothing unrelated), "
     "retrenchment NEVER (no discounting, no marketplace volume, no contract manufacturing) — “they would rather "
     "stay small than cheap.” Levels: corporate = culture-led premium footwear + apparel, vertically integrated, "
     "equity-financed; business = focused differentiation in the ₹4–15k niche, never cost leadership; functional "
     "= 60-day drops, karigar capacity, community marketing, membership retail. Portfolio: BCG gives one star "
     "(GL 001 India D2C), question marks (collabs, exports/diaspora, apparel) and a dog to refuse (discount-led "
     "volume); the GE matrix, which scores brand/craft/factory rather than share alone, places India D2C in "
     "invest/grow, exports and apparel in selectivity, mass basics in harvest/exit. Used together both tools say: "
     "concentrate on the star, cap the question marks, refuse the dog.")

h("8. Implementation — challenges, allocation, blue ocean, design thinking", 13)
body("Where strategies like this die: scaling craft (trained hands, not machines — 1,000 wanted vs 600 made), "
     "funding the build (₹85 lakh against three stores and a factory expansion), holding the line (marketplaces "
     "and festivals push discounting), governance (founder-trust culture meeting payroll-scale risk), and "
     "cross-border ops (6–8 day global delivery, returns, duties on 25% of revenue). Proposed split of the fresh "
     "₹27.5 Cr (inferred from stated uses of proceeds, not a disclosure): production & karigar skilling 34%, "
     "brand & content 26%, retail build-out 24%, senior talent & controls 10%, working capital 6% — capacity "
     "first, brand second. Blue-ocean reading: eliminate celebrity-athlete deals and discount-led growth; reduce "
     "SKU sprawl and marketplace dependence; raise craft depth, storytelling, store experience, export service; "
     "create the drop ritual, the karigar celebrity, the Baithak, India-origin sneaker IP. Design thinking is in "
     "the name: six months of community posts (empathise) → “India needs its own sneaker” (define) → Baithak "
     "sessions (ideate) → 50 pairs at ₹1,000 of a ₹6,000 shoe (prototype) → the 250-pair sellout (test).")

h("9. Evaluation and control — scorecard, controls, contingency", 13)
body("A financial-only scorecard would have told them to shut the factory in 2023; the balanced one justifies "
     "investing in brand, craft and community before profit. Disclosed baselines vs proposed FY27 targets: "
     "financial — revenue ₹2.9 Cr → ₹16–30 Cr, CM2 ~11% → 18%, marketing/revenue ~35% → 22%, cash ₹85 lakh → "
     "12-month runway; customer — repeat rate n.d. → 25–35%, 7-day sell-through → >80%, export share ~25% → 35%, "
     "owned community 3.6k → 100k; internal process — pairs/month ~5,000 → 12,000, days per pair ~4 → 2.5, "
     "returns <5%, on-time drops 100%; learning & growth — karigars ~100 → 220, attrition <7%, new silhouettes "
     "4/yr, six senior hires. Control: strategic (are we on the right course; 1–3 yr horizon; founders + board; "
     "e.g. Mumbai or margin first) versus operational (is today’s task done right; daily–monthly; function heads; "
     "stock counts, discount permissions) — the February 2026 fraud, in which a new customer-service hire used "
     "admin access to create 100%-discount codes and shipped ₹2 lakh of sneakers before quitting, was an "
     "operational-control failure whose strategic question is whether a trust culture survives 150 employees. "
     "Prescription: role-based permissioning (done), segregation of duties and four-eyes on discounts, weekly "
     "anomaly reports, surprise stock counts, written incident protocol, an independent audit-minded director. "
     "Contingency with triggers: hero-SKU fatigue (two drops <70% sell-through), karigar supply shock "
     "(utilisation >90% for a quarter), cash crunch (runway <6 months → pause store #3, not marketing), "
     "authenticity attack (sentiment spike → founder-led reply within 24 h).")

h("10. Conclusion and recommendations", 13)
body("Working: a positioning no one else occupies; a 56% gross margin that proves the price is accepted; vertical "
     "integration as quality and duty shield; real growth (20 lakh → 2.9 Cr → 16 Cr). Not working: 11% CM2 "
     "(marketing buys every order), capacity capped by trained hands, ₹85 lakh against a three-city build-out, "
     "governance proven fragile once. Verdict: culture is a real advantage — but only welded to factory, karigars "
     "and store; remove one and it is a marketing line a better-funded rival copies in a season. Recommendations: "
     "(1) treat craft like technology — a karigar academy with certification, retention-linked pay and a "
     "documented pipeline, because capacity is the ceiling and the only lever that does not dilute the hand-made "
     "claim; (2) make exports the margin engine — the diaspora pays $140–220 with no 18% slab and lower CAC, "
     "target 35% exports and a US pop-up before a permanent store; (3) protect the premium with controls — formal "
     "discount authority, an independent audit-minded director, a sell-through-based drop calendar, because "
     "scarcity only works if nobody believes a sale is coming. Answer to the exam question — is “unapologetically "
     "Indian” a strategy or a marketing line? It is a strategy, conditionally: sustainable only as long as "
     "evaluation catches the financial and organisational moment before the market does.")

h("Sources", 13)
for s in ["gullylabs.com and global.gullylabs.com — story, pricing, ranges.",
          "@GullyLabs on X — 3,640 followers; posting verified to 19 Sep 2026.",
          "Financial Express, 28 Jan 2026 — FY24/25 financials and unit economics.",
          "Inc42, 23 Jan 2026 — founding story, capacity, FY26/27 targets.",
          "Indian Retailer — seed round, Series A, flagship store.",
          "Mint, 22 Feb 2026 — the ₹2 lakh internal fraud.",
          "PIB, Sep 2025 — GST rationalisation; Moneycontrol — IFLDP scheme.",
          "Tracxn and company filings via press — competitor funding (Comet $6.6M, CHK $3.8M, RapidBox $9.5M).",
          "Architectural Digest India — flagship interior (photograph)."]:
    p = doc.add_paragraph(s, style="List Bullet")
    p.paragraph_format.space_after = Pt(2)

out = os.path.join(ROOT, "Gully_Labs_Strategy_Report.docx")
doc.save(out)

# verify
d2 = Document(out)
print("script md:", os.path.getsize(os.path.join(ROOT, "speaker_script.md")), "bytes")
print("viewer html:", os.path.getsize(os.path.join(ROOT, "viewer.html")), "bytes")
print("report docx paragraphs:", len(d2.paragraphs))
