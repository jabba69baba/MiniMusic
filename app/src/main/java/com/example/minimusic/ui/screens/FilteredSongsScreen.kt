package com.example.minimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.AlbumArtImage
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.theme.MiniMusicMotion

/**
 * Gramophone-style detail page for an album or artist: a hero header with the
 * large album art (albums only — artists share no single cover), the title,
 * a summary line, and Play / Shuffle actions, followed by the song list.
 */
@Composable
fun FilteredSongsScreen(
    title: String,
    songs: List<Song>,
    currentSongId: Long?,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onOpenDetails: (Song) -> Unit = {},
    onPlayAll: () -> Unit = {},
    onShuffleAll: () -> Unit = {},
    // Albums pass their cover so the hero can show it; artists stay abstract.
    headerArtUri: android.net.Uri? = null,
    headerSubtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Opaque background: without it the screen draws over the Library
            // base layer and reads as a translucent overlay.
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // Hero header
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (headerArtUri != null) {
                        AlbumArtImage(
                            model = headerArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            iconSize = 64.dp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    val summary = headerSubtitle
                        ?: buildString {
                            append(songs.size)
                            append(if (songs.size == 1) " song" else " songs")
                            if (songs.isNotEmpty()) {
                                val totalMin = songs.sumOf { it.durationMs } / 60000
                                append(" · ")
                                if (totalMin >= 60) {
                                    append(totalMin / 60)
                                    append(" hr ")
                                    append(totalMin % 60)
                                    append(" min")
                                } else {
                                    append(totalMin)
                                    append(" min")
                                }
                            }
                        }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    // Songs + total duration indicator (h:mm or m:ss scale)
                    val totalMs = songs.sumOf { it.durationMs }
                    val totalMin = totalMs / 60000
                    val totalSec = (totalMs / 1000) % 60
                    val durationText = if (totalMin >= 60) {
                        "%d:%02d:%02d".format(totalMin / 60, totalMin % 60, totalSec)
                    } else {
                        "%d:%02d".format(totalMin, totalSec)
                    }
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "song" else "songs"} · $durationText",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlayAll,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        Button(
                            onClick = onShuffleAll,
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(song) },
                    onOpenDetails = onOpenDetails,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = MiniMusicMotion.defaultSpatial(),
                        fadeOutSpec = null
                    )
                )
            }
        }
    }
}
