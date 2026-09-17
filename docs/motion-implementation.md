# MiniMusic × M3 transitions — what was implemented

Follows the audit in `docs/motion-transitions-audit.md`. Source studied:
**m3.material.io/styles/motion/transitions** — *Transition patterns* and
*Applying transitions*. Every change below is tied to a sentence of that guide.

---

## 1. The three clocks are now one

The audit's diagnosis was that components already ran the M3 Expressive physics
system while every screen-level transition ran the legacy bezier/duration system,
with ten raw millisecond values hardcoded in feature code. Those raw values are
gone:

| Was | Where | Now |
|---|---|---|
| `fadeIn(180) + slideIn(360)` against `fadeOut(120) + slideOut(260)` | `MiniPlayer` track change | one `carouselSpatial()` spring, no fades |
| `fadeIn(tween(130))` + spring scale | `PlayerScreen` play/pause | `fastEffects()` for alpha, `fastSpatial()` for size |
| `tween(180)` | predictive-back snap-back | `fastEffects()` |

Two new tokens carry the values that were needed but missing:
`navForwardDurationMillis = 300` and `contentHandoffMillis = 180`. The 400ms
`navTransitionDurationMillis` survives only where it belongs — the full-height
lyrics card's travel.

## 2. Per-pattern work

### Forward and backward — hierarchy pushes and pops (`NavGraph.kt`)

One push recipe and one pop recipe, mirrored, for all four hierarchy edges.

- **Fade added.** The exiting screen used to run a *zero-travel* slide for the
  full 400ms purely to stay visible — a hold dressed as an animation. Android's
  forward/backward default is *"a fade as screens slide"*, and the Library layer
  already sits beneath the graph, so the fade now dissolves onto it.
- **Unified direction.** The entering screen travels a third of the width, the
  exiting one drifts a sixth the other way, on one axis, as a group.
- **Exits accelerate.** The non-Settings `popExitTransition` exited on the
  *enter* (decelerate) curve while the other exit used the exit curve; both exits
  now use `navExitEasing`, which is what the token file always documented.
- **The Settings fork is gone.** Returning from Settings used a different
  trajectory than returning from Album. Both are the same pop now.
- **Reduced motion** collapses all four to fades.

### Lateral — the Library's three peers (`LibraryScreen.kt`)

The pill's label already slid; the content below it hard-cut between Songs,
Artists and Albums. The guide names *"tabs of a content library"* as the lateral
pattern's own example, so the content now slides in unison along one axis with
**no fade** — the guide is explicit that fading a lateral transition makes the
peer relationship less obvious — with the direction taken from the tab index.
The clock is `carouselSpatial()`: critically damped, because a full-width slide
that settles with an overshoot pays for the bounce on every frame.

Reduced motion swaps this for a fade.

### Enter and exit — the details dialog (`DetailsScreen.kt`)

The card used a uniform `scale 0.92 → 1` with alpha. The guide's Android rule is
that components *"expand and collapse along the x or y axis"* and that *"scale and
z-axis motion is avoided since they imply elevation change, which doesn't match
M3's reduced elevation model"* — the uniform scale was the iOS treatment the same
page prints as the contrast case. It now grows vertically (`scaleY 0.94 → 1`)
from its centre, with the scrim still riding the identical 220ms driver and the
short fade the guide explicitly sanctions for a dialog.

### Enter and exit — the mini player's track change (`MiniPlayer.kt`)

`fadeIn(180)` against `fadeOut(120)` left both songs partially transparent and
overlapping for ~120ms on every track change, which is exactly the guide's
"messy and distracting frames". Both rows now travel on one axis on one clock,
the incoming arriving as the outgoing leaves — the same film-strip motion the
full player's artwork and title strips already used. The mini player and the full
player finally agree about how long a track change takes.

### Skeleton loaders (`Skeleton.kt`, `LibraryScreen.kt`)

The library's first load showed a spinner (and wrapped it in a constant-`true`
`AnimatedVisibility` whose animation could never run). Both are gone.

- Three skeletons — songs, artists, albums — drawn in the **real rows' geometry**
  (18dp card radius, 12dp gutters, 48dp artwork, same stacked line heights) so
  nothing moves when content replaces them. This is the guide's *"stable
  layouts"* characteristic: *"use skeleton loaders so that UI elements are
  coherent and stable during a transition. Avoid content shifting positions or
  instantly popping in."*
- **Pulse + sweep on one clock.** A slow pulse swells the placeholder tone while
  a highlight band travels left to right; each row lags the one above it and each
  line lags its artwork, so the motion reads as the guide's *"starts at the top
  left of the screen and moves down to the bottom right"*. All of it runs in the
  draw phase, so it never recomposes the list it stands in for.
- **Handoff.** Content *"quickly fades in on top of the skeleton loader"* — the
  one place the guide sanctions an overlapping fade — at 180ms.
- **A rescan keeps the real rows.** The skeleton stands in only for content that
  has never arrived; `isLoading` with songs already on screen no longer replaces
  them.
- **The details dialog's pending values pulse too.** Each card is complete from
  the first frame; a value still being read shows the placeholder pulsing and
  then resolves in place — the skeleton pattern in miniature, with no spinner and
  no layout change, which keeps the app's standing "no loading indicator in song
  details" rule intact rather than overriding it.

### Reduced motion (`ReducedMotion.kt`)

The guide's first characteristic: *"follows accessibility settings… use subtle
fades instead of intense sliding or scaling animations."*

Compose honours the platform animator duration scale for its own animations, but
two things escape it: transitions that should be *reduced* rather than removed,
and motion the app drives itself in the view system. The setting is now read once
(from `Settings.Global.ANIMATOR_DURATION_SCALE`, with a `ContentObserver` so a
change from quick settings takes effect live) and published as
`LocalMiniMusicReducedMotion`. Branching on it:

| Motion | Reduced path |
|---|---|
| Hierarchy push/pop | fade only |
| Library lateral tabs | fade only |
| Album-art film strip | snap to the new cover |
| Lyrics card (full-height slide — the most intense motion in the app) | an 8% rise **plus** a fade on the same driver |
| Queue drawer's smooth-scroll glide | instant placement at the same anchor |

## 3. Deliberately unchanged

- **Details and Lyrics keep their `None` route transitions.** One motion owner
  per transition is the rule the guide's "unified direction" characteristic is
  really about, and both surfaces own a single animation. Their shared-element
  and container-transform upgrade is still open (see §4).
- **The predictive-back transform.** Continuous, one driver, and the Library
  layer stays visible under it.
- **The mini → full player handoff.** Grouped, one axis, no cross-fade.
- **The landscape queue bar's fade.** The guide warns against fading a bottom
  sheet, but this is a hand-off between a bar and the sheet replacing it in the
  same slot, not a sheet entering or leaving. Left as the user asked for it.
- **The details card's other geometry, the player's carousel, and every
  typography decision from `docs/typography-audit.md`.**

## 4. Still open, and why

| Item | Blocker |
|---|---|
| **Container transform** for the song row → details card (the guide's own "hero moment": *"shallow hierarchies where you expand an element for more detail then collapse it"*) | The dialog is a **separate window**, and shared elements cannot cross windows. Needs either a manual bounds morph from the tapped row or promoting details to a real NavHost route — the latter re-opens the predictive-back design that `NavGraph` settled deliberately. |
| **Swipe between Library tabs** | The lateral transition now *promises* the gesture the guide says it hints at. A `HorizontalPager` would need to coexist with each tab's own scroll and the alphabet scrollbars' drag handling. |
| **Coplanar lyrics sheet in landscape** | The guide's coplanar rule says a sheet at the same elevation *shrinks* the content area rather than covering it; the pane currently overlays the right half of the player. |

## 5. Verification

Build is CI-only here; the build is the first gate. Then, worth watching:

1. **Library tabs** — cycle Songs → Artists → Albums and back. Content should
   slide with the pill's label, in the direction you moved, with no fade.
2. **Track change with the mini player visible** — no double image, art and text
   arriving together.
3. **Library → Album → back**, and **Library → Settings → back** — both pops
   should now be the same motion.
4. **First launch (or clear data)** — skeleton rows in the shape of the real
   list, sweeping down-right, then content fading in over them.
5. **Song details** — the card opens by growing vertically; pending values pulse
   and then resolve.
6. **Developer options → Animator duration scale → off** — pushes, tab switches,
   the lyrics card and the queue's Locate should all fade or jump instead of
   travelling.
