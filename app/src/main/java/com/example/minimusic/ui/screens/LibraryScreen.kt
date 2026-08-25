package com.example.minimusic.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.ui.components.AlbumGridItem
import com.example.minimusic.ui.components.AlphabetScrollbar
import com.example.minimusic.ui.components.ArtistListItem
import com.example.minimusic.ui.components.MiniPlayerReservedHeight
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.PillShape
import com.example.minimusic.ui.theme.rememberArtColorRoles
import com.example.minimusic.ui.viewmodel.LibraryEvent
import com.example.minimusic.ui.viewmodel.LibraryUiState
import com.example.minimusic.ui.viewmodel.SongSortOrder
import kotlinx.coroutines.flow.SharedFlow

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
    onOpenDetails: (Song) -> Unit = {},
    onRetryDelete: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onOpenPlayer: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetryLoad: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.SONGS) }
    val view = LocalView.current
    val miniPlayerColors = rememberArtColorRoles(playbackState.currentSong?.albumArtUri)
    DisposableEffect(view, miniPlayerColors.surfaceVariant) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            window.navigationBarColor = miniPlayerColors.surfaceVariant.toArgb()
            val useDarkIcons = miniPlayerColors.surfaceVariant.luminance() > 0.52f
            controller.isAppearanceLightNavigationBars = useDarkIcons
        }
        onDispose { }
    }
    val footerHeight =
        MiniPlayerReservedHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val filteredSongs = uiState.filteredSongs

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
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.secondary
                    )
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
                trailingIcon = {
                    AnimatedVisibility(
                        visible = uiState.searchQuery.isNotEmpty(),
                        enter = fadeIn(animationSpec = MiniMusicMotion.fastEffects()) +
                            scaleIn(initialScale = 0.82f, animationSpec = MiniMusicMotion.fastEffects()),
                        exit = fadeOut(animationSpec = MiniMusicMotion.fastEffects()) +
                            scaleOut(targetScale = 0.82f, animationSpec = MiniMusicMotion.fastEffects())
                    ) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
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
                                    horizontalPadding = 10.dp,
                                    shape = PillGroupShapes.First,
                                    // Fixed width sized to the longest label
                                    // ("Artists") so the pill never resizes when
                                    // switching tabs — without this, "Songs" and
                                    // "Albums" (shorter/different width) made the
                                    // whole pill shrink and grow between taps.
                                    // Padding trimmed from the shared 16.dp down
                                    // to 10.dp here specifically so the fixed
                                    // width can stay compact (matching the
                                    // right-side group's total width) while
                                    // still leaving enough room for the text
                                    // to render without truncating.
                                    modifier = Modifier.width(SelectorLabelWidth)
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
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                PillButton(
                                    onClick = { tabMenuExpanded = true },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Last,
                                    modifier = Modifier.width(SelectorChevronWidth)
                                ) {
                                    Icon(
                                        Icons.Filled.ExpandMore,
                                        contentDescription = "Switch view",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            LibraryTabDialog(
                                expanded = tabMenuExpanded,
                                selected = selectedTab,
                                onDismiss = { tabMenuExpanded = false },
                                onSelect = {
                                    selectedTab = it
                                    tabMenuExpanded = false
                                }
                            )
                        }

                        // Right controls: Locate, Shuffle, Sort — three
                        // segments of one continuous pill silhouette, same
                        // shape/height treatment as the left selector, but a
                        // more neutral fill: these are one-off action taps,
                        // not a persistent view-state toggle like the left
                        // selector, so they shouldn't carry the same
                        // saturated accent tone.
                        Box {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                PillButton(
                                    onClick = { jumpToCurrentRequest++ },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.First,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.width(ControlSegmentWidth)
                                ) {
                                    Icon(
                                        Icons.Filled.MyLocation,
                                        contentDescription = "Jump to current song",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                PillButton(
                                    onClick = {
                                        if (filteredSongs.isNotEmpty()) {
                                            val startSong = filteredSongs.random()
                                            onShufflePlayFrom(startSong, filteredSongs)
                                        }
                                    },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Middle,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.width(ControlSegmentWidth)
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                PillButton(
                                    onClick = { sortMenuExpanded = true },
                                    horizontalPadding = 12.dp,
                                    shape = PillGroupShapes.Last,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.width(ControlSegmentWidth)
                                ) {
                                    Icon(
                                        Icons.Filled.Sort,
                                        contentDescription = "Sort songs",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            SortMenu(
                                expanded = sortMenuExpanded,
                                selected = uiState.sortOrder,
                                onDismiss = { sortMenuExpanded = false },
                                onSelect = { onSortOrderChange(it) }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.isLoading -> LoadingState()
                            uiState.loadError != null -> LibraryLoadErrorState(
                                message = uiState.loadError,
                                onRetry = onRetryLoad
                            )
                            uiState.allSongs.isEmpty() -> EmptyLibraryState()
                            else -> when (selectedTab) {
                                LibraryTab.SONGS -> SongsTab(
                                    songs = filteredSongs,
                                    currentSongId = playbackState.currentSong?.id,
                                    jumpToCurrentRequest = jumpToCurrentRequest,
                                    bottomContentPadding = footerHeight,
                                    onPlaySong = { song -> onPlaySong(song, filteredSongs) },
                                    onPlayNext = onPlayNext,
                                    onAddToQueue = onAddToQueue,
                                    onShufflePlayFrom = { song -> onShufflePlayFrom(song, filteredSongs) },
                                    onDelete = onDeleteSong,
                                    onOpenDetails = onOpenDetails
                                )
                                LibraryTab.ALBUMS -> AlbumsTab(
                                    albums = uiState.albums,
                                    bottomContentPadding = footerHeight,
                                    onAlbumClick = onAlbumClick
                                )
                                LibraryTab.ARTISTS -> ArtistsTab(
                                    artists = uiState.artists,
                                    bottomContentPadding = footerHeight,
                                    onArtistClick = onArtistClick
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}


/**
 * Builds the "which letter does this index fall under" lookup used to drive
 * [AlphabetScrollbar]'s preview bubble — shared by all three tabs (Songs,
 * Artists, Albums) so each gets identical letter-jump behavior keyed off
 * whatever text the caller extracts a label from (song title, artist name,
 * or album title).
 */
@Composable
private fun <T> rememberLetterIndex(items: List<T>, labelOf: (T) -> String): (Int) -> Char? {
    val sortedLetterEntries = remember(items) {
        val map = LinkedHashMap<Char, Int>()
        items.forEachIndexed { index, item ->
            val letter = labelOf(item).firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
            map.putIfAbsent(letter, index)
        }
        map.entries.map { it.value to it.key }.sortedBy { it.first }
    }
    return remember(sortedLetterEntries) {
        { index -> sortedLetterEntries.lastOrNull { it.first <= index }?.second ?: sortedLetterEntries.firstOrNull()?.second }
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
    onDelete: (Song) -> Unit,
    onOpenDetails: (Song) -> Unit
) {
    val listState = rememberLazyListState()
    val letterForIndex = rememberLetterIndex(songs) { it.title }

    LaunchedEffect(jumpToCurrentRequest) {
        if (jumpToCurrentRequest == 0) return@LaunchedEffect
        val index = songs.indexOfFirst { it.id == currentSongId }
        if (index >= 0) {
            // Locate is an explicit position reset, not a smooth navigation gesture. A
            // zero-offset request makes the selected row share the exact same leading
            // edge as item zero and eliminates the previous-row sliver.
            listState.requestScrollToItem(index = index, scrollOffset = 0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Bottom padding matches the footer's actual measured height, so
            // the last song is never hidden underneath the now-opaque mini
            // player + nav bar, without reserving more space than needed.
            contentPadding = PaddingValues(bottom = bottomContentPadding + 8.dp, end = 28.dp)
        ) {
            items(
                items = songs,
                key = { it.id },
                contentType = { "song-row" }
            ) { song ->
                SongListItem(
                    song = song,
                    isPlaying = song.id == currentSongId,
                    onClick = { onPlaySong(song) },
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onShufflePlayFrom = onShufflePlayFrom,
                    onDelete = onDelete,
                    onOpenDetails = onOpenDetails
                )
            }
        }

        AlphabetScrollbar(
            itemCount = songs.size,
            currentIndex = listState.firstVisibleItemIndex,
            letterForIndex = letterForIndex,
            onScrollToIndex = { index ->
                listState.requestScrollToItem(index = index, scrollOffset = 0)
            },
            // The top remains aligned with the first song container. The bottom
            // Keep the same 3dp visual inset at both ends: the top is 8dp
            // versus the first card's 5dp, so the bottom is shortened by 3dp.
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = bottomContentPadding + 16.dp)
        )
    }
}
@Composable
private fun AlbumsTab(

    albums: List<Album>,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onAlbumClick: (Album) -> Unit
) {
    val gridState = rememberLazyGridState()
    val letterForIndex = rememberLetterIndex(albums) { it.title }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp + 28.dp,
                bottom = bottomContentPadding + 12.dp
            )
        ) {
            gridItems(albums, key = { it.id }) { album ->
                AlbumGridItem(album = album, onClick = { onAlbumClick(album) })
            }
        }

        AlphabetScrollbar(
            itemCount = albums.size,
            currentIndex = gridState.firstVisibleItemIndex,
            letterForIndex = letterForIndex,
            onScrollToIndex = { index ->
                gridState.requestScrollToItem(index = index, scrollOffset = 0)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 12.dp, bottom = bottomContentPadding + 12.dp)
        )
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onArtistClick: (Artist) -> Unit
) {
    val listState = rememberLazyListState()
    val letterForIndex = rememberLetterIndex(artists) { it.name }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomContentPadding + 8.dp, end = 28.dp)
        ) {
            items(
                items = artists,
                key = { it.name },
                contentType = { "artist-row" }
            ) { artist ->
                ArtistListItem(artist = artist, onClick = { onArtistClick(artist) })
            }
        }

        AlphabetScrollbar(
            itemCount = artists.size,
            currentIndex = listState.firstVisibleItemIndex,
            letterForIndex = letterForIndex,
            onScrollToIndex = { index ->
                listState.requestScrollToItem(index = index, scrollOffset = 0)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 8.dp, bottom = bottomContentPadding)
        )
    }
}

/** Fixed height shared by every [PillButton] segment across both control
 *  groups — without this, a segment with a text label (taller intrinsic
 *  line-height) and an icon-only segment can each size their own Row
 *  slightly differently even with identical vertical padding, which is
 *  exactly what made the Songs/chevron pill and the Shuffle/Locate/Sort
 *  pill sit at visibly different heights before. */
private val PillButtonHeight = 44.dp

/** Fixed width for the Songs/Artists/Albums label segment (icon + text),
 *  sized to comfortably fit "Artists" — the longest of the three labels —
 *  so the pill's overall width never changes when switching tabs. */
private val SelectorLabelWidth = 96.dp

/** Fixed width for the chevron segment next to the label, and for each of
 *  the three Shuffle/Locate/Sort segments — chosen so the left group
 *  (label + chevron) and the right group (3 icon segments) add up to the
 *  same total width, including the 2.dp gaps between segments in each
 *  group. Right group (unchanged icon-only sizing): 3 segments of 46.dp +
 *  2 gaps of 2.dp = 142.dp. Left group needs label + chevron + 1 gap to
 *  also total 142.dp, so with chevron kept at 46.dp: 142 - 2 - 46 = 94.dp
 *  for the label segment — see [SelectorLabelWidth] above, rounded to
 *  96.dp for comfortable text fit (144.dp total, 2.dp over — negligible). */
private val SelectorChevronWidth = 46.dp
private val ControlSegmentWidth = 46.dp

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
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
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

private enum class SortField {
    NAME, ARTIST, ALBUM, DURATION, DATE_ADDED
}

private fun sortFieldOf(order: SongSortOrder): SortField = when (order) {
    SongSortOrder.NAME_A_Z, SongSortOrder.NAME_Z_A -> SortField.NAME
    SongSortOrder.ARTIST_A_Z, SongSortOrder.ARTIST_Z_A -> SortField.ARTIST
    SongSortOrder.ALBUM_A_Z, SongSortOrder.ALBUM_Z_A -> SortField.ALBUM
    SongSortOrder.DURATION_SHORTEST, SongSortOrder.DURATION_LONGEST -> SortField.DURATION
    SongSortOrder.DATE_ADDED_NEWEST, SongSortOrder.DATE_ADDED_OLDEST -> SortField.DATE_ADDED
}

private fun sortOrderOf(field: SortField, ascending: Boolean): SongSortOrder = when (field) {
    SortField.NAME -> if (ascending) SongSortOrder.NAME_A_Z else SongSortOrder.NAME_Z_A
    SortField.ARTIST -> if (ascending) SongSortOrder.ARTIST_A_Z else SongSortOrder.ARTIST_Z_A
    SortField.ALBUM -> if (ascending) SongSortOrder.ALBUM_A_Z else SongSortOrder.ALBUM_Z_A
    SortField.DURATION -> if (ascending) SongSortOrder.DURATION_SHORTEST else SongSortOrder.DURATION_LONGEST
    SortField.DATE_ADDED -> if (ascending) SongSortOrder.DATE_ADDED_OLDEST else SongSortOrder.DATE_ADDED_NEWEST
}

@Composable
private fun LibraryTabDialog(
    expanded: Boolean,
    selected: LibraryTab,
    onDismiss: () -> Unit,
    onSelect: (LibraryTab) -> Unit
) {
    if (!expanded) return
    val tabs = listOf(
        LibraryTab.SONGS,
        LibraryTab.ARTISTS,
        LibraryTab.ALBUMS
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("View by") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEach { tab ->
                    val selectedRow = selected == tab
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelect(tab) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedRow) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (selectedRow) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedRow) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            )
                            RadioButton(
                                selected = selectedRow,
                                onClick = { onSelect(tab) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    selected: SongSortOrder,
    onDismiss: () -> Unit,
    onSelect: (SongSortOrder) -> Unit
) {
    if (!expanded) return
    var selectedField by remember(selected) { mutableStateOf(sortFieldOf(selected)) }
    var ascending by remember(selected) {
        mutableStateOf(
            selected in setOf(
                SongSortOrder.NAME_A_Z,
                SongSortOrder.ARTIST_A_Z,
                SongSortOrder.ALBUM_A_Z,
                SongSortOrder.DURATION_SHORTEST,
                SongSortOrder.DATE_ADDED_OLDEST
            )
        )
    }
    val fields = listOf(
        SortField.NAME to "Title",
        SortField.ARTIST to "Artist",
        SortField.ALBUM to "Album",
        SortField.DATE_ADDED to "Date added",
        SortField.DURATION to "Duration"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Sort by") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortDirectionPill(
                    ascending = ascending,
                    onAscending = {
                        ascending = true
                        onSelect(sortOrderOf(selectedField, true))
                    },
                    onDescending = {
                        ascending = false
                        onSelect(sortOrderOf(selectedField, false))
                    }
                )
                fields.forEach { (field, label) ->
                    val selectedFieldRow = selectedField == field
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedField = field
                                onSelect(sortOrderOf(field, ascending))
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedFieldRow) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedFieldRow) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = selectedFieldRow,
                                onClick = {
                                    selectedField = field
                                    onSelect(sortOrderOf(field, ascending))
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun SortDirectionPill(
    ascending: Boolean,
    onAscending: () -> Unit,
    onDescending: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SortDirectionOption(
            label = "Ascending",
            selected = ascending,
            onClick = onAscending,
            modifier = Modifier.weight(1f)
        )
        SortDirectionOption(
            label = "Descending",
            selected = !ascending,
            onClick = onDescending,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SortDirectionOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = MiniMusicMotion.defaultEffects()) +
            scaleIn(initialScale = 0.94f, animationSpec = MiniMusicMotion.selectionEffects()),
        exit = fadeOut(animationSpec = MiniMusicMotion.fastEffects()) +
            scaleOut(targetScale = 0.94f, animationSpec = MiniMusicMotion.fastEffects()),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            M3ExpressiveContainedLoadingIndicator()
        }
    }
}

/**
 * Local spec-matched fallback for the contained M3 Expressive indicator. The
 * project is pinned to Material 3 1.4.0, whose Android artifact does not yet
 * expose ContainedLoadingIndicator, so this keeps the documented 48dp overall
 * footprint, 38dp container, contained color roles, and expressive motion
 * without adding a network dependency or changing the project version.
 */
@Composable
private fun M3ExpressiveContainedLoadingIndicator() {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "libraryLoadingIndicator"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                                durationMillis = 1200,
                easing = androidx.compose.animation.core.LinearEasing
            )

        ),
        label = "libraryLoadingRotation"
    )
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            rotate(rotation) {
                drawRoundRect(
                    color = indicatorColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.22f),
                    size = size
                )
            }
        }
    }
}

@Composable
private fun LibraryLoadErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
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
