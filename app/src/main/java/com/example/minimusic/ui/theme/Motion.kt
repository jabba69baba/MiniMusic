package com.example.minimusic.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Shared presentation-only motion tokens for MiniMusic, mirroring the official
 * M3 Expressive physics motion system
 * (m3.material.io/styles/motion — "physics-based system", May 2025).
 *
 * Two rules from the spec, applied everywhere below:
 * - **Spatial** properties (position, offset, size — anything that changes a
 *   component's bounds) use underdamped springs that may overshoot:
 *   `md.sys.motion.spring.{fast,default,slow}.spatial`.
 * - **Effects** properties (color, alpha — anything that must not change
 *   bounds) use critically-damped springs (`dampingRatio = 1.0`) that never
 *   overshoot: `md.sys.motion.spring.{fast,default,slow}.effects`.
 *
 * Speeds follow the spec's speed table: most motion uses **default**, small
 * components (press ripples, bubbles, badges) use **fast**, full-screen
 * transitions use **slow**. The product-level scheme is
 * [MotionScheme.expressive], set in MiniMusicTheme; these helpers carry the
 * same token values so custom motion stays in sync with every M3 component.
 * They must never own playback, queue, or navigation state.
 */
object MiniMusicMotion {
    /** View-system-only lift duration (RecyclerView drag). Compose motion uses springs. */
    const val selectionDurationMillis = 250

    /** Matches the lyrics sheet exit trajectory so back navigation never cuts it off. */
    const val sheetDurationMillis = 255

    /** `md.sys.motion.spring.fast.effects` — small components, exits, badges. */
    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    /** `md.sys.motion.spring.default.effects` — color/alpha that starts and ends on screen. */
    fun <T> defaultEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)

    /** `md.sys.motion.spring.slow.effects` — full-screen color/alpha transitions. */
    fun <T> slowEffects(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 800f)

    /** Selection highlights are small components: fast effects. */
    fun <T> selectionEffects(): FiniteAnimationSpec<T> = fastEffects()

    /** Track-change color/alpha: effects that start and end on screen. */
    fun <T> trackChangeEffects(): FiniteAnimationSpec<T> = defaultEffects()

    /** Track-change exits leave quickly: fast effects. */
    fun <T> trackChangeExitEffects(): FiniteAnimationSpec<T> = fastEffects()

    // Canonical M3E-style motion roles. These are presentation-only and do not
    // own playback, queue, or navigation state.
    /** `md.sys.motion.spring.fast.spatial` — small positional movement. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    /**
     * Bottom sheets and partial-screen movement use the **default** spatial
     * token, so the sheet settles with the same gentle overshoot as every
     * other default-speed surface instead of a dead critically-damped stop.
     */
    fun <T> sheetSpatial(): FiniteAnimationSpec<T> = defaultSpatial()

    /** `md.sys.motion.spring.default.spatial` — most positional movement. */
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    /** `md.sys.motion.spring.slow.spatial` — full-screen positional movement. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 200f)

    /**
     * Carousel programmatic travel: critically-damped, no overshoot. A
     * documented exception to the "spatial may overshoot" rule — full-bleed
     * art frames pay per-frame measure/clip through the whole settle tail, so
     * a bouncy default spatial reads as lag on track switches. Same reasoning
     * as PixelPlayer's carousel, reimplemented on our tokens. Stiffness 300
     * settles the full-width travel in roughly half a second: deliberately
     * slower than default spatial so the switch reads clearly. The seekbar
     * rewind shares this exact spec, so bar and art move as one choreography
     * (damping 1.0 also satisfies the effects no-overshoot rule, which is
     * what lets a positional token drive a progress value).
     */
    fun <T> carouselSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 300f)

    // Screen transitions use emphasized *tweens*, not springs: M3 container
    // motion is easing-based while springs stay reserved for components.
    // Cubic values are the M3 motion spec's emphasized curves, cross-checked
    // against the material-components-android toolkit (emphasized standard +
    // accelerate; decelerate is the spec's canonical mirror).
    /** Emphasized easing for elements entering the screen (decelerate). */
    val navEnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized easing for elements leaving the screen (accelerate). */
    val navExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Screen-transition duration at 1x animator scale. */
    const val navTransitionDurationMillis = 400

    fun <T> fastEffectsSpring(): FiniteAnimationSpec<T> = fastEffects()

    fun <T> defaultEffectsSpring(): FiniteAnimationSpec<T> = defaultEffects()

    fun <T> slowEffectsSpring(): FiniteAnimationSpec<T> = slowEffects()
}
