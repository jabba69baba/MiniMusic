package com.example.minimusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Skeleton loaders for the M3 "skeleton loaders" transition pattern.
 *
 * The pattern exists to *"hint at where content will appear once it's loaded"*
 * so the layout stays stable across the transition, and the guide is specific
 * about the motion: a *"subtle pulsing animation to indicate indeterminate
 * progress"* which *"starts at the top left of the screen and moves down to the
 * bottom right"*, after which *"content quickly fades in once it's loaded"*.
 *
 * Two things are animated on **one** clock, so the placeholders never beat
 * against each other:
 * - a slow **pulse** — the placeholder tone itself swells and settles — and
 * - a **highlight sweep** that travels left to right within each row.
 *
 * Rows lag the row above them ([SkeletonRowLag]), and each row's text lines lag
 * their artwork, which is what makes the sweep read as travelling down and to
 * the right across the whole screen rather than as every row flashing in
 * unison.
 *
 * The placeholders deliberately reuse the real rows' geometry — the same 18dp
 * card radius, 12dp gutters, 48dp artwork box, and stacked line heights — so
 * nothing shifts position when the real content replaces them.
 */
private const val SkeletonSweepMillis = 1600

/** How far each row trails the one before it, as a fraction of the sweep. */
private const val SkeletonRowLag = 0.07f

/** Extra lag for a row's supporting line, so the sweep moves left-to-right too. */
private const val SkeletonLineLag = 0.05f

/**
 * The one shared clock every skeleton in a screen reads from. Obtain it once per
 * screen and pass it down; reading it inside a draw lambda means the sweep runs
 * in the draw phase and never recomposes the list it is standing in for.
 */
@Composable
fun rememberSkeletonPulse(): State<Float> {
    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SkeletonSweepMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonSweep"
    )
}

/**
 * One placeholder block. [lag] shifts this block's phase within the shared
 * sweep so blocks cascade instead of blinking together.
 */
@Composable
fun SkeletonBox(
    phase: State<Float>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    lag: Float = 0f
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                if (size.width <= 0f || size.height <= 0f) return@drawBehind
                val sweep = (phase.value + lag) % 1f
                // Pulse: one full swell per sweep, so the placeholder tone
                // breathes underneath the travelling highlight.
                val pulse = (sin(sweep * 2.0 * PI).toFloat() + 1f) / 2f
                val resting = lerp(base, highlight, 0.35f + 0.4f * pulse)
                val band = size.width * 0.6f
                val travel = size.width + band
                val startX = -band + travel * sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(resting, lerp(resting, highlight, 0.85f), resting),
                        start = Offset(startX, 0f),
                        end = Offset(startX + band, 0f)
                    )
                )
            }
    )
}

/** Placeholder rows shaped like [SongListItem]: pill card, 48dp art, two lines. */
@Composable
fun SongListSkeleton(modifier: Modifier = Modifier, rows: Int = 9) {
    val phase = rememberSkeletonPulse()
    Column(modifier = modifier.fillMaxSize().clipToBounds()) {
        repeat(rows) { index ->
            val lag = index * SkeletonRowLag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonBox(
                            phase = phase,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            lag = lag
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SkeletonBox(
                                phase = phase,
                                modifier = Modifier.fillMaxWidth(0.62f).height(14.dp),
                                shape = RoundedCornerShape(7.dp),
                                lag = lag + SkeletonLineLag
                            )
                            SkeletonBox(
                                phase = phase,
                                modifier = Modifier.fillMaxWidth(0.4f).height(12.dp),
                                shape = RoundedCornerShape(6.dp),
                                lag = lag + SkeletonLineLag * 2f
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Placeholder rows shaped like [ArtistListItem]: circle monogram and two lines. */
@Composable
fun ArtistListSkeleton(modifier: Modifier = Modifier, rows: Int = 9) {
    val phase = rememberSkeletonPulse()
    Column(modifier = modifier.fillMaxSize().clipToBounds()) {
        repeat(rows) { index ->
            val lag = index * SkeletonRowLag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SkeletonBox(
                            phase = phase,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            lag = lag
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SkeletonBox(
                                phase = phase,
                                modifier = Modifier.fillMaxWidth(0.5f).height(15.dp),
                                shape = RoundedCornerShape(7.dp),
                                lag = lag + SkeletonLineLag
                            )
                            SkeletonBox(
                                phase = phase,
                                modifier = Modifier.fillMaxWidth(0.34f).height(12.dp),
                                shape = RoundedCornerShape(6.dp),
                                lag = lag + SkeletonLineLag * 2f
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Placeholder cells shaped like [AlbumGridItem]: two columns of square art. */
@Composable
fun AlbumGridSkeleton(modifier: Modifier = Modifier, rows: Int = 4) {
    val phase = rememberSkeletonPulse()
    Column(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(start = 12.dp, top = 12.dp, end = 12.dp)
    ) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(2) { column ->
                    val lag = (row * 2 + column) * SkeletonRowLag
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBox(
                            phase = phase,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            shape = RoundedCornerShape(20.dp),
                            lag = lag
                        )
                        SkeletonBox(
                            phase = phase,
                            modifier = Modifier.fillMaxWidth(0.7f).height(14.dp),
                            shape = RoundedCornerShape(7.dp),
                            lag = lag + SkeletonLineLag
                        )
                        SkeletonBox(
                            phase = phase,
                            modifier = Modifier.fillMaxWidth(0.45f).height(12.dp),
                            shape = RoundedCornerShape(6.dp),
                            lag = lag + SkeletonLineLag * 2f
                        )
                    }
                }
            }
        }
    }
}
