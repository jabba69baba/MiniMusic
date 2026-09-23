package com.example.minimusic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.viewmodel.PlaylistViewModel

/**
 * A user playlist: title, songs + duration summary, Play/Shuffle, and a song
 * list with per-row remove. Same hero + list grammar as album/artist pages.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistViewModel: PlaylistViewModel,
    allSongs: List<Song>,
    currentSongId: Long?,
    onBack: () -> Unit,
    onPlaySong: (List<Song>, Song) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onDeletePlaylist: () -> Unit,
    onOpenDetails: (Song) -> Unit
) {
    val state by playlistViewModel.observePlaylistWithSongs(playlistId) { allSongs }
        .collectAsState(initial = null)

    val playlist = state?.playlist
    val songs = state?.songs ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = playlist?.name ?: "Playlist",
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
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val totalMin = songs.sumOf { it.durationMs } / 60000
                    Text(
                        text = "${songs.size} ${if (songs.size == 1) "song" else "songs"} · ${totalMin} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (songs.isNotEmpty()) onPlaySong(songs, songs.first()) },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        Button(
                            onClick = { onShuffleAll(songs) },
                            enabled = songs.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDeletePlaylist) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.height(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete playlist", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(songs, song) },
                    onOpenDetails = onOpenDetails,
                    trailingContent = {
                        IconButton(onClick = { onRemoveSong(song.id) }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove from playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
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
