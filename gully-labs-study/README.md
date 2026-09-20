# Gully Labs — Brand Study & Strategic Analysis

Course deliverable (Business Policy & Strategic Management). Team: Aisha · Rohan · Meera · Kabir. September 2026.

## Deliverables
| File | What it is |
|---|---|
| `Gully_Labs_Strategy_Deck.pptx` | The 15-slide minimal deck, 16:9, with speaker notes per slide (4 presenters, 11:15). |
| `Gully_Labs_Strategy_Report.docx` | Assignment-style written report (10 sections + sources). |
| `speaker_script.md` | Full spoken script, extracted from the deck notes, with timings and INTERACT cues. |
| `viewer.html` | Interactive browser viewer: slide images + speaker notes, arrow-key navigation. |
| `cue_sheet.html` | Printable one-page presenter cue sheet (timings, interacts, backups, house rules). |
| `index.html` | Landing redirect for the live preview server. |
| `DESIGN.md` | The design system the deck follows (palette, type scale, grid, components). |
| `assets/` | Source photos used on slides 1/3/4/6/15 (press imagery; see provenance below). |
| `deck_min/` | Generator + renderer sources (`gl_min2.py`, `gl_min2b.py`, `build2.py`, `render2.py`, `build_extras.py`, `preview/`). |

## Design system (summary — full rules in DESIGN.md)
Cream `#F4EFE6` / ink `#221B15` / soft `#4A443C` / muted `#8A8177` / hairline `#CFC5B4` / brown `#8C6248`.
Georgia for display, titles, quotes and folios; Calibri for body/kickers. Fixed scale only; hairline rules;
offset photo frames; outlined (unfilled) bars, circles and matrices; footer `GULLY LABS — CULTURE AS CAPITAL · NN / 15`.

## Rebuild
```
/home/user/.venv/bin/python deck_min/build2.py        # regenerate the .pptx from source
/home/user/.venv/bin/python deck_min/render2.py       # render preview PNGs + overflow report (expect 0 issues)
/home/user/.venv/bin/python deck_min/build_extras.py  # regenerate script md, viewer html, report docx
```

## Photo provenance & swapping in the originals
The seven images attached in the original brief never reached the workspace, so slides use press photography:
S1/S15 Inc42 founders collage · S3 AD India flagship interior · S4 r/Delhi AMA selfie · S6 hand-held गली heel.
To swap in the real attachments: drop each file into `assets/`, edit the `photo(s, "…jpg", …)` call for slides
1/3/4/6/15 in `deck_min/gl_min2.py` (s01, s03, s04, s06) and `deck_min/gl_min2b.py` (s15), re-run `build2.py`
then `render2.py`. The crop + offset frame are applied automatically.

## Evidence base (also on slide 15 and in the report)
gullylabs.com · global.gullylabs.com · @GullyLabs on X (3,640 followers, to 19 Sep 2026) · Financial Express
28 Jan 2026 · Inc42 23 Jan 2026 · Indian Retailer · Mint 22 Feb 2026 · PIB Sep 2025 · Tracxn · AD India.
Figures marked *proposed/n.d.* are our inferences, stated as such.
