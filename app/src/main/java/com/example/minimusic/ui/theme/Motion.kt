package com.example.minimusic.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared presentation-only motion tokens for MiniMusic.
 *
 * Spatial properties use restrained springs; color, alpha, and small shape
 * changes use short eased effects tweens. These specs must never own playback,
 * queue, or navigation state.
 */
object MiniMusicMotion {
    const val sheetDurationMillis = 255
    const val fastEffectDurationMillis = 180
    const val defaultEffectDurationMillis = 220
    const val selectionDurationMillis = 250
    const val trackChangeDurationMillis = 480

    fun <T> fastEffects(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = fastEffectDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun <T> defaultEffects(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = defaultEffectDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun <T> selectionEffects(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = selectionDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun <T> trackChangeEffects(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = trackChangeDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun <T> trackChangeExitEffects(): FiniteAnimationSpec<T> =
        tween(
            durationMillis = trackChangeDurationMillis / 2,
            easing = FastOutSlowInEasing
        )

    // Canonical M3E-style motion roles. These are presentation-only and do not
    // own playback, queue, or navigation state.
    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 1400f)

    /** Used by the player/miniplayer sheet so it settles quickly without rubber-band overshoot. */
    fun <T> sheetSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1f, stiffness = 900f)

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 700f)

    fun <T> slowSpatial(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 400f)

    fun <T> fastEffectsSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 1400f)

    fun <T> defaultEffectsSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 700f)

    fun <T> slowEffectsSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.90f, stiffness = 400f)
}
