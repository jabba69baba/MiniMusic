# MiniMusic × M3 transitions — audit and implementation plan

Source studied: **m3.material.io/styles/motion/transitions** — *Applying transitions* and
*Transition patterns* (fetched 2026-09-18). Everything below is either a quotation of that
page or a line-anchored observation about this repo at `fc2687e`.

---

## 1. What the guide actually asks for

### 1.1 Eight characteristics every transition should have

| # | Characteristic | What the guide demands |
|---|---|---|
| 1 | Follows accessibility settings | With reduced motion on: *"Use subtle fades instead of intense sliding or scaling animations"*, *"Disable decorative effects like parallax or shape morphing"* |
| 2 | Consistent | *"Consistently applying the right type of transition helps make apps feel cohesive and predictable"* — the same class of move should feel the same everywhere |
| 3 | Stable layouts | *"Use skeleton loaders so that UI elements are coherent and stable during a transition. Avoid content shifting positions or instantly popping in"* |
| 4 | No jarring jump cuts | *"Jump cuts should generally be avoided as a default setting since they can be disorienting… offers no clues to help a user orient themselves"* |
| 5 | Coherent spatial model | Where something enters from teaches where it lives (top → notification shade, left → drawer, bottom → sheet/keyboard) |
| 6 | Unified direction | *"Elements are grouped and move along a primary axis instead of moving in independent directions. Only important elements like hero images remain persistent"* |
| 7 | Clean fades | *"Fully fade out content before fading new content in. This avoids the overlap of partially transparent elements resulting in distracting and messy frames"* |
| 8 | Simple style | *"Common transitions should not use overt style effects like bouncy springs"* |

Two explicit **Don'ts** that matter for this app:

- *"Don't slowly fade components on top of other content as they enter or exit… Don't fade a
  bottom sheet as it enters and exits, it creates distracting cross faded frames."*
- (Enter/exit, within screen bounds) *"**Android** components expand and collapse along the x or y
  axis as they enter and exit. **Scale and z-axis motion is avoided since they imply elevation
  change**, which doesn't match M3's reduced elevation model."* (iOS is the one that scales.)

### 1.2 The six patterns, and which one each surface should use

| Pattern | Prescribed for | Explicit cautions |
|---|---|---|
| **Container transform** | *"Hero moments that should be expressive… shallow hierarchies where you expand an element for more detail then collapse it… a seamless connection between elements."* Cards, lists, galleries, sheets, FABs, chips | *"most dramatic pattern… reserved for the right context"*; don't use in deep hierarchies |
| **Forward and backward** | Consecutive levels of hierarchy (inbox → thread) | *"Both Android and iOS should use platform defaults… easy to implement and stays current."* Android's default = *"a fade as screens slide"* |
| **Lateral** | Peers in one set — *"tabs of a content library"*, carousels, galleries | Slide **in unison**, *"does not use a fade"*; *"Fading content as it slides makes the peer relationship and swipe gesture less obvious."* Never for hierarchical or top-level navigation |
| **Top level** | Nav bar / rail / drawer destinations | Quick fade, intentionally **no** relationship between screens. Lateral here *"implies you can swipe between top level destinations which conflicts with other components like carousels or swipe-able list items"* |
| **Enter and exit** | Components appearing over a screen. *Within* bounds: FABs, dialogs, menus, snackbars (expand/collapse along x or y). *Beyond* bounds: app bars, banners, nav bar/rail/drawer, sheets (expand/collapse as they slide off) | Don't use for hierarchical navigation (*"sliding content the full height of the screen is excessive"*). Don't fade sheets |
| **Skeleton loaders** | Temporary loading → loaded UI | Subtle pulse, *"starts at the top left of the screen and moves down to the bottom right"*, then *"content quickly fades in on top"* |

Also worth quoting because it frames this whole exercise:

> *"M3 transitions use the legacy easing and duration system. They'll eventually be updated to use
> the motion physics system."*

MiniMusic has **already** moved its components to the physics system (`MaterialExpressiveTheme`,
`motionScheme = MotionScheme.expressive()`, `Theme.kt:74`). That is precisely why the transitions
now stand out: components overshoot gently on springs, transitions stop dead on beziers.

---

## 2. Inventory — what the app does today, judged against the guide

| Surface | What it does now (file:line) | Guide's pattern | Verdict |
|---|---|---|---|
| Hierarchy push (Library → Settings / Album / Artist) | Hand-rolled shared axis: slide in at 50% width + `scaleIn(0.92)`, **no fade**; exiting screen does a 400 ms *zero-travel* slide to hold its pixels; 400 ms emphasized tween (`NavGraph.kt:263-291`) | Forward and backward | ⚠️ Right family, wrong recipe — no fade, and `popExitTransition` branches on the *origin* route (`NavGraph.kt:294`, `:317`) so returning from Settings moves differently than returning from Album |
| Details dialog | Card `alpha 0→1` **and** `scale 0.92→1.0`, 220 ms; scrim fades on the same value (`DetailsScreen.kt:133-152`, `:191-195`) | Enter and exit / **container transform** | ❌ This is the *iOS* recipe the guide prints as the contrast case (scale = implied elevation change). Also the guide's canonical container-transform candidate |
| Lyrics (portrait: full-height card; landscape: half-width pane at `zIndex 4f`) | Route motion `None`; card owns one 400 ms vertical slide, symmetric, no fade (`NavGraph.kt:467-470`, `LyricsScreen.kt:164-192`) | Enter and exit, beyond screen bounds | ✅/⚠️ One owner is right; full-height slide on a hierarchical-ish surface is what the guide calls excessive, and landscape overlays instead of shrinking |
| Landscape queue bar ↔ sheet | One shared `defaultSpatial()` spring; bar `translationY` + **`alpha = 1 - progress`**, sheet rises (`QueueDrawer.kt:205-211`, `:224-227`) | Enter and exit, beyond bounds | ✅ grouped, unified, single owner — but ⚠️ the bar's fade is the guide's explicit *"Don't fade a bottom sheet"* |
| Portrait queue drawer | Panel height expands from 48 dp to 82% on one `Animatable` (`QueueDrawer.kt:507-520`) | Enter and exit, beyond bounds | ✅ on-pattern (expand/collapse along Y) |
| Mini → full player sheet | `PlayerSheetMotionState` animates one offset; mini player translates down as the player rises, `alpha` pinned at 1 (`PlayerSheetMotion.kt:74`, `NavGraph.kt:621-630`) | Container transform (shared element) | ✅ coherent, spatially honest, one owner — the best-designed motion in the app |
| Library peers: Songs / Artists / Albums | Pill label slides laterally (`LibraryScreen.kt:912-923`) but the **content below hard-cuts** (`LibraryScreen.kt:425-453`) | Lateral | ❌ Guide's own example ("tabs of a content library") and its explicit "no jarring jump cuts" |
| Mini player track change | `AnimatedContent` = `fadeIn(180)+slideIn(360)` **togetherWith** `fadeOut(120)+slideOut(260)` (`MiniPlayer.kt:162-172`) | Enter and exit / lateral | ❌ Overlapping cross-fade ≈ 120 ms of translucent double image — the guide's "messy and distracting frames". Four raw durations not from the token file |
| Player album-art strip | One shared `Animatable` progress, film-strip, no fade, `carouselSpatial()` (`PlayerScreen.kt:646-700`) | Lateral | ✅ textbook |
| Song-details / Lyrics route entries | Both `EnterTransition.None`, surface owns its own motion (`NavGraph.kt:448-451`, `:467-470`) | — | ✅ Keep. One motion owner per transition is the rule this respects |
| Predictive back | Continuous progress → Library layer translate/scale, then pop (`NavGraph.kt:112-134`, `:252-262`) | — | ✅ genuinely good; one raw `tween(180)` snap-back |
| Library first load / empty | Spinner; `LoadingState()` wraps a constant-`true` `AnimatedVisibility` (`LibraryScreen.kt:1289-1295`) → **dead motion**, never plays | Skeleton loaders | ❌ spinner instead of skeleton, plus a no-op animation wrapper |
| Album / Artist list reorder | `animateItem(placementSpec = defaultSpatial(), fadeIn/Out = null)` (`FilteredSongsScreen.kt:63-67`) | — | ✅ good, fade-free placement is correct |

### 2.1 The systemic diagnosis

**Three motion clocks are running at once.**

1. **Physics (M3E)** — `Theme.kt:74`, all `MiniMusicMotion.*` springs. Used by components and by the
   app's best hand-rolled work (carousel, sheets, queue).
2. **Legacy bezier tweens at 400 ms** — `navTransitionDurationMillis`, `navEnterEasing`,
   `navExitEasing`, `dialogEasing` (`Motion.kt:105-138`). Used by every navigation edge.
3. **Ten ad-hoc millisecond values baked into feature code**, bypassing the token file entirely:

   | Value | Where |
   |---|---|
   | 180 / 360 / 120 / 260 | `MiniPlayer.kt:165-168` (track change) |
   | 220 linear | `MiniPlayer.kt:199` (progress ring) |
   | 180 | `NavGraph.kt:130` (predictive-back snap-back) |
   | 130 / 90 | `PlayerScreen.kt:1461-1463` (play/pause morph) |
   | 1200 linear | `LibraryScreen.kt:1325` (indeterminate rotation) |
   | 1400 linear | `WavyMusicSlider.kt:65` |

That is the mechanical explanation of "uneven". A user toggles a Settings switch (M3E spring,
decisive) and then taps back (400 ms bezier slide, no fade, different curve in each direction, and a
different trajectory again when the origin was Settings). The durations differ per edge, the exit
curves contradict the file's own documented rule (see §3.2), and the mini player and the main player
disagree about how long a track change takes.

**Deeper structural point:** `Surface`-level motion is well-architected; *screen*-level motion is
where the guide is unimplemented. Almost every ❌ above is a screen-level or content-swap
transition.

---

## 3. Where to implement the guide, ranked

Ordered by (perceived gain ÷ risk). Each item is independently shippable.

### 3.1 — Lateral transition for the Library's three peers · **highest value, lowest risk**

*Guide:* "Lateral … tabs of a content library … elements are grouped and slide in unison" + "no jarring
jump cuts" + "Fading content as it slides makes the peer relationship less obvious" (so: **no fade**).

*Now:* `LibraryScreen.kt:425-453` is a bare `when (selectedTab)`; the pill's own label already slides
correctly at `:912-923`.

*Change:* wrap that `when` in one `AnimatedContent` keyed on `selectedTab`, with
`slideInHorizontally { +it } togetherWith slideOutHorizontally { -it }` — direction from the tab
index, not the cycle. Use **`MiniMusicMotion.carouselSpatial()`**: critically damped, no overshoot
(a full-width content slide is exactly the "full-bleed frames pay per-frame measure/clip" case that
token's own comment already documents), and it makes the Library's content travel on the same clock
as the player's art strip — one "content strip" motion for the whole app.

*Risk:* the three tabs have different scroll/footer padding and a locate-scroll path
(`jumpToCurrentRequest`). Scope the `AnimatedContent` to the list area only, keep state keyed per
tab, and confirm the locate-button path still scrolls songs after a tab round-trip.

### 3.2 — One recipe for forward/backward, exits accelerating · **high value, low risk**

*Guide:* forward/backward = platform defaults; Android = "a fade as screens slide". Also
"The exiting screen …" must move in one unified direction.

*Now:* `NavGraph.kt:263-334`. Specifically:
- `:284-291` — the exiting screen runs a **zero-travel** `slideOutHorizontally { 0 }` for the full
  400 ms purely to stay visible. A 400 ms animation that moves nothing is a hold, not a transition.
- `:293-295` and `:316-319` — `if (initialState.destination.route == Routes.SETTINGS)` forks the
  pop motion, so *returning from Settings* and *returning from Album* are different moves.
- `:325-333` — the non-Settings `popExitTransition` exits on **`navEnterEasing`** (decelerate)
  while `exitTransition :287` exits on `navExitEasing` (accelerate). `Motion.kt:100` documents the
  rule this violates.

*Change:* one push and one pop recipe for all four hierarchy edges. Add a real fade to the exiting
screen instead of the zero-travel hold, delete both `SETTINGS` forks, and use `navExitEasing` for
every exit. Drop the duration to ~300 ms, or better — follow the guide's own trajectory note
("they'll eventually be updated to use the motion physics system") and move these to
`defaultSpatial()`, so a screen push settles on the same spring as the component that launched it.
That single change is what makes the app feel like one system rather than two.

*Risk:* low — pure transition-spec edit, no layout impact. Predictive back still needs one visual
check after the fork removal.

### 3.3 — Mini player track change: stop cross-fading · **high value, low risk**

*Guide:* "Fully fade out content before fading new content in… Avoid showing cross faded content";
"Unified direction".

*Now:* `MiniPlayer.kt:162-172` — `fadeIn(180)+slideIn(360)` against `fadeOut(120)+slideOut(260)`
means both songs are on screen at partial opacity for ~120 ms, and in/out travel different distances
at different speeds. Meanwhile the main player does this perfectly (`PlayerScreen.kt:646-700`).

*Change:* give the mini player the player's treatment — one `Animatable` progress, no fades, both
title and art carried on the same `carouselSpatial()` clock as the art strip. The mini player and the
full player then agree about a track change, which they currently don't (≈360 ms vs ≈500 ms settle).

*Risk:* low. Watch for the moment the mini player coexists with a collapsing sheet (already handled by
`sheetState.progress` owning translation, not alpha — keep that).

### 3.4 — Collapse the three clocks into `MiniMusicMotion` · **medium value, low risk, improves everything after it**

Kill the ten raw durations in §2.1 by adding two tokens (`contentSwapSpatial` → `carouselSpatial`,
`indeterminateLoop` → the existing linear values) and routing `PlayerScreen.kt:1461-1463`'s
`fadeIn(tween(130)) + scaleIn(selectionEffects())` through **one** spec — it is currently a bezier
fade and a spring scale inside a single transition, which is the same double-clock bug the details
dialog was fixed for at the route level. Also `MiMusicMotion.navTransitionDurationMillis` should not
be reachable from `NavGraph` once 3.2 uses a physics token.

*Risk:* near zero, mechanical. Do this *before* 3.5/3.6 so the new work inherits one clock.

### 3.5 — Details dialog: container transform (the guide's "hero moment") · **highest ceiling, highest effort**

*Guide:* "Use a container transform transition for hero moments rather than a forward and backward
transition"; container transform is *for* "shallow hierarchies where you expand an element for more
detail then collapse it" and "Cards, lists…". The song row → details card is the textbook case.

Two tiers, and the user should pick:

- **Tier 1 (small, guide-compliant):** change the card's enter/exit from scale+alpha to an
  **expand/collapse along Y** (`expandIn(expandFrom = Alignment.Center, clip = false)` ⊆
  `shrinkOut`), keeping the 220 ms and the scrim on the same driver (`DetailsScreen.kt:133-152`,
  `:191-195`). This is the letter of the Android enter/exit rule. Caveat: this surface is one you
  tuned deliberately in earlier rounds; the current scale+fade may simply be the feel you want, in
  which case keep it and treat this as an option, not a bug.
- **Tier 2 (the real thing):** a true container transform from the tapped row's bounds into the
  details card. Compose BOM `2025.01.00` (animation 1.7.x) does provide
  `SharedTransitionLayout` / `Modifier.sharedElement` / `Modifier.animateBounds`
  (`@OptIn(ExperimentalSharedTransitionApi::class)`), so this is reachable. **The blocker is the
  window:** `DetailsScreen` renders into a `Dialog`, which is a separate window/composition, and
  shared elements do not cross windows. Tier 2 therefore requires either (a) a manual morph —
  capture the row's `boundsInWindow` on tap and animate the card's inset/scale between those two
  rects — or (b) promoting details out of the `Dialog` into a real NavHost destination so both
  surfaces share one composition. (b) is cleaner conceptually and re-usable for album/artist art
  later, but it re-opens the back-press/predictive-back design that `NavGraph.kt:108-111`
  deliberately settled. Recommend (a) first: it is contained inside `DetailsScreen` and cannot
  disturb navigation.
- Note: the standing rule "song details must show no loading indicator: every card renders from the
  first frame" is **compatible** with both tiers — a container transform needs no loader.

### 3.6 — Skeleton loaders where the guide wants them

*Guide:* pattern 6 + "Stable layouts… avoid content shifting positions or instantly popping in".

*Now:* the library's first load shows a spinner, and `LoadingState()` wraps it in a constant-`true`
`AnimatedVisibility` whose enter/exit can therefore never run (`LibraryScreen.kt:1289-1295`) — dead
motion, worth deleting on its own merit.

*Change:* a pulsing skeleton for the **library list** and the **album/artist list**, matching the row
geometry it replaces (72 dp rows), fading real content in over it, sweeping top-left → bottom-right
per the guide. **Not for song details** — that is explicitly excluded by a standing requirement.

### 3.7 — Reduced motion: make it a decision, not a default

*Guide:* characteristic #1.

*Now:* nothing in the app references `MotionDurationScale`/animator scale. Compose's own animations
do honour the platform animator duration scale (scale 0 → instant), so fades and slides degrade
acceptably without help. Two gaps remain:

- The guide asks for a *substitution*, not just "off": *"subtle fades instead of intense sliding or
  scaling"* and *"disable decorative effects like parallax or shape morphing"*. The app's intense
  motions — the 50%-width screen push (§3.2), the full-height lyrics card, the library lateral slide
  (§3.1, which the guide itself calls "excessive amount of motion" for a full-width peer slide), and
  the album-art film strip — have no reduced-path.
- Two motions live in the **view system** and so bypass Compose's duration scaling entirely: the
  RecyclerView smooth-scroll glide (`QueueDrawer.kt:937-983`, time-based `LinearSmoothScroller`) and
  the drag-lift `view.animate()` (`QueueDrawer.kt:1279-1291`).

*Change:* read the motion scale once (a `LocalMiniMusicReducedMotion` provided from
`LocalMotionDurationScale.current.scale <= 0f`) and branch the art strip and the screen push to
cross-fade/short-slide, plus short-circuit the queue glide to a direct placement when reduced.

### 3.8 — Smaller inconsistencies (one sweep)

| Item | Anchor | Note |
|---|---|---|
| Predictive-back snap-back | `NavGraph.kt:130` | raw `tween(180)`; should be a token |
| Play/pause morph | `PlayerScreen.kt:1461-1463` | two clocks inside one transition (bezier fade + spring scale) |
| Landscape queue bar fade | `QueueDrawer.kt:224-227` | the guide's explicit *"Don't fade a bottom sheet"*. It is a user-requested handoff (bar yields to sheet) rather than a standalone exit, and the slot is already `clipToBounds`, so a fade-free slide is available if the guide wins over taste here. Flagging the tension; not a defect |
| Landscape lyrics as a coplanar sheet | `NavGraph.kt:473-486` | guide: "coplanar sheets **shrink** the available area for content" — currently it overlays 50% of the player at `zIndex 4f` instead |
| Landscape pane reflow | `PlayerScreen.kt:1242` | `if (isLandscape)` swaps the entire pane composition with no transition at all on rotation/resize |
| Progress ring | `MiniPlayer.kt:197-200` | positional-ish property on a linear tween while everything around it is springy |

---

## 4. What is already right — do not "fix" these

The guide endorses several deliberate decisions already in the tree, and they are the reason some
surfaces feel finished:

1. **One motion owner per transition.** Details and Lyrics set all four route transitions to `None`
   and let the surface own its single animation (`NavGraph.kt:448-451`, `:467-470`). This is exactly
   the "unified direction" rule and it took real debugging to reach — leave it.
2. **The mini → full player handoff** (`PlayerSheetMotion.kt`, `NavGraph.kt:621-630`): grouped, one
   axis, one driver, mini player pinned opaque so it never cross-fades. Guide-clean.
3. **View-system surfaces never move** (queue panel, landscape bar) while the player rises over them
   — a coherent spatial model.
4. **The art strip and the queue drawer** are the app's two best transitions; they are the template
   the other surfaces should be brought up to, which is why §3 items repeatedly point at
   `carouselSpatial()` and the one-owner pattern.
5. **Top-level pattern does not apply here.** There is no nav bar/rail; Library is the root. So the
   guide's "quick fade for top-level destinations" has nothing to bind to, and the Library's internal
   peers should be *lateral*, not top-level-fade (§3.1).

---

## 5. Suggested sequence

| Phase | Items | Why this order |
|---|---|---|
| 1 | §3.1 Library lateral · §3.3 mini player · §3.4 clock collapse · §3.2 nav recipe | Four contained, low-risk edits that together remove most of the perceived unevenness: the jump cut, the cross-fade, the stray durations, and the inconsistent exit curve |
| 2 | §3.5 Tier 1 (expand/collapse) · §3.6 skeletons | Visual-quality step; both are self-contained screens |
| 3 | §3.5 Tier 2 (container transform) · §3.7 reduced motion · §3.8 sweep | Highest ceiling and highest risk; needs a design decision on the details window first |

Verification for every phase is the same as the app's normal loop: build via CI on
`arena/01a0aead-minimusic`, then a screen-recording of the four or five affected gestures
(Library tab cycle, track skip with the mini player visible, Library → Album → back,
Library → Settings → back, details open/close) played at 0.25× to check for double-driven frames.
