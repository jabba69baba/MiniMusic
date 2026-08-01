package com.example.minimusic.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.components.AlbumGridItem
import com.example.minimusic.ui.components.AlphabetScrollbar
import com.example.minimusic.ui.components.ArtistListItem
import com.example.minimusic.ui.components.MiniPlayer
import com.example.minimusic.ui.components.PillNavRow
import com.example.minimusic.ui.viewmodel.LibraryUiState
import kotlinx.coroutines.launch

private val tabOptions = listOf(0 to "Songs", 1 to "Albums", 2 to "Artists")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playbackState: PlaybackUiState,
    onSearchQueryChange: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            playbackState.currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    onTogglePlayPause = onTogglePlayPause,
                    onSkipNext = onSkipNext,
                    onClick = onOpenPlayer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your library",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }

            TextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search songs, artists, albums") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )

            PillNavRow(
                options = tabOptions,
                selected = selectedTab,
                onSelect = { selectedTab = it },
                fillWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                uiState.isLoading -> LoadingState()
                uiState.allSongs.isEmpty() -> EmptyLibraryState()
                else -> when (selectedTab) {
                    0 -> SongsTab(
                        songs = uiState.filteredSongs,
                        currentSongId = playbackState.currentSong?.id,
                        onPlaySong = { song -> onPlaySong(song, uiState.filteredSongs) }
                    )
                    1 -> AlbumsTab(albums = uiState.albums, onAlbumClick = onAlbumClick)
                    else -> ArtistsTab(artists = uiState.artists, onArtistClick = onArtistClick)
                }
            }
        }
    }
}

@Composable
private fun SongsTab(songs: List<Song>, currentSongId: Long?, onPlaySong: (Song) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // First list index for each distinct starting letter, so the scrollbar only
    // ever offers letters that actually exist and always lands on the right spot.
    val firstIndexByLetter = remember(songs) {
        val map = LinkedHashMap<Char, Int>()
        songs.forEachIndexed { index, song ->
            val letter = song.title.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
            map.putIfAbsent(letter, index)
        }
        map
    }
    val letters = remember(firstIndexByLetter) { firstIndexByLetter.keys.sorted() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 28.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                com.example.minimusic.ui.components.SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(song) }
                )
            }
        }

        AlphabetScrollbar(
            letters = letters,
            onLetterSelected = { letter ->
                firstIndexByLetter[letter]?.let { index ->
                    scope.launch { listState.scrollToItem(index) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp)
    ) {
        gridItems(albums, key = { it.id }) { album ->
            AlbumGridItem(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun ArtistsTab(artists: List<Artist>, onArtistClick: (Artist) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(artists, key = { it.name }) { artist ->
            ArtistListItem(artist = artist, onClick = { onArtistClick(artist) })
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No songs found on this device yet.",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
