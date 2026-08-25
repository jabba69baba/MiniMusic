package com.example.minimusic.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
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

    fun <T> fastSpatial(): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
}
