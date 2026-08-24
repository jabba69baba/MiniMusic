package com.example.minimusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.theme.rememberArtColorRoles

/** Corner shape for the mini player bar — rounded on top to match the app's
 *  Material Expressive shape scale, but square on the bottom two corners so
 *  it sits flush against the bottom of the screen instead of floating with
 *  a gap on either side. */
private val MiniPlayerShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

/**
 * Permanent mini player bar — always present at the bottom regardless of
 * whether a song is currently loaded. When [song] is null (nothing has
 * ever been played this install), a muted placeholder state is shown
 * instead of hiding the bar, so its height never changes and the layout
 * around it stays stable.
 *
 * Playback progress is shown as a circular Material-style ring around the
 * play/pause control, leaving the miniplayer surface visually stable while
 * the arc advances clockwise.
 */
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    onSwipeToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by remember(positionMs, durationMs) {
        derivedStateOf {
            if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }
    val artColors = rememberArtColorRoles(song?.albumArtUri)
    val miniPlayerColor = artColors.surfaceVariant
    val controlTint = if (song != null) artColors.onSurface else artColors.onSurfaceVariant
    var verticalDragDistance = 0f
    val density = LocalDensity.current
    val minSwipeDistancePx = with(density) { 48.dp.toPx() }
    val progressRingColor = readableProgressColor(
        accent = artColors.primary,
        background = miniPlayerColor,
        primary = artColors.primary,
        onSurface = controlTint
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MiniPlayerShape)
            .background(miniPlayerColor)
            .pointerInput(song?.id) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        verticalDragDistance += dragAmount
                    },
                    onDragEnd = {
                        if (verticalDragDistance < -minSwipeDistancePx) onSwipeToPlayer()
                        verticalDragDistance = 0f
                    },
                    onDragCancel = { verticalDragDistance = 0f }
                )
            }
            .clickable(enabled = song != null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniPlayerArt(artUri = song?.albumArtUri)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "What's the vibe?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1_000,
                        repeatDelayMillis = 1_400,
                        velocity = 24.dp
                    )
                )
                Text(
                    text = song?.artist ?: "Tap a song to listen",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/pause control with a circular progress ring. The ring uses
            // the same 44dp footprint as the next control so the mini-player
            // remains optically symmetrical.
            key(song?.id ?: -1L) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 220, easing = LinearEasing),
                    label = "miniPlayerCircularProgress"
                )
                CircularProgressPlayButton(
                    progress = animatedProgress,
                    enabled = song != null,
                    isPlaying = isPlaying,
                    progressColor = progressRingColor,
                    controlTint = controlTint,
                    onClick = onTogglePlayPause
                )
            }
            // Skip next: plain icon, no background, right-aligned next to play/pause.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = song != null,
                        onClick = onSkipNext
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Skip next",
                    tint = controlTint,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }
        }
    }
}

@Composable
private fun CircularProgressPlayButton(
    progress: Float,
    enabled: Boolean,
    isPlaying: Boolean,
    progressColor: Color,
    controlTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = if (enabled) controlTint.copy(alpha = 0.24f)
    else MaterialTheme.colorScheme.outlineVariant
    val resolvedProgressColor = if (enabled) progressColor else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val inset = strokeWidth / 2f
            val diameter = size.minDimension - strokeWidth
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = resolvedProgressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = controlTint,
            modifier = Modifier.fillMaxSize(0.5f)
        )
    }
}

private fun readableProgressColor(
    accent: Color,
    background: Color,
    primary: Color,
    onSurface: Color
): Color {
    val minimumContrast = 2.4f
    return when {
        contrastRatio(accent, background) >= minimumContrast -> accent
        contrastRatio(primary, background) >= minimumContrast -> primary
        else -> onSurface
    }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance()
    val backgroundLuminance = background.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/** Square album art thumbnail (not circular). Shows a muted music-note
 *  placeholder tile when there's no art (or no song at all). */
@Composable
private fun MiniPlayerArt(
    artUri: android.net.Uri?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (artUri != null) {
            AsyncImage(
                model = artUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            )
        }
    }
}
