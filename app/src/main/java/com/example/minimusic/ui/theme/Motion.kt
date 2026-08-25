package com.example.minimusic.ui.theme

import androidx.compose.animation.core.AnimationSpec
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

    fun fastEffects(): AnimationSpec<Float> =
        tween(
            durationMillis = fastEffectDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun defaultEffects(): AnimationSpec<Float> =
        tween(
            durationMillis = defaultEffectDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun selectionEffects(): AnimationSpec<Float> =
        tween(
            durationMillis = selectionDurationMillis,
            easing = FastOutSlowInEasing
        )

    fun fastSpatial(): AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    fun defaultSpatial(): AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        )
}
