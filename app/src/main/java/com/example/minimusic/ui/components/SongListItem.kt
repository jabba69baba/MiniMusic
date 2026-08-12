package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minimusic.data.model.Song

/**
 * A single song row, presented as its own rounded-pill container (PixelPlayer
 * style) with spacing between rows rather than a flat continuous list. The
 * currently-playing row gets a tinted background, accent-colored text, and
 * inline previous/play-pause/next controls in place of the plain thumbnail —
 * matching the reference "Next up" / library list design.
 */
@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    isActuallyPlaying: Boolean = false,
    onTogglePlayPause: (() -> Unit)? = null,
    onSkipNext: (() -> Unit)? = null,
    onSkipPrevious: (() -> Unit)? = null
) {
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    }
    val contentColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPlaying) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Inline transport controls on the now-playing row only, matching
            // the reference; everywhere else just shows the row itself (tap
            // anywhere to play that track).
            if (isPlaying && onTogglePlayPause != null) {
                if (onSkipPrevious != null) {
                    IconButton(onClick = onSkipPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = contentColor)
                    }
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isActuallyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isActuallyPlaying) "Pause" else "Play",
                        tint = contentColor
                    )
                }
                if (onSkipNext != null) {
                    IconButton(onClick = onSkipNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = contentColor)
                    }
                }
            }
        }
    }
}
