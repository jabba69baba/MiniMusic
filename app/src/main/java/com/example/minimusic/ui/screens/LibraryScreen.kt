package com.example.minimusic.ui.screens

import kotlin.math.roundToInt

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.components.performMiniMusicHaptic
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.AlbumGridItem
import com.example.minimusic.ui.components.AlphabetScrollbar
import com.example.minimusic.ui.components.ArtistListItem
import com.example.minimusic.ui.components.AlbumGridSkeleton
import com.example.minimusic.ui.components.ArtistListSkeleton
import com.example.minimusic.ui.components.MiniMusicImageLoader
import com.example.minimusic.ui.components.MiniPlayerReservedHeight
import com.example.minimusic.ui.components.SongListItem
import com.example.minimusic.ui.components.SongListSkeleton
import com.example.minimusic.ui.theme.LocalMiniMusicReducedMotion
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.theme.MiniMusicType
import com.example.minimusic.ui.viewmodel.LibraryEvent
import com.example.minimusic.ui.viewmodel.LibraryUiState
import com.example.minimusic.ui.viewmodel.AlbumSortOrder
import com.example.minimusic.ui.viewmodel.ArtistSortOrder
import com.example.minimusic.ui.viewmodel.SongSortOrder
import com.example.minimusic.ui.viewmodel.sortAlbums
import com.example.minimusic.ui.viewmodel.sortArtists
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.Job
import kotlin.math.abs
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    currentSongId: Long?,
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
    val reducedMotion = LocalMiniMusicReducedMotion.current
    // Each tab carries its own order, so switching tabs never silently reorders
    // a list the user has already arranged. Songs' order lives in the view
    // model (it is applied to the filtered list there); these two are applied
    // here because the artists and albums lists are what this screen renders.
    var artistSortOrder by remember { mutableStateOf(ArtistSortOrder.NAME_A_Z) }
    var albumSortOrder by remember { mutableStateOf(AlbumSortOrder.TITLE_A_Z) }
    val sortedArtists = remember(uiState.artists, artistSortOrder) {
        sortArtists(uiState.artists, artistSortOrder)
    }
    val sortedAlbums = remember(uiState.albums, albumSortOrder) {
        sortAlbums(uiState.albums, albumSortOrder)
    }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val view = LocalView.current
    val appNavigationBarColor = MaterialTheme.colorScheme.surface
    DisposableEffect(view, appNavigationBarColor) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            window.navigationBarColor = appNavigationBarColor.toArgb()
            val useDarkIcons = appNavigationBarColor.luminance() > 0.52f
            controller.isAppearanceLightNavigationBars = useDarkIcons
        }
        onDispose { }
    }
    // The outer library column already consumes system-bar insets. Reserve
    // only the persistent miniplayer here so navigation space is not counted twice.
    val footerHeight = MiniPlayerReservedHeight
    val filteredSongs = uiState.filteredSongs
    // Which of the four mutually exclusive states the content area is in. A
    // skeleton stands in only for content that has never arrived: a rescan of a
    // populated library keeps the real rows on screen rather than replacing
    // them with placeholders, so the layout never shifts under the user.
    val libraryContentState = when {
        uiState.isLoading && uiState.allSongs.isEmpty() -> LibraryContentState.Loading
        uiState.loadError != null -> LibraryContentState.Error
        uiState.allSongs.isEmpty() -> LibraryContentState.Empty
        else -> LibraryContentState.Ready
    }

    // Handles the one round-trip Android 10+ requires to delete a song this app
    // doesn't own the underlying file for: launch the system confirmation dialog,
    // and on a successful result, retry the same delete (which then succeeds).
    var pendingRetrySong by remember { mutableStateOf<Song?>(null) }
    val currentOnRetryDelete = rememberUpdatedState(onRetryDelete)
    val snackbarHostState = remember { SnackbarHostState() }

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
                is LibraryEvent.SongDeleted -> {
                    pendingRetrySong = null
                    snackbarHostState.showSnackbar("Deleted ${event.song.title}")
                }
                is LibraryEvent.DeleteFailed -> {
                    pendingRetrySong = null
                    snackbarHostState.showSnackbar(event.message)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort moved up here from the tab row. Sorting is a
                    // preference about how a list is shown rather than a
                    // one-off action, and it now serves all three tabs — so it
                    // belongs beside Settings in the bar, not squeezed into the
                    // row that has to fit the switcher as well.
                    //
                    // The glyph takes no flip. Material's sort icon already
                    // draws its bars longest-first (18, 12, 6 from the top) —
                    // the descending stack it should show — so the scaleY = -1
                    // that used to sit here was mirroring the right icon into a
                    // bar chart that grows downwards.
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            Icons.Filled.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = {},
                active = false,
                onActiveChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search....") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = uiState.searchQuery.isNotEmpty(),
                        // Scale-only pop, no fade — the clear button grows
                        // over the static search field.
                        enter = scaleIn(initialScale = 0.82f, animationSpec = MiniMusicMotion.fastEffects()),
                        exit = scaleOut(targetScale = 1f, animationSpec = MiniMusicMotion.fastEffects())
                    ) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = androidx.compose.material3.SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    dividerColor = Color.Transparent
                ),
                content = {}
            )

            var jumpToCurrentRequest by remember { mutableStateOf(0) }
            var stopSongScrollRequest by remember { mutableStateOf(0) }
            // sortMenuExpanded belongs to the screen, not to this drawer: the
            // button that opens the menu now lives up in the app bar, outside
            // this scope. Declaring a second one here would shadow the outer
            // state and leave the app-bar button able to set a value nothing
            // reads.
            val hapticView = LocalView.current
            val hapticsEnabled = LocalMiniMusicHaptics.current

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The switcher takes every dp the action pill does not
                        // need. It is the primary control in this row — it
                        // changes what the whole screen below is showing —
                        // while Locate and Shuffle are one-off taps, so the
                        // layout gives the width to the thing that is used
                        // most and lets the buttons sit at their natural size
                        // instead of stretching to fill a fixed slot.
                        ExpandingCategoryControl(
                            selected = selectedTab,
                            onSelect = { tab ->
                                if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                selectedTab = tab
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Locate and Shuffle: two segments of one continuous
                        // pill — the grouped treatment this row used before the
                        // switcher redesign. The segments share a container and
                        // a hairline, with the group's outer corners at a full
                        // stadium radius and only a small radius where they
                        // meet, so it reads as one object split in two rather
                        // than two buttons that happen to be adjacent.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillButton(
                                onClick = {
                                    if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                    stopSongScrollRequest++
                                    jumpToCurrentRequest++
                                },
                                horizontalPadding = 12.dp,
                                shape = PillGroupShapes.First,
                                modifier = Modifier.width(ControlSegmentWidth)
                            ) {
                                Icon(
                                    Icons.Filled.MyLocation,
                                    contentDescription = "Jump to current song",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            PillButton(
                                onClick = {
                                    if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                    // Shuffle changes playback in the background
                                    // only: it must NOT stop an active fling or
                                    // re-anchor the list (that's Locate's job).
                                    // Scrolling continues undisturbed.
                                    if (filteredSongs.isNotEmpty()) {
                                        val startSong = filteredSongs.random()
                                        onShufflePlayFrom(startSong, filteredSongs)
                                    }
                                },
                                horizontalPadding = 12.dp,
                                shape = PillGroupShapes.Last,
                                modifier = Modifier.width(ControlSegmentWidth)
                            ) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    CategorySortMenu(
                        expanded = sortMenuExpanded,
                        tab = selectedTab,
                        songSortOrder = uiState.sortOrder,
                        artistSortOrder = artistSortOrder,
                        albumSortOrder = albumSortOrder,
                        onDismiss = { sortMenuExpanded = false },
                        onSongSort = onSortOrderChange,
                        onArtistSort = { artistSortOrder = it },
                        onAlbumSort = { albumSortOrder = it }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        // Skeleton-to-content handoff. This is the one place the
                        // guide sanctions overlapping fades — "content quickly
                        // fades in once it's loaded, on top of the skeleton
                        // loader" — because what overlaps is a placeholder
                        // standing in for the content, not other content.
                        Crossfade(
                            targetState = libraryContentState,
                            animationSpec = tween(MiniMusicMotion.contentHandoffMillis),
                            label = "libraryContent"
                        ) { contentState ->
                            when (contentState) {
                                LibraryContentState.Loading -> LibrarySkeleton(
                                    tab = selectedTab,
                                    bottomContentPadding = footerHeight
                                )

                                LibraryContentState.Error -> LibraryLoadErrorState(
                                    message = uiState.loadError ?: "",
                                    onRetry = onRetryLoad
                                )

                                LibraryContentState.Empty -> EmptyLibraryState()

                                // The three tabs are peers of one set, so the
                                // change between them is a *lateral* transition:
                                // both contents slide in unison along one axis
                                // with no fade, which reads them as equals and
                                // hints that the content area is swipeable. A
                                // fade here would read as a hierarchy move, and
                                // the jump cut this replaced left the user to
                                // work out what had changed. The clock is the
                                // carousel token — critically damped, because a
                                // full-width slide that settles with an overshoot
                                // charges for the bounce on every frame.
                                LibraryContentState.Ready -> AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = {
                                        val direction =
                                            if (targetState.ordinal > initialState.ordinal) 1 else -1
                                        if (reducedMotion) {
                                            fadeIn(MiniMusicMotion.fastEffects()) togetherWith
                                                fadeOut(MiniMusicMotion.fastEffects())
                                        } else {
                                            slideInHorizontally(
                                                animationSpec = MiniMusicMotion.carouselSpatial()
                                            ) { width -> direction * width } togetherWith
                                                slideOutHorizontally(
                                                    animationSpec = MiniMusicMotion.carouselSpatial()
                                                ) { width -> -direction * width }
                                        }
                                    },
                                    label = "libraryTabs"
                                ) { tab ->
                                    when (tab) {
                                        LibraryTab.SONGS -> SongsTab(
                                        songs = filteredSongs,
                                        currentSongId = currentSongId,
                                        jumpToCurrentRequest = jumpToCurrentRequest,
                                        stopScrollRequest = stopSongScrollRequest,
                                        bottomContentPadding = footerHeight,
                                        onPlaySong = { song -> onPlaySong(song, filteredSongs) },
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onShufflePlayFrom = { song -> onShufflePlayFrom(song, filteredSongs) },
                                        onDelete = onDeleteSong,
                                        onOpenDetails = onOpenDetails
                                    )
                                    LibraryTab.ALBUMS -> AlbumsTab(
                                        albums = sortedAlbums,
                                        bottomContentPadding = footerHeight,
                                        onAlbumClick = onAlbumClick
                                    )
                                    LibraryTab.ARTISTS -> ArtistsTab(
                                        artists = sortedArtists,
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = footerHeight + 12.dp)
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongsTab(
    songs: List<Song>,
    currentSongId: Long?,
    jumpToCurrentRequest: Int,
    stopScrollRequest: Int,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onPlaySong: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShufflePlayFrom: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onOpenDetails: (Song) -> Unit
) {
    val listState = rememberLazyListState(cacheWindow = ListPrefetchWindow)
    val scrollScope = rememberCoroutineScope()
    val context = LocalContext.current
    var locateJob by remember { mutableStateOf<Job?>(null) }
    val letterForIndex = rememberLetterIndex(songs) { it.title }

    LaunchedEffect(songs) {
        // Front-loaded at app open (Auxio-style): decode the first screenfuls
        // into the memory cache while the user is still orienting, instead of
        // trickling background loads that compete with scroll animations for
        // minutes afterwards. Enqueueing is cheap; decodes run on Coil's own
        // dispatcher and never block composition.
        songs.asSequence()
            .mapNotNull { it.albumArtUri }
            .distinct()
            .take(64)
            .forEach { artworkUri ->
                MiniMusicImageLoader.get(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(artworkUri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(RowArtSizePx)
                        .build()
                )
            }
    }

    LaunchedEffect(stopScrollRequest) {
        if (stopScrollRequest == 0) return@LaunchedEffect
        locateJob?.cancel()
        listState.scroll(MutatePriority.PreventUserInput) {}
    }

    LaunchedEffect(jumpToCurrentRequest) {
        if (jumpToCurrentRequest == 0) return@LaunchedEffect
        val index = songs.indexOfFirst { it.id == currentSongId }
        if (index >= 0) {
            // Locate cancels any active fling/fast-scroll before moving to the
            // target, preventing old velocity from carrying into the new position.
            locateJob?.cancel()
            locateJob = scrollScope.launch {
                listState.scroll(MutatePriority.PreventUserInput) {}
                // Warm the target window FIRST: rows must land with art ready,
                // not cold-decode it after arrival.
                preloadArtWindow(context, songs.size, index, ArtPreloadRadius, RowArtSizePx) {
                    songs.getOrNull(it)?.albumArtUri
                }
                val distance = abs(index - listState.firstVisibleItemIndex)
                if (distance > LocateAnimateThreshold) {
                    // Jump to just outside the target, then glide the final
                    // stretch. A full-distance animateScrollToItem composes and
                    // art-loads every intermediate row — the sustained loading
                    // lag felt after shuffle-then-locate across a big library.
                    val staged = (index + if (index > listState.firstVisibleItemIndex) -LocateGlideTail else LocateGlideTail)
                        .coerceIn(0, songs.size - 1)
                    listState.scrollToItem(index = staged, scrollOffset = 0)
                    locateCentered(listState, index, songs.size)
                } else {
                    locateCentered(listState, index, songs.size)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Bottom padding matches the footer's actual measured height, so
            // the last song is never hidden underneath the now-opaque mini
            // player + nav bar, without reserving more space than needed.
            // The 68 dp miniplayer is compensated by 2 dp at each lazy-space
            // edge, restoring the prior card/scrollbar visual positions.
            contentPadding = PaddingValues(bottom = bottomContentPadding + 4.dp, end = 28.dp)
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
                    onOpenDetails = onOpenDetails,
                    // Reorder glide on sort/search changes (Metrolist pattern:
                    // stable keys + contentType above, animateItem on the row).
                    // Fades stay null: newly-composed rows must not spend
                    // their first frames on alpha layers during fast flings.
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = MiniMusicMotion.defaultSpatial(),
                        fadeOutSpec = null
                    )
                )
            }
        }

        // Reads scroll position inside its own subtree: scrubbing the list no
        // longer recomposes every row on each visible-index change. The landing
        // window's art is warmed before each jump (same 96px key the rows use).
        SongsScrollbarOverlay(
            listState = listState,
            itemCount = songs.size,
            letterForIndex = letterForIndex,
            bottomContentPadding = bottomContentPadding,
            artUriAt = { songs.getOrNull(it)?.albumArtUri }
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
    val context = LocalContext.current

    // Same front-load contract as Songs: grid cells decode at 512px, so warm
    // the opening window at exactly that key on first mount.
    LaunchedEffect(albums) {
        albums.asSequence()
            .mapNotNull { it.albumArtUri }
            .distinct()
            .take(80)
            .forEach { artworkUri ->
                MiniMusicImageLoader.get(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(artworkUri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .size(GridArtSizePx)
                        .build()
                )
            }
    }

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

        AlbumsScrollbarOverlay(
            gridState = gridState,
            itemCount = albums.size,
            letterForIndex = letterForIndex,
            bottomContentPadding = bottomContentPadding,
            artUriAt = { albums.getOrNull(it)?.albumArtUri }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    onArtistClick: (Artist) -> Unit
) {
    val listState = rememberLazyListState(cacheWindow = ListPrefetchWindow)
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
                ArtistListItem(
                    artist = artist,
                    onClick = { onArtistClick(artist) },
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        placementSpec = MiniMusicMotion.defaultSpatial(),
                        fadeOutSpec = null
                    )
                )
            }
        }

        ArtistsScrollbarOverlay(
            listState = listState,
            itemCount = artists.size,
            letterForIndex = letterForIndex,
            bottomContentPadding = bottomContentPadding
        )
    }
}

/**
 * Extra composition window ahead of/behind the viewport, in dp. This is the
 * Compose equivalent of RecyclerView's GapWorker prefetch (the reason the
 * View-based queue drawer never hitches): rows ahead of a fling are composed
 * and their Coil decodes enqueued before they enter the viewport, instead of
 * just-in-time on arrival. ~70dp rows: ~11 ahead, ~6 behind.
 */
@OptIn(ExperimentalFoundationApi::class)
private val ListPrefetchWindow = LazyLayoutCacheWindow(ahead = 800.dp, behind = 400.dp)

/** Beyond this row distance, locate jumps instead of animating the whole flight. */
private const val LocateAnimateThreshold = 40

/**
 * Scroll so the target row sits in the MIDDLE of the viewport — the same
 * centering the queue drawer uses. scrollToItem only takes positive offsets
 * (item top at-or-above the viewport start), so centering is expressed as
 * [rowsAbove] whole rows plus a remainder offset: scroll to the row that
 * many rows earlier, with the remainder pushing the target down into place.
 * Clamped so rows near the list start stay flush (no invented blank space);
 * the end of the list clamps itself.
 */
private suspend fun locateCentered(
    listState: androidx.compose.foundation.lazy.LazyListState,
    index: Int,
    itemCount: Int
) {
    val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
    val rows = listState.layoutInfo.visibleItemsInfo
    val rowPx = rows.firstOrNull()?.size ?: rows.lastOrNull()?.size ?: 0
    if (viewport <= 0 || rowPx <= 0) {
        listState.animateScrollToItem(index = index)
        return
    }
    // ~58% of the way down the viewport: the active row sits just below
    // center with one more row of upcoming content visible above it, per
    // the reference screenshots.
    val centered = (((viewport - rowPx) * 0.58f)).toInt().coerceAtLeast(0)
    // Never ask for more rows above than exist.
    val rowsAbove = (centered / rowPx).coerceAtMost(index)
    val anchor = index - rowsAbove
    listState.animateScrollToItem(index = anchor, scrollOffset = rowsAbove * rowPx)
}
/** Rows covered by the closing glide after a long-distance locate jump. */
private const val LocateGlideTail = 20
/** Artwork warmup radius around a locate target. */
private const val ArtPreloadRadius = 16
/** Artwork warmup radius around a scrollbar landing (fires per pointer event). */
private const val ScrubPreloadRadius = 12
/** Must match the row/grid art request sizes or the warmup misses the cache. */
private const val RowArtSizePx = 96
private const val GridArtSizePx = 512

/**
 * Enqueues Coil memory-cache warmups for the artwork window around [center]
 * before a programmatic jump lands there. Scrubbing and long-distance locate
 * otherwise cold-decode a full window of MediaStore thumbnails on arrival,
 * which is the stutter users feel as "loading lag" — the scrollbar thumb
 * itself stays smooth while the rows hitch behind it.
 */
private fun preloadArtWindow(
    context: Context,
    itemCount: Int,
    center: Int,
    radius: Int,
    sizePx: Int,
    uriAt: (Int) -> Uri?
) {
    if (itemCount <= 0) return
    val from = (center - radius).coerceAtLeast(0)
    val to = (center + radius).coerceAtMost(itemCount - 1)
    if (from > to) return
    // Distinct and bounded: scrub gestures fire this per pointer event, so the
    // enqueue burst stays small even mid-fling.
    val seen = HashSet<Uri>(radius * 2 + 1)
    for (i in from..to) {
        val uri = uriAt(i) ?: continue
        if (!seen.add(uri)) continue
        MiniMusicImageLoader.get(context).enqueue(
            ImageRequest.Builder(context)
                .data(uri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .size(sizePx)
                .build()
        )
    }
}

/**
 * Scrollbar overlays read the list/grid scroll position inside their own
 * subtree. Scrubbing the list therefore recomposes only the thumb and its
 * letter bubble — never the rows — which keeps fast flings smooth even in
 * very large libraries.
 */
@Composable
private fun BoxScope.SongsScrollbarOverlay(
    listState: LazyListState,
    itemCount: Int,
    letterForIndex: (Int) -> Char?,
    bottomContentPadding: Dp,
    artUriAt: (Int) -> Uri? = { null }
) {
    val scrollScope = rememberCoroutineScope()
    val context = LocalContext.current
    var fastScrollJob by remember { mutableStateOf<Job?>(null) }
    AlphabetScrollbar(
        itemCount = itemCount,
        currentIndex = listState.firstVisibleItemIndex,
        letterForIndex = letterForIndex,
        onScrollToIndex = { index ->
            // Do not queue one jump per pointer event. Only the newest
            // target is relevant while the finger is on the scrollbar.
            fastScrollJob?.cancel()
            fastScrollJob = scrollScope.launch {
                // Warm the landing window before jumping: scrubbing through
                // unseen territory otherwise cold-decodes a full window of
                // MediaStore thumbnails per pointer event.
                preloadArtWindow(context, itemCount, index, ScrubPreloadRadius, RowArtSizePx, artUriAt)
                listState.scrollToItem(index = index, scrollOffset = 0)
            }
        },
        // The top remains aligned with the first song container. The bottom
        // Keep the same 3dp visual inset at both ends: the top is 8dp
        // versus the first card's 5dp, so the bottom is shortened by 3dp.
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = bottomContentPadding + 12.dp)
    )
}

@Composable
private fun BoxScope.AlbumsScrollbarOverlay(
    gridState: LazyGridState,
    itemCount: Int,
    letterForIndex: (Int) -> Char?,
    bottomContentPadding: Dp,
    artUriAt: (Int) -> Uri? = { null }
) {
    val scrollScope = rememberCoroutineScope()
    val context = LocalContext.current
    var fastScrollJob by remember { mutableStateOf<Job?>(null) }
    AlphabetScrollbar(
        itemCount = itemCount,
        currentIndex = gridState.firstVisibleItemIndex,
        letterForIndex = letterForIndex,
        onScrollToIndex = { index ->
            fastScrollJob?.cancel()
            fastScrollJob = scrollScope.launch {
                preloadArtWindow(context, itemCount, index, ScrubPreloadRadius, GridArtSizePx, artUriAt)
                gridState.scrollToItem(index = index, scrollOffset = 0)
            }
        },
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(top = 12.dp, bottom = bottomContentPadding + 12.dp)
    )
}

@Composable
private fun BoxScope.ArtistsScrollbarOverlay(
    listState: LazyListState,
    itemCount: Int,
    letterForIndex: (Int) -> Char?,
    bottomContentPadding: Dp
) {
    val scrollScope = rememberCoroutineScope()
    var fastScrollJob by remember { mutableStateOf<Job?>(null) }
    AlphabetScrollbar(
        itemCount = itemCount,
        currentIndex = listState.firstVisibleItemIndex,
        letterForIndex = letterForIndex,
        onScrollToIndex = { index ->
            fastScrollJob?.cancel()
            fastScrollJob = scrollScope.launch {
                listState.scrollToItem(index = index, scrollOffset = 0)
            }
        },
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = bottomContentPadding)
    )
}

/**
 * The Songs / Artists / Albums switcher.
 *
 * All three destinations are on screen at once, and the selected one expands
 * to carry its label while the other two sit as icons — so the width of a leg
 * *is* the selection state, nothing is hidden behind a cycle, and every option
 * is one tap away from wherever you are. The previous control showed two of
 * three slots and only ever cycled forward, which meant Albums did not exist
 * until you tapped once and going back a step cost two taps.
 *
 * The legs animate by **weight** rather than by measured dp. A Row divides its
 * width by weight, so the three legs always add up to the row exactly — no
 * frame can overshoot the container the way independently animated widths can
 * when two legs shrink as one grows.
 *
 * Reuses the token file's default spatial spring, so a tap moves the fill and
 * the two labels on the same clock as every other component in the app.
 */
private val SwitcherCollapsedLegWidth = 48.dp

@Composable
private fun ExpandingCategoryControl(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.height(48.dp)
    ) {
        // The split is measured, not fixed. The two collapsed legs are pinned
        // at 48dp — M3's minimum touch target — and the selected leg takes
        // every remaining dp, which is what makes the control read as one bar
        // filling itself rather than three buttons of drifting size. A fixed
        // ratio could not promise that: the same 3.3 weight that gives 48/162/
        // 48 on a 412dp phone squeezes the collapsed legs to 39dp on a 360dp
        // one, under the target M3 asks for.
        BoxWithConstraints {
            val expandedWeight = ((maxWidth - 8.dp) / SwitcherCollapsedLegWidth - 2f)
                .coerceAtLeast(1f)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(LibraryTab.SONGS, LibraryTab.ARTISTS, LibraryTab.ALBUMS)
                tabs.forEachIndexed { index, tab ->
                    val active = tab == selected
                    val weight by animateFloatAsState(
                        targetValue = if (active) expandedWeight else 1f,
                        animationSpec = MiniMusicMotion.defaultSpatial(),
                        label = "switcherLegWeight"
                    )
                    val legShape = RoundedCornerShape(50)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(legShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent
                            )
                            .clickable {
                                if (!active) onSelect(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // The icon is the same in both states. It used to
                            // swap to a tick on the selected leg, which made
                            // the segment change identity mid-animation — the
                            // tab's mark disappeared exactly while the eye was
                            // following it — and the fill plus the label are
                            // already unambiguous on their own.
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (active) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            // The label belongs to the expanded leg only. It
                            // fades rather than popping, and the leg clips its
                            // own bounds, so a shrinking leg never spills text
                            // over its neighbour mid-animation.
                            AnimatedVisibility(
                                visible = active,
                                enter = fadeIn(MiniMusicMotion.fastEffects()),
                                exit = fadeOut(MiniMusicMotion.fastEffects())
                            ) {
                                Text(
                                    text = tab.label,
                                    style = MiniMusicType.compactLabel,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                        // Hairline separator between legs, hidden wherever it
                        // would touch the selected segment's filled shape.
                        if (index > 0 && !active && tabs[index - 1] != selected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(Dp.Hairline)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Height shared by the segments of the action pill, matched to the switcher
 *  beside it in the same row so the two controls share a baseline. */
private val PillButtonHeight = 48.dp

/** Width of a single action-pill segment. Two of these plus the hairline
 *  between them is the whole group, and 48dp keeps each a full touch target. */
private val ControlSegmentWidth = 48.dp

/**
 * A single segment of the Locate/Shuffle pill — segments sit in a row with a
 * hairline gap between them and per-segment corner shapes (see
 * [PillGroupShapes]) so the group reads as one continuous pill silhouette,
 * not a row of fully separate buttons and not one pill with divider lines
 * drawn inside it.
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
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/** Corner shapes for a group of [PillButton]s meant to read as one continuous
 *  pill split into segments — full stadium radius on the outer side of each
 *  end segment (a corner size larger than the pill's own height clamps to a
 *  perfect half-circle). Where two segments meet, a small (not zero) radius on
 *  both facing corners gives the soft inward curve the design calls for — a
 *  hard square edge there read as visually disconnected rather than like two
 *  pieces of one pill. */
private object PillGroupShapes {
    private val Full = 50.dp
    private val Meeting = 5.dp
    val First = RoundedCornerShape(topStart = Full, topEnd = Meeting, bottomEnd = Meeting, bottomStart = Full)
    val Last = RoundedCornerShape(topStart = Meeting, topEnd = Full, bottomEnd = Full, bottomStart = Meeting)
}

private enum class SortField {
    NAME, ARTIST, ALBUM, DURATION, DATE_ADDED,
    /** Row count — used by the artists and albums menus. */
    SONGS,
    /** Album count — artists only. */
    ALBUMS
}

private fun sortFieldOf(order: SongSortOrder): SortField = when (order) {
    SongSortOrder.NAME_A_Z, SongSortOrder.NAME_Z_A -> SortField.NAME
    SongSortOrder.ARTIST_A_Z, SongSortOrder.ARTIST_Z_A -> SortField.ARTIST
    SongSortOrder.ALBUM_A_Z, SongSortOrder.ALBUM_Z_A -> SortField.ALBUM
    SongSortOrder.DURATION_SHORTEST, SongSortOrder.DURATION_LONGEST -> SortField.DURATION
    SongSortOrder.DATE_ADDED_NEWEST, SongSortOrder.DATE_ADDED_OLDEST -> SortField.DATE_ADDED
    // The count fields belong to the artists and albums menus, and a
    // SongSortOrder cannot express one, so no branch here can ever carry a
    // count. `else` rather than enumerating the impossible: this `when` is
    // subject-typed to SongSortOrder, so a SortField branch would be read as an
    // equality test between two different enums.
    else -> SortField.NAME
}

private fun sortOrderOf(field: SortField, ascending: Boolean): SongSortOrder = when (field) {
    SortField.NAME -> if (ascending) SongSortOrder.NAME_A_Z else SongSortOrder.NAME_Z_A
    SortField.ARTIST -> if (ascending) SongSortOrder.ARTIST_A_Z else SongSortOrder.ARTIST_Z_A
    SortField.ALBUM -> if (ascending) SongSortOrder.ALBUM_A_Z else SongSortOrder.ALBUM_Z_A
    SortField.DURATION -> if (ascending) SongSortOrder.DURATION_SHORTEST else SongSortOrder.DURATION_LONGEST
    SortField.DATE_ADDED -> if (ascending) SongSortOrder.DATE_ADDED_OLDEST else SongSortOrder.DATE_ADDED_NEWEST
    // Unreachable from the songs menu (see sortFieldOf); a song list has no
    // row count of its own to sort by, so these fall back to title order.
    SortField.SONGS, SortField.ALBUMS ->
        if (ascending) SongSortOrder.NAME_A_Z else SongSortOrder.NAME_Z_A
}

/**
 * The sort sheet, for whichever of the three tabs is showing.
 *
 * Each tab gets the fields its rows can actually be judged by: a song by its
 * title, artist, album, date added or length; an **artist** by name or by the
 * two counts its row already prints; an **album** by title, artist or song
 * count. One menu serves all three so the control behaves identically wherever
 * it is opened, and the direction pill keeps its meaning (ascending = A-first
 * for text, smallest-first for counts).
 */
@Composable
private fun CategorySortMenu(
    expanded: Boolean,
    tab: LibraryTab,
    songSortOrder: SongSortOrder,
    artistSortOrder: ArtistSortOrder,
    albumSortOrder: AlbumSortOrder,
    onDismiss: () -> Unit,
    onSongSort: (SongSortOrder) -> Unit,
    onArtistSort: (ArtistSortOrder) -> Unit,
    onAlbumSort: (AlbumSortOrder) -> Unit
) {
    if (!expanded) return
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current

    // Which row is highlighted, per tab.
    val selectedField: SortField = when (tab) {
        LibraryTab.SONGS -> sortFieldOf(songSortOrder)
        LibraryTab.ARTISTS -> when (artistSortOrder) {
            ArtistSortOrder.NAME_A_Z, ArtistSortOrder.NAME_Z_A -> SortField.NAME
            ArtistSortOrder.SONGS_MOST, ArtistSortOrder.SONGS_FEWEST -> SortField.SONGS
            ArtistSortOrder.ALBUMS_MOST, ArtistSortOrder.ALBUMS_FEWEST -> SortField.ALBUMS
        }
        LibraryTab.ALBUMS -> when (albumSortOrder) {
            AlbumSortOrder.TITLE_A_Z, AlbumSortOrder.TITLE_Z_A -> SortField.NAME
            AlbumSortOrder.ARTIST_A_Z, AlbumSortOrder.ARTIST_Z_A -> SortField.ARTIST
            AlbumSortOrder.SONGS_MOST, AlbumSortOrder.SONGS_FEWEST -> SortField.SONGS
        }
    }
    val ascending = when (tab) {
        LibraryTab.SONGS -> songSortOrder in setOf(
            SongSortOrder.NAME_A_Z,
            SongSortOrder.ARTIST_A_Z,
            SongSortOrder.ALBUM_A_Z,
            SongSortOrder.DURATION_SHORTEST,
            SongSortOrder.DATE_ADDED_OLDEST
        )
        LibraryTab.ARTISTS -> artistSortOrder in setOf(
            ArtistSortOrder.NAME_A_Z,
            ArtistSortOrder.SONGS_FEWEST,
            ArtistSortOrder.ALBUMS_FEWEST
        )
        LibraryTab.ALBUMS -> albumSortOrder in setOf(
            AlbumSortOrder.TITLE_A_Z,
            AlbumSortOrder.ARTIST_A_Z,
            AlbumSortOrder.SONGS_FEWEST
        )
    }

    fun apply(field: SortField, asc: Boolean) {
        if (hapticsEnabled) hapticView.performMiniMusicHaptic()
        when (tab) {
            LibraryTab.SONGS -> onSongSort(sortOrderOf(field, asc))
            LibraryTab.ARTISTS -> onArtistSort(artistSortOrderOf(field, asc))
            LibraryTab.ALBUMS -> onAlbumSort(albumSortOrderOf(field, asc))
        }
    }

    val fields = when (tab) {
        LibraryTab.SONGS -> listOf(
            SortField.NAME to "Title",
            SortField.ARTIST to "Artist",
            SortField.ALBUM to "Album",
            SortField.DATE_ADDED to "Date added",
            SortField.DURATION to "Duration"
        )
        LibraryTab.ARTISTS -> listOf(
            SortField.NAME to "Name",
            SortField.SONGS to "Songs",
            SortField.ALBUMS to "Albums"
        )
        LibraryTab.ALBUMS -> listOf(
            SortField.NAME to "Title",
            SortField.ARTIST to "Artist",
            SortField.SONGS to "Songs"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                when (tab) {
                    LibraryTab.SONGS -> "Sort songs"
                    LibraryTab.ARTISTS -> "Sort artists"
                    LibraryTab.ALBUMS -> "Sort albums"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortDirectionPill(
                    ascending = ascending,
                    onAscending = { apply(selectedField, true) },
                    onDescending = { apply(selectedField, false) }
                )
                fields.forEach { (field, label) ->
                    val selectedFieldRow = selectedField == field
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { apply(field, ascending) },
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
                                onClick = { apply(field, ascending) }
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

/** Artists share fields with songs where the names line up; counts run "most" first when descending. */
private fun artistSortOrderOf(field: SortField, ascending: Boolean): ArtistSortOrder = when (field) {
    SortField.NAME -> if (ascending) ArtistSortOrder.NAME_A_Z else ArtistSortOrder.NAME_Z_A
    SortField.SONGS -> if (ascending) ArtistSortOrder.SONGS_FEWEST else ArtistSortOrder.SONGS_MOST
    SortField.ALBUMS -> if (ascending) ArtistSortOrder.ALBUMS_FEWEST else ArtistSortOrder.ALBUMS_MOST
    // Fields an artist cannot be sorted by; unreachable from the artists menu.
    SortField.ARTIST, SortField.ALBUM, SortField.DURATION, SortField.DATE_ADDED ->
        if (ascending) ArtistSortOrder.NAME_A_Z else ArtistSortOrder.NAME_Z_A
}

private fun albumSortOrderOf(field: SortField, ascending: Boolean): AlbumSortOrder = when (field) {
    SortField.NAME -> if (ascending) AlbumSortOrder.TITLE_A_Z else AlbumSortOrder.TITLE_Z_A
    SortField.ARTIST -> if (ascending) AlbumSortOrder.ARTIST_A_Z else AlbumSortOrder.ARTIST_Z_A
    SortField.SONGS -> if (ascending) AlbumSortOrder.SONGS_FEWEST else AlbumSortOrder.SONGS_MOST
    // Fields an album cannot be sorted by; unreachable from the albums menu.
    SortField.ALBUM, SortField.DURATION, SortField.DATE_ADDED, SortField.ALBUMS ->
        if (ascending) AlbumSortOrder.TITLE_A_Z else AlbumSortOrder.TITLE_Z_A
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
                // Match the Title/Artist/Album/Date added/Duration rows exactly.
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * The content area's four mutually exclusive states. A skeleton is shown only
 * for content that has never arrived; anything that has been on screen keeps it.
 */
private enum class LibraryContentState { Loading, Error, Empty, Ready }

/**
 * Skeleton loaders for the library's three tabs, drawn in the shape of the real
 * rows so the layout is already correct before the scan lands — the guide's
 * "stable layouts" characteristic, and the reason the spinner (which said
 * nothing about what was coming) is gone.
 *
 * One shared pulse drives every placeholder, so the sweep reads as a single
 * sheet of light travelling down and to the right rather than as independent
 * blinking rows.
 */
@Composable
private fun LibrarySkeleton(
    tab: LibraryTab,
    bottomContentPadding: androidx.compose.ui.unit.Dp
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (tab) {
            LibraryTab.SONGS -> SongListSkeleton()
            LibraryTab.ARTISTS -> ArtistListSkeleton()
            LibraryTab.ALBUMS -> AlbumGridSkeleton()
        }
        // Same reserved space the real lists leave, so the mini player never
        // sits on top of a placeholder that the content will not sit under.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomContentPadding)
        )
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
