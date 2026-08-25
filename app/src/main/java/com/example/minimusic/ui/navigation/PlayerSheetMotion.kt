package com.example.minimusic.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Immutable
data class PlayerSheetMotionBounds(
    val expandedOffsetPx: Float,
    val collapsedOffsetPx: Float
) {
    val rangePx: Float get() = (collapsedOffsetPx - expandedOffsetPx).coerceAtLeast(1f)
}

@Stable
class PlayerSheetMotionState internal constructor(
    private val scope: CoroutineScope
) {
    private val mutex = MutatorMutex()
    private val offset = Animatable(0f)
    private var bounds = PlayerSheetMotionBounds(0f, 1f)
    var isDragging by mutableStateOf(false)
        private set

    val offsetPx: Float get() = offset.value
    val progress: Float
        get() = ((bounds.collapsedOffsetPx - offset.value) / bounds.rangePx).coerceIn(0f, 1f)

    fun updateBounds(newBounds: PlayerSheetMotionBounds) {
        bounds = newBounds
        if (offset.value == 0f || offset.value !in newBounds.expandedOffsetPx..newBounds.collapsedOffsetPx) {
            scope.launch { offset.snapTo(newBounds.collapsedOffsetPx) }
        }
    }

    fun dragBy(deltaPx: Float) {
        scope.launch {
            mutex.mutate {
                isDragging = true
                offset.snapTo((offset.value + deltaPx).coerceIn(bounds.expandedOffsetPx, bounds.collapsedOffsetPx))
            }
        }
    }

    fun settle(
        velocityPxPerSecond: Float,
        targetProgressOverride: Float? = null,
        onExpanded: () -> Unit,
        onCollapsed: () -> Unit
    ) {
        scope.launch {
            mutex.mutate {
                isDragging = false
                val fastDirection = when {
                    velocityPxPerSecond < -900f -> 1f
                    velocityPxPerSecond > 900f -> 0f
                    else -> progress
                }
                val targetProgress = targetProgressOverride
                    ?: if (abs(velocityPxPerSecond) > 900f) fastDirection else if (progress >= 0.5f) 1f else 0f
                val target = bounds.collapsedOffsetPx - bounds.rangePx * targetProgress
                offset.animateTo(
                    target,
                    animationSpec = tween(
                        durationMillis = 255,
                        easing = FastOutSlowInEasing
                    )
                )
                if (targetProgress == 1f) onExpanded() else onCollapsed()
            }
        }
    }
}

@Composable
fun rememberPlayerSheetMotionState(scope: CoroutineScope): PlayerSheetMotionState {
    return remember(scope) { PlayerSheetMotionState(scope) }
}
