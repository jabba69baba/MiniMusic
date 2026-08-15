package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.theme.PillShape

/** Corner shape for the mini player bar — rounded on top to match the app's
 *  Material Expressive shape scale, but square on the bottom two corners so
 *  it sits flush against the bottom of the screen instead of floating with
 *  a gap on either side. */
private val MiniPlayerShape = RoundedCornerShape(
    topStart = 20.dp,
    topEnd = 20.dp,
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
 * The bar's own background doubles as the progress indicator: the played
 * portion (left) is tinted with the primary color, the remaining portion
 * (right) is the plain surface color, and the split moves left-to-right as
 * the song plays — rather than a separate progress line drawn on top.
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
    modifier: Modifier = Modifier
) {
    val progress by remember(positionMs, durationMs) {
        derivedStateOf {
            if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MiniPlayerShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = song != null, onClick = onClick)
    ) {
        // Progress fill: a plain colored box sized to the played fraction,
        // behind the row's content — this IS the progress indicator, not a
        // separate bar drawn on top of it. Height comes from matchParentSize
        // (ties to the Box's own resolved height, driven by the Row below,
        // instead of fillMaxHeight() expanding to fill the whole screen);
        // width is driven separately by fillMaxWidth(progress) on an inner
        // Box, since chaining both size modifiers on one Box doesn't work —
        // matchParentSize's parent-data sizing overrides fillMaxWidth's
        // fraction rather than combining with it.
        if (song != null) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song?.artist ?: "Tap a song to listen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/pause: filled squircle background, kept as-is; disabled
            // (muted, non-interactive) in the placeholder state.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(PillShape)
                    .background(
                        if (song != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = song != null, onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (song != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
            // Skip next: plain icon, no background, right-aligned next to play/pause.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(enabled = song != null, onClick = onSkipNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Skip next",
                    tint = if (song != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }
        }
    }
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
