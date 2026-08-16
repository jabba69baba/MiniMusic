package com.example.minimusic.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.components.AlbumGridItem
import com.example.minimusic.ui.components.AlphabetScrollbar
import com.example.minimusic.ui.components.ArtistListItem
import com.example.minimusic.ui.components.MiniPlayer
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.theme.PillShape
import com.example.minimusic.ui.viewmodel.LibraryEvent
import com.example.minimusic.ui.viewmodel.LibraryUiState
import com.example.minimusic.ui.viewmodel.SongSortOrder
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private enum class LibraryTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SONGS("Songs", Icons.Filled.MusicNote),
    ARTISTS("Artists", Icons.Filled.Person),
    ALBUMS("Albums", Icons.Filled.Album)
}

/** The library drawer's shape: rounded only at the top, flat everywhere else —
 *  it's one continuous container holding both the Shuffle/Locate/Sort row and
 *  the song list beneath it, with no visual seam between the two. */
private val LibraryDrawerShape = RoundedCornerShape(
    topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp
)

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
    val density = LocalDensity.current
    var footerHeight by remember { mutableStateOf(0.dp) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MiniMusic",
                    style = MaterialTheme.typography.headlineSmall,
                    // Softer wallpaper-tinted tone than the default near-white
                    // onSurface (Material's legibility-first default for
                    // titles/body text) — secondary carries hue from the
                    // Monet palette without the higher-saturation punch of
                    // primary, which read as too loud for a page title.
                    color = MaterialTheme.colorScheme.secondary
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
            var sortMenuExpanded by remember { mutableStateOf(false) }
            var tabMenuExpanded by remember { mutableStateOf(false) }

            // One continuous drawer, rounded only at the top: the selector/
            // controls row and the song list beneath it share the same
            // surface with no seam, matching the reference — not two
            // visually separate stacked containers.
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = LibraryDrawerShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left selector: current view (Songs/Artists/Albums)
                        // and the chevron next to it — two segments of one
                        // continuous pill silhouette (rounded on the outer
                        // ends, square where they meet), separated by a
                        // hairline gap rather than genuinely separate pills.
                        Box {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PillButton(
                                    onClick = { tabMenuExpanded = true },
                                    horizontalPadding = 16.dp,
                                    shape = PillGroupShapes.First
                                ) {
                                    Icon(
                                        selectedTab.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        selectedTab.label,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                PillButton(
                                    onClick = { tabMenuExpanded = true },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Last
                                ) {
                                    Icon(
                                        Icons.Filled.ExpandMore,
                                        contentDescription = "Switch view",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            DropdownMenu(expanded = tabMenuExpanded, onDismissRequest = { tabMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Songs") },
                                    leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                                    onClick = { selectedTab = LibraryTab.SONGS; tabMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Artists") },
                                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                    onClick = { selectedTab = LibraryTab.ARTISTS; tabMenuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Albums") },
                                    leadingIcon = { Icon(Icons.Filled.Album, contentDescription = null) },
                                    onClick = { selectedTab = LibraryTab.ALBUMS; tabMenuExpanded = false }
                                )
                            }
                        }

                        // Right controls: Shuffle, Locate, Sort — three
                        // segments of one continuous pill silhouette, same
                        // treatment as the left selector.
                        Box {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PillButton(
                                    onClick = {
                                        if (uiState.filteredSongs.isNotEmpty()) {
                                            val startSong = uiState.filteredSongs.random()
                                            onShufflePlayFrom(startSong, uiState.filteredSongs)
                                        }
                                    },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.First
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                PillButton(
                                    onClick = { jumpToCurrentRequest++ },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Middle
                                ) {
                                    Icon(
                                        Icons.Filled.MyLocation,
                                        contentDescription = "Jump to current song",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                PillButton(
                                    onClick = { sortMenuExpanded = true },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Last
                                ) {
                                    Icon(
                                        Icons.Filled.FilterList,
                                        contentDescription = "Sort songs",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
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
                                    bottomContentPadding = footerHeight,
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
                }
            }
        }

        // Permanent mini player, pinned to the bottom edge: always present
        // regardless of playback state (shows a placeholder when nothing has
        // ever been played), so its height never changes and the song list's
        // bottom padding above it stays stable. Replaces the old separate
        // Songs/Artists/Albums nav bar entirely — that switching now happens
        // via the dropdown pill at the top of the drawer.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .onGloballyPositioned { coordinates ->
                    footerHeight = with(density) { coordinates.size.height.toDp() }
                }
        ) {
            MiniPlayer(
                song = playbackState.currentSong,
                isPlaying = playbackState.isPlaying,
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onClick = onOpenPlayer
            )
        }
    }
}


@Composable
private fun SongsTab(
    songs: List<Song>,
    currentSongId: Long?,
    jumpToCurrentRequest: Int,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShufflePlayFrom: (Song) -> Unit,
    onDelete: (Song) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // First list index for each distinct starting letter — no longer used to
    // drive the thumb's position (that's now raw scroll progress through the
    // whole list, so it moves smoothly regardless of how unevenly songs are
    // spread across letters), but still used to resolve which letter to show
    // in the preview bubble while actively dragging the thumb.
    val firstIndexByLetter = remember(songs) {
        val map = LinkedHashMap<Char, Int>()
        songs.forEachIndexed { index, song ->
            val letter = song.title.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
            map.putIfAbsent(letter, index)
        }
        map
    }
    // Sorted (startIndex -> letter) pairs, used to turn any list index (the
    // one currently under the finger while dragging) into its section letter.
    val sortedLetterEntries = remember(firstIndexByLetter) {
        firstIndexByLetter.entries.map { it.value to it.key }.sortedBy { it.first }
    }
    val letterForIndex: (Int) -> Char? = remember(sortedLetterEntries) {
        { index -> sortedLetterEntries.lastOrNull { it.first <= index }?.second ?: sortedLetterEntries.firstOrNull()?.second }
    }

    LaunchedEffect(jumpToCurrentRequest) {
        if (jumpToCurrentRequest == 0) return@LaunchedEffect
        val index = songs.indexOfFirst { it.id == currentSongId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Bottom padding matches the footer's actual measured height, so
            // the last song is never hidden underneath the now-opaque mini
            // player + nav bar, without reserving more space than needed.
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomContentPadding + 8.dp, end = 28.dp)
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

        AlphabetScrollbar(
            itemCount = songs.size,
            currentIndex = listState.firstVisibleItemIndex,
            letterForIndex = letterForIndex,
            onScrollToIndex = { index ->
                scope.launch { listState.scrollToItem(index) }
            },
            // Matches the song list's own top/bottom content padding exactly
            // so the track starts level with the first song container and
            // ends level with the last one, never running behind the footer.
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = bottomContentPadding + 8.dp)
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

/** Fixed height shared by every [PillButton] segment across both control
 *  groups — without this, a segment with a text label (taller intrinsic
 *  line-height) and an icon-only segment can each size their own Row
 *  slightly differently even with identical vertical padding, which is
 *  exactly what made the Songs/chevron pill and the Shuffle/Locate/Sort
 *  pill sit at visibly different heights before. */
private val PillButtonHeight = 44.dp

/**
 * A single segment of a multi-part pill control — several of these sit in a
 * row with a hairline gap between them and per-segment corner shapes (see
 * [PillGroupShapes]) so the group reads as one continuous pill silhouette,
 * not a row of fully separate buttons and not one pill with divider lines
 * drawn inside it. Used for both the Songs/Artists/Albums selector and the
 * Shuffle/Locate/Sort controls, so every segment across both groups shares
 * the same height, fill color, and content weight.
 */
@Composable
private fun PillButton(
    onClick: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.height(PillButtonHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Corner shapes for a group of [PillButton]s meant to read as one
 *  continuous pill split into segments — full stadium radius (a corner
 *  size larger than the pill's own height always clamps to a perfect
 *  half-circle) on the outer side of each end segment. Where two segments
 *  meet, a small (not zero) radius on both facing corners gives the soft
 *  inward curve the design calls for — a hard square edge there read as
 *  visually disconnected rather than like two pieces of one pill. */
private object PillGroupShapes {
    private val Full = 50.dp
    private val Meeting = 5.dp
    val First = RoundedCornerShape(topStart = Full, topEnd = Meeting, bottomEnd = Meeting, bottomStart = Full)
    val Middle = RoundedCornerShape(Meeting)
    val Last = RoundedCornerShape(topStart = Meeting, topEnd = Full, bottomEnd = Full, bottomStart = Meeting)
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
