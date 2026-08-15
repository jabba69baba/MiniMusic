package com.example.minimusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/**
 * Solid, full-width mini player bar — same opaque background as the app's
 * dark surface, flush edges, square corners, no side margins. Sits directly
 * above [FloatingTabBar] with no gap so the two read as one continuous
 * footer, matching the reference layout exactly.
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MiniPlayerArt(
            artUri = song.albumArtUri,
            positionMs = positionMs,
            durationMs = durationMs
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play/pause: filled squircle background, kept as-is.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onTogglePlayPause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
        // Skip next: plain icon, no background, right-aligned next to play/pause.
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onSkipNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip next",
                modifier = Modifier.fillMaxSize(0.7f)
            )
        }
    }
}

/**
 * Square album art thumbnail (not circular) with a thin progress line
 * beneath it showing time remaining — fills left to right as the song
 * plays and completes exactly when it ends.
 */
@Composable
private fun MiniPlayerArt(
    artUri: android.net.Uri?,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val progress by remember(positionMs, durationMs) {
        derivedStateOf {
            if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            AsyncImage(
                model = artUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Canvas(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = 44.dp, height = 2.dp)
        ) {
            drawLine(
                color = trackColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                strokeWidth = size.height
            )
            drawLine(
                color = progressColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                end = androidx.compose.ui.geometry.Offset(size.width * progress, size.height / 2f),
                strokeWidth = size.height
            )
        }
    }
}
