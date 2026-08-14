package com.example.minimusic.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.components.AlbumGridItem
import com.example.minimusic.ui.components.AlphabetScrollbar
import com.example.minimusic.ui.components.ArtistListItem
import com.example.minimusic.ui.components.FloatingTabBar
import com.example.minimusic.ui.components.MiniPlayer
import com.example.minimusic.ui.components.SongGridItem
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.components.TabBarItem
import com.example.minimusic.ui.theme.PillShape
import com.example.minimusic.ui.viewmodel.LibraryEvent
import com.example.minimusic.ui.viewmodel.LibraryUiState
import com.example.minimusic.ui.viewmodel.SongSortOrder
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private enum class LibraryTab { SONGS, ARTISTS, ALBUMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playbackState: PlaybackUiState,
    events: SharedFlow<LibraryEvent>,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (SongSortOrder) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShufflePlayFrom: (Song, List<Song>) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onRetryDelete: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }

    // Handles the one round-trip Android 10+ requires to delete a song this app
    // doesn't own the underlying file for: launch the system confirmation dialog,
    // and on a successful result, retry the same delete (which then succeeds).
    var pendingRetrySong by remember { mutableStateOf<Song?>(null) }
    val currentOnRetryDelete = rememberUpdatedState(onRetryDelete)

    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val song = pendingRetrySong
        pendingRetrySong = null
        if (result.resultCode == Activity.RESULT_OK && song != null) {
            currentOnRetryDelete.value(song)
        }
    }

    LaunchedEffect(Unit) {
        events.collect { event ->
            when (event) {
                is LibraryEvent.RequestDeletePermission -> {
                    pendingRetrySong = event.song
                    deletePermissionLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
                is LibraryEvent.SongDeleted, is LibraryEvent.DeleteFailed -> {
                    pendingRetrySong = null
                }
            }
        }
    }

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
                    text = "MiniMusic",
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
                placeholder = { Text("Search....") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )

            var jumpToCurrentRequest by remember { mutableStateOf(0) }
            var isGridView by remember { mutableStateOf(false) }
            var sortMenuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .clickable(enabled = uiState.filteredSongs.isNotEmpty()) {
                            val startSong = uiState.filteredSongs.random()
                            onShufflePlayFrom(startSong, uiState.filteredSongs)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Shuffle", color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { jumpToCurrentRequest++ }
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Jump to current song",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable { isGridView = !isGridView }
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { sortMenuExpanded = true }
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Sort songs",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    SortMenu(
                        expanded = sortMenuExpanded,
                        selected = uiState.sortOrder,
                        onDismiss = { sortMenuExpanded = false },
                        onSelect = { onSortOrderChange(it); sortMenuExpanded = false }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.allSongs.isEmpty() -> EmptyLibraryState()
                    else -> when (selectedTab) {
                        LibraryTab.SONGS -> SongsTab(
                            songs = uiState.filteredSongs,
                            currentSongId = playbackState.currentSong?.id,
                            jumpToCurrentRequest = jumpToCurrentRequest,
                            isGridView = isGridView,
                            onPlaySong = { song -> onPlaySong(song, uiState.filteredSongs) },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onShufflePlayFrom = { song -> onShufflePlayFrom(song, uiState.filteredSongs) },
                            onDelete = onDeleteSong
                        )
                        LibraryTab.ALBUMS -> AlbumsTab(albums = uiState.albums, onAlbumClick = onAlbumClick)
                        LibraryTab.ARTISTS -> ArtistsTab(artists = uiState.artists, onArtistClick = onArtistClick)
                    }
                }
            }

            FloatingTabBar(
                items = listOf(
                    TabBarItem(LibraryTab.SONGS, "Songs", Icons.Filled.MusicNote),
                    TabBarItem(LibraryTab.ARTISTS, "Artists", Icons.Filled.Person),
                    TabBarItem(LibraryTab.ALBUMS, "Albums", Icons.Filled.Album)
                ),
                selected = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun SongsTab(
    songs: List<Song>,
    currentSongId: Long?,
    jumpToCurrentRequest: Int,
    isGridView: Boolean,
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShufflePlayFrom: (Song) -> Unit,
    onDelete: (Song) -> Unit
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // The item index the alphabet scrollbar should treat as "currently visible" —
    // a flat index into `songs` either way, since LazyGridState's index is also
    // flat (not per-row), so the same letter-lookup logic works for both modes.
    val firstVisibleIndex = if (isGridView) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex

    suspend fun scrollToIndex(index: Int) {
        if (isGridView) gridState.scrollToItem(index) else listState.scrollToItem(index)
    }

    suspend fun animateScrollToIndex(index: Int) {
        if (isGridView) gridState.animateScrollToItem(index) else listState.animateScrollToItem(index)
    }

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

    // Sorted (startIndex -> letter) pairs, used below to turn the currently-visible
    // list index into a section letter so the popup sticker tracks real scroll position.
    val sortedLetterEntries = remember(firstIndexByLetter) {
        firstIndexByLetter.entries.map { it.value to it.key }.sortedBy { it.first }
    }
    val currentLetter = remember(sortedLetterEntries, firstVisibleIndex) {
        sortedLetterEntries.lastOrNull { it.first <= firstVisibleIndex }?.second
            ?: sortedLetterEntries.firstOrNull()?.second
    }

    LaunchedEffect(jumpToCurrentRequest) {
        if (jumpToCurrentRequest == 0) return@LaunchedEffect
        val index = songs.indexOfFirst { it.id == currentSongId }
        if (index >= 0) animateScrollToIndex(index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 28.dp)
            ) {
                gridItems(songs, key = { it.id }) { song ->
                    SongGridItem(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        onClick = { onPlaySong(song) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onShufflePlayFrom = onShufflePlayFrom,
                        onDelete = onDelete
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 28.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = song.id == currentSongId,
                        onClick = { onPlaySong(song) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onShufflePlayFrom = onShufflePlayFrom,
                        onDelete = onDelete
                    )
                }
            }
        }

        AlphabetScrollbar(
            letters = letters,
            currentLetter = currentLetter,
            onLetterSelected = { letter ->
                firstIndexByLetter[letter]?.let { index ->
                    scope.launch { scrollToIndex(index) }
                }
            },
            // Match the LazyColumn/LazyGrid's own content padding exactly, so the
            // track's start/end line up with the first and last song — not the
            // full Box, which is slightly taller than the padded content area.
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 8.dp, bottom = 8.dp)
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
private fun SortMenu(
    expanded: Boolean,
    selected: SongSortOrder,
    onDismiss: () -> Unit,
    onSelect: (SongSortOrder) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortMenuGroup(title = "Name") {
            SortMenuOption("A to Z", SongSortOrder.NAME_A_Z, selected, onSelect)
            SortMenuOption("Z to A", SongSortOrder.NAME_Z_A, selected, onSelect)
        }
        SortMenuGroup(title = "Artist") {
            SortMenuOption("A to Z", SongSortOrder.ARTIST_A_Z, selected, onSelect)
            SortMenuOption("Z to A", SongSortOrder.ARTIST_Z_A, selected, onSelect)
        }
        SortMenuGroup(title = "Date added") {
            SortMenuOption("Newest first", SongSortOrder.DATE_ADDED_NEWEST, selected, onSelect)
            SortMenuOption("Oldest first", SongSortOrder.DATE_ADDED_OLDEST, selected, onSelect)
        }
    }
}

@Composable
private fun SortMenuGroup(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    content()
}

@Composable
private fun SortMenuOption(
    label: String,
    value: SongSortOrder,
    selected: SongSortOrder,
    onSelect: (SongSortOrder) -> Unit
) {
    val isSelected = value == selected
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        onClick = { onSelect(value) }
    )
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