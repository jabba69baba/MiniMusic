# MiniMusic × M3 typography — audit and implementation

Source studied: **m3.material.io/styles/typography/applying-type** (fetched 2026-09-18, all
three chunks). The companion *Type scale & tokens* page renders client-side and returns no
values to a fetch, so where a number was needed it was taken from either the ratios this page
states explicitly or Material3's own `Typography()` defaults — which *are* the M3 scale.

---

## 1. What the guide asks for

### 1.1 Five roles, three sizes

> *"The Material 3 type scale organizes styles into five roles that are named to describe their
> purposes: display, headline, title, label, body… These roles and sizes create clear page
> hierarchy and work across many devices."*

| Role | The guide's own words | The guide's own examples |
|---|---|---|
| **Display** | *"largest text on the screen… reserved for short, important text or numerals"* | cards with a big number |
| **Headline** | *"short, high-emphasis text on smaller screens… marking primary passages of text or important regions of content"* | **"Dialog using a headline style"** |
| **Title** | *"medium-emphasis text that remains relatively short… to divide secondary passages"* | **app bar**, category header |
| **Body** | *"longer passages of text"*, readable, never decorative | article text, setup flow |
| **Label** | *"smaller, utilitarian styles… the text inside components"*, small captions | **buttons**, **"A music player using label style for the timecode"**, navigation-bar text |

Two of those examples land directly on this app: a **dialog title is a headline**, and a
**music player's timecode is a label**.

### 1.2 Typesetting: pick the model per platform

> *"Use this method for web products, and iOS products"* — padding and bounding boxes.
> *"**Use this method for Android products** or platform-agnostic specs"* — **the baseline**.

The baseline model defines line height as *"distance from the text baseline of one line to the
text baseline of the next"*, and centering as *"specify center alignment as a reference instead
of measuring the distance to the text baseline"*. In Compose that is `LineHeightStyle`, not
padding nudges.

### 1.3 Ensuring readability

> *"For larger type legibility using styles like title, headline, and display, we recommend a
> line height ratio of **1.2** times the type size. For smaller body copy using styles like body
> and label, we recommend a line height ratio around **1.5** times the type size. If your line
> height is too tight, you'll undermine the flow of the text."*

> *"Use **tabular figures** (also known as monospaced numbers) rather than proportional digits in
> tables or places where values may change often, **such as clocks**… Use tabular numbers to
> prevent layout shifting when values change, such as in a clock UI."*

This one is illustrated twice, with a clock and with **a music player's timecode**.

### 1.4 Color & contrast

> *"Material aims for two main text contrast levels: **3:1 for large text, 4.5:1 for small
> text**."*
> *"The default color for typography is **on surface**, although **on surface variant** is a
> strong alternative."*

### 1.5 Icons

> *"Properly aligning typography with Material Symbols can improve cohesion and unity."*

---

## 2. Audit — what was already right, what wasn't

### 2.1 Already right (and left alone)

| Item | Anchor | Note |
|---|---|---|
| A real scale exists, in the app's own family | `Type.kt` | Google Sans Flex bundled in `res/font`; all 15 roles defined rather than dropped on Material defaults |
| Timecodes already use the **label** role | `PlayerScreen.kt:1104`, `:1136` | Matches the guide's music-player example exactly |
| Dialog details are already label/title/body | `DetailsScreen.kt` `DetailCard` | `titleSmall` label over `bodyMedium` `onSurfaceVariant` value — correct roles and correct color role |
| Secondary text already uses `onSurfaceVariant` in lists | `SongListItem.kt`, `DetailsScreen.kt:353` | e.g. the details artist line |
| List hierarchy is role-based | `SongListItem` | `titleMedium` title over `bodyMedium` supporting line |
| Lyrics' editorial size | `LyricsScreen.kt:328-330` | 22sp/28sp = 1.27 ratio, inside the guide's 1.2 guidance for large text, with tight tracking — a deliberate editorial treatment |

### 2.2 Wrong, fixed here

| # | Defect | Anchor | Guide rule violated |
|---|---|---|---|
| 1 | **No tabular figures anywhere.** The playhead, the duration, and the sleep-timer countdown all render proportional digits, so every tick changes glyph widths. The countdown re-lays-out once a second. | `PlayerScreen.kt:437`, `:1104`, `:1136` | *"tabular figures… places where values may change often, such as clocks"* |
| 2 | **The landscape metadata strip clips its own artist line.** The strip's box is 48dp in landscape with no top padding, but `headlineSmall` (30dp) + `bodyLarge` (22dp) needs 52dp — the supporting line was being cut off by `clipToBounds`. | `PlayerScreen.kt` `VerticalMetadataStrip` | Roles must fit their context; line height *"directly connected to type size"* |
| 3 | **`bodyLarge` and `labelLarge` were the two roles tightened below the ratio.** bodyLarge 16/22 = 1.375, labelLarge 14/18 = 1.29, against the guide's *"around 1.5"* for body and label — and the two roles that carry the most running text in the app. | `Type.kt` | §1.3 ratios |
| 4 | **No tracking at all.** Every role sat at zero letter spacing, so small text ran tight and large text ran loose. | `Type.kt` | The M3 scale's per-role optical tracking |
| 5 | **No line-height model.** Nothing declared how line height is distributed, leaving the Android baseline model to the platform default. | `Type.kt` | §1.2 *"Use this method for Android"* |
| 6 | **The library pill rendered in a different typeface.** It overrode `fontFamily` to `FontFamily.SansSerif` at a one-off 13sp, so Songs/Artists/Albums alone were not Google Sans Flex. | `LibraryScreen.kt:933`, `:962` | Role/scale consistency |
| 7 | **Emphasis faked by bolding the body scale.** The artist line in song rows and the mini player was `bodyMedium` + `Bold`, making the supporting line visually heavier than the title above it — an inverted hierarchy. | `SongListItem.kt`, `MiniPlayer.kt` | Roles carry hierarchy |
| 8 | **A control's value was a second `bodyLarge` in the same `onSurface`**, separated from its own title by weight alone, and the two texts in the row were otherwise identical. | `SettingsScreen.kt:459-460` | Role separation; the guide puts control values in the **label** role |
| 9 | **Contrast below the floor.** Inactive lyric lines were `onBackground` at **42% alpha** (the guide: 4.5:1 for small text). The player's artist line was `onPrimaryContainer` at 70% — a *container* role painted over the `background` surface, a pairing the palette doesn't guarantee. The mini player's artist line was also at 70%. | `LyricsScreen.kt:153`, `PlayerScreen.kt:809`, `MiniPlayer.kt:187` | §1.4 |

---

## 3. What was implemented

**`ui/theme/Type.kt`** — rebuilt around one `role(weight, size, lineHeight, tracking)` helper:

- `lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.None)` on every role — the Android
  baseline model: half-leading split evenly above and below, so a line sits on its baseline
  inside a box of its own line height.
- `bodyLarge` 16/**24** and `labelLarge` 14/**20**, bringing body and label to the guide's ~1.5
  ratio. Sizes are unchanged everywhere else — the app's scale is its own, and only the two
  roles that broke the stated ratio were corrected.
- Per-role tracking added: displayLarge −0.25 · titleMedium +0.15 · titleSmall +0.1 ·
  bodyLarge +0.5 · bodyMedium +0.25 · bodySmall +0.4 · labelLarge +0.1 · labelMedium +0.5 ·
  labelSmall +0.5.
- New `object MiniMusicType`:
  - `tabular(style)` → `fontFeatureSettings = "tnum"`. A font feature, not a family: digits keep
    Google Sans Flex's shapes and only their advance widths are equalised, and a font without
    the feature ignores it.
  - `compactLabel` → 13/19 in the app's own family, for the fixed-width library pill.

**Call sites**

| Surface | Change |
|---|---|
| Position timecode, duration timecode, sleep-timer countdown | `MiniMusicType.tabular(labelMedium)` |
| Details card values (duration, quality, size, year) | `MiniMusicType.tabular(bodyMedium)` — the card is a column of values read against each other |
| Player metadata strip | Roles chosen per box: portrait `headlineSmall` + `bodyLarge` (54dp + 16dp padding = 70dp of 72dp); landscape `headlineSmall` + `bodySmall` (46dp of 48dp). The artist line's color moves from `onPrimaryContainer` @70% to `onSurfaceVariant` |
| Library pill (both slots) | `MiniMusicType.compactLabel` — app family restored, active slot keeps its weight step over the filled highlight |
| Song rows, mini player, sleep-timer switch row | Bold removed from the supporting/body line |
| Settings slider rows | Value becomes `labelLarge` in `onSurfaceVariant` (38% `onSurface` when disabled), title stays `bodyLarge` |
| Lyrics inactive lines | `onBackground` @42% → `onSurfaceVariant` |
| Mini player artist line | 70% → 80% (`onPrimaryContainer` is the correct family there — it sits on `primaryContainer`) |

## 4. Deliberately not changed

- **The scale's sizes.** `headlineSmall` 24, `titleLarge` 20, etc. are the app's own choices;
  resizing them would move text the layouts were tuned around, and the guide's ratio guidance
  constrains *line height relative to size*, not which size to pick.
- **Display and headline leading.** Those roles already carry M3's published line heights
  (57/64, 45/52, 32/40, 28/36, 24/30), which sit slightly under the 1.2 ratio — that is the
  scale's own intent, not a defect.
- **Lyrics' 22sp/28sp treatment.** Self-consistent at a 1.27 ratio and deliberately editorial.
- **The sort menu's bold selected row** (`LibraryScreen.kt:1079`). Menu selection emphasis, not
  body text.
- **`includeFontPadding`.** Deprecated in this Compose version with the default migrating to
  `false`; the baseline model is expressed through `LineHeightStyle` instead.
- **`controlTint` at 42%** (`MiniPlayer.kt:126`). It tints an *icon* in the empty state, not
  text, so the 4.5:1 text floor doesn't apply.

## 5. Verification

Compile/build is CI-only here (no local toolchain), so the build is the first gate. Anything
worth eyeballing after install, in order of how visible it is:

1. **Player**: watch the elapsed timecode for a few seconds — the digits should no longer shift
   their own spacing, and the duration on the right should stay put.
2. **Sleep timer**: start one and watch the chip — the countdown must not resize the chip.
3. **Landscape player**: the artist line under the song title should be fully visible now (it
   was clipped by 4dp) and slightly smaller than in portrait.
4. **Song list / mini player**: artist lines are no longer bold; hierarchy now comes from size
   and color.
5. **Library pill**: Songs/Artists/Albums should now render in the same typeface as the rest of
   the app.
6. **Settings**: slider values (`6 seconds`) sit in the label role and no longer look like a
   second title.
