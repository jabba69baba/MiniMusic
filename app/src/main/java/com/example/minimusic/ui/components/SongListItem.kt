package com.example.minimusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Song

/**
 * A single song row, presented as its own rounded-pill container (PixelPlayer
 * style) with spacing between rows rather than a flat continuous list. The
 * currently-playing row just gets a tinted background and accent text — no
 * inline transport controls, matching the reference library list design.
 * A trailing three-dot button opens [SongContextMenu] with play/queue/delete/
 * share/info actions.
 */
@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onShufflePlayFrom: (Song) -> Unit = {},
    onDelete: (Song) -> Unit = {},
    onOpenDetails: (Song) -> Unit = {}
) {
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    }
    val contentColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlbumArtImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                iconSize = 26.dp,
                crossfadeMillis = 0
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlaying) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box {
                // Subtle round background behind the overflow icon on normal
                // rows; on the currently-playing row it switches to the same
                // bright accent (colorScheme.primary) used by the scrollbar
                // thumb and the mini player's play button, so it's actually
                // visible against the row's own tinted highlight instead of
                // nearly disappearing into it.
                val menuButtonColor = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                }
                val menuIconTint = if (isPlaying) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    onClick = { menuExpanded = true },
                    shape = CircleShape,
                    color = menuButtonColor
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = menuIconTint,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                if (menuExpanded) {
                    SongContextMenu(
                        song = song,
                        expanded = true,
                        onDismiss = { menuExpanded = false },
                        onPlayNow = { onClick() },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onShufflePlayFrom = onShufflePlayFrom,
                        onDelete = onDelete,
                        onOpenDetails = onOpenDetails
                    )
                }
            }
        }
    }
}
