package com.example.minimusic.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import com.example.minimusic.ui.screens.DetailsScreen
import com.example.minimusic.ui.screens.FilteredSongsScreen
import com.example.minimusic.ui.screens.LibraryScreen
import com.example.minimusic.ui.theme.LocalMiniMusicReducedMotion
import com.example.minimusic.ui.theme.MiniMusicMotion
import com.example.minimusic.ui.screens.LyricsScreen
import com.example.minimusic.ui.screens.PlayerScreen
import com.example.minimusic.ui.components.MiniPlayer
import com.example.minimusic.ui.components.MiniPlayerReservedHeight
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.screens.SettingsScreen
import com.example.minimusic.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import com.example.minimusic.ui.viewmodel.PlayerViewModel
import com.example.minimusic.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private object Routes {
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val LYRICS = "lyrics"
    const val DETAILS = "details/{songId}"
    const val SETTINGS = "settings"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistName}"
    fun album(albumId: Long) = "album/$albumId"
    fun artist(artistName: String) = "artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}"
    fun details(songId: Long) = "details/$songId"
}

@Composable
fun MiniMusicNavGraph(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    settingsViewModel: SettingsViewModel,
    openPlayerFromWidget: Boolean = false,
    navController: NavHostController = rememberNavController()
) {
    val libraryState by libraryViewModel.uiState.collectAsState()
    // Sliced so the 20 Hz position ticker never recomposes this graph: only an
    // actual track change flows down. Player/MiniPlayer/Lyrics collect the full
    // playback flow themselves, scoped to their own subtrees.
    val currentSong by remember(playerViewModel) {
        playerViewModel.uiState.map { it.currentSong }.distinctUntilChanged()
    }.collectAsState(initial = null)

    val queueSnapshot by playerViewModel.queueSnapshot.collectAsState()
    val lyricsState by playerViewModel.lyricsState.collectAsState()
    val appSettings by settingsViewModel.settings.collectAsState()
    val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()

    LaunchedEffect(libraryState.allSongs, appSettings.resumeOnLaunch) {
        // Resume-on-launch OFF means a genuinely fresh start: no restored queue,
        // no restored track — the library opens empty of playback state.
        if (appSettings.resumeOnLaunch && libraryState.allSongs.isNotEmpty()) {
            playerViewModel.restoreLastSession(
                libraryState.allSongs,
                playOnLaunch = true
            )
        }
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    // The system predictive-back gesture is left to the platform: the custom
    // handler only intercepted the gesture to drive a transform that then
    // fought the NavHost transition (the "back feels broken" bug). With no
    // interception, the system's own predictive animation plays and the
    // pop runs the standard backEnter/backExit slides.
    // Every transition below branches on this: with reduced motion on, the
    // guide asks for fades instead of the sliding, so the recipes collapse to
    // their fade component and keep the same timing.
    val reducedMotion = LocalMiniMusicReducedMotion.current

    fun openPlayer() {
        if (navController.currentDestination?.route != Routes.PLAYER) {
            navController.navigate(Routes.PLAYER) {
                launchSingleTop = true
            }
        }
    }

    BoxWithConstraints(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val sheetState = rememberPlayerSheetMotionState(scope)
        var queueDrawerOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val fullHeightPx = with(density) { maxHeight.toPx() }
        val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
        val miniPlayerHeightPx = with(density) { MiniPlayerReservedHeight.toPx() } + navigationBarHeightPx
        val isLandscape = maxWidth > maxHeight && maxHeight >= 320.dp
        val hasActiveSong = currentSong != null
        val collapsedOffsetPx = (fullHeightPx - miniPlayerHeightPx).coerceAtLeast(0f)
        val sheetDragState = rememberDraggableState { delta -> sheetState.dragBy(delta) }
        val sheetDragModifier = androidx.compose.ui.Modifier.draggable(
            orientation = Orientation.Vertical,
            state = sheetDragState,
            startDragImmediately = false,
            onDragStopped = { velocity ->
                sheetState.settle(
                    velocityPxPerSecond = velocity,
                    onExpanded = { if (currentRoute != Routes.PLAYER) openPlayer() },
                    onCollapsed = {},
                    onCollapseStarted = {
                        if (currentRoute == Routes.PLAYER) navController.popBackStack()
                    }
                )
            }
        )

        fun openPlayerWithSheetTransition() {
            sheetState.settle(
                velocityPxPerSecond = 0f,
                targetProgressOverride = 1f,
                onExpanded = { if (currentRoute != Routes.PLAYER) openPlayer() },
                onCollapsed = {}
            )
        }

        LaunchedEffect(fullHeightPx, collapsedOffsetPx) {
            sheetState.updateBounds(
                PlayerSheetMotionBounds(
                    expandedOffsetPx = 0f,
                    collapsedOffsetPx = collapsedOffsetPx
                )
            )
        }
        LaunchedEffect(openPlayerFromWidget, hasActiveSong) {
            if (openPlayerFromWidget && hasActiveSong) {
                sheetState.settle(
                    velocityPxPerSecond = 0f,
                    targetProgressOverride = 1f,
                    onExpanded = { openPlayer() },
                    onCollapsed = {}
                )
            }
        }
        LaunchedEffect(currentRoute) {
            sheetState.settle(
                velocityPxPerSecond = 0f,
                targetProgressOverride = if (currentRoute == Routes.PLAYER || currentRoute == Routes.LYRICS) 1f else 0f,
                onExpanded = {},
                onCollapsed = {}
            )
        }

        // Keep Home composed as a stable base layer for every destination.
        // Overlay destinations can then enter/exit over the already-rendered
        // library instead of exposing a stale frame while the back stack changes.
        CompositionLocalProvider(LocalMiniMusicHaptics provides appSettings.hapticFeedback) {
        LibraryScreen(
            uiState = libraryState,
            currentSongId = currentSong?.id,
            events = libraryViewModel.events,
            onSearchQueryChange = libraryViewModel::onSearchQueryChange,
            onSortOrderChange = libraryViewModel::onSortOrderChange,
            onPlaySong = { song, queue ->
                playerViewModel.playQueue(queue, queue.indexOf(song))
            },
            onPlayNext = playerViewModel::playNext,
            onAddToQueue = playerViewModel::addToQueue,
            onShufflePlayFrom = { song, songs ->
                playerViewModel.startShufflePlayback(songs, songs.indexOf(song))
            },
            onDeleteSong = libraryViewModel::deleteSong,
            onOpenDetails = { song -> navController.navigate(Routes.details(song.id)) },
            onRetryDelete = libraryViewModel::deleteSong,
            onAlbumClick = { album -> navController.navigate(Routes.album(album.id)) },
            onArtistClick = { artist -> navController.navigate(Routes.artist(artist.name)) },
            onTogglePlayPause = playerViewModel::togglePlayPause,
            onSkipNext = playerViewModel::skipToNext,
            onSkipPrevious = playerViewModel::skipToPrevious,
            onOpenPlayer = ::openPlayer,
            onOpenSettings = {
                navController.navigate(Routes.SETTINGS) {
                    // launchSingleTop: tapping the settings gear while Settings
                    // is open must never stack another copy on top.
                    launchSingleTop = true
                }
            },
            onRetryLoad = libraryViewModel::loadLibrary
        )
        }

        NavHost(
            navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .then(
                if (currentRoute == Routes.ALBUM ||
                    currentRoute == Routes.ARTIST
                ) {
                    androidx.compose.ui.Modifier.background(MaterialTheme.colorScheme.background)
                } else {
                    androidx.compose.ui.Modifier
                }
            ),
        // Standard NavHost transitions only — the custom predictive-back
        // graphicsLayer transform that used to sit here drove scale+translate
        // on every frame WHILE the route transition also animated, and the
        // two fighting is what made back feel broken. NextPlayer and the
        // material.io guidance both rely on the transition alone.
        enterTransition = { pushEnter(reducedMotion) },
        exitTransition = { pushExit(reducedMotion) },
        popEnterTransition = { backEnter(reducedMotion) },
        popExitTransition = { backExit(reducedMotion) }
    ) {

        composable(Routes.LIBRARY) {
            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize())
        }

        composable(
            route = Routes.SETTINGS
        ) {
            CompositionLocalProvider(LocalMiniMusicHaptics provides appSettings.hapticFeedback) {
            SettingsScreen(
                settings = appSettings,
                libraryState = libraryState,
                appVersion = settingsViewModel.appVersion,
                onBack = { navController.popBackStack() },
                onDynamicColorChange = settingsViewModel::setDynamicColorEnabled,
                onThemeModeChange = settingsViewModel::setThemeMode,
                onAlbumArtPaletteStyleChange = settingsViewModel::setAlbumArtPaletteStyle,
                onAmoledBlackModeChange = settingsViewModel::setAmoledBlackMode,
                onResumeOnLaunchChange = settingsViewModel::setResumeOnLaunch,
                onShowAudioQualityBadgeChange = settingsViewModel::setShowAudioQualityBadge,
                onCenteredTitleChange = settingsViewModel::setCenteredTitle,
                onPlayerArtworkShadowEnabledChange = settingsViewModel::setPlayerArtworkShadowEnabled,
                onPlayerArtworkShadowDpChange = settingsViewModel::setPlayerArtworkShadowDp,
                onStopOnDismissChange = settingsViewModel::setStopOnDismiss,
                onHapticFeedbackChange = settingsViewModel::setHapticFeedback,
                onCrossfadeEnabledChange = settingsViewModel::setCrossfadeEnabled,
                onCrossfadeSecondsChange = settingsViewModel::setCrossfadeSeconds,
                onMonoAudioChange = settingsViewModel::setMonoAudio,
                onMinDurationChange = settingsViewModel::setMinDurationSeconds,
                onRescanLibrary = { libraryViewModel.rescanLibrary() }
            )
            }
        }

        composable(
            route = Routes.ALBUM,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            val songs = libraryViewModel.songsForAlbum(albumId)
            FilteredSongsScreen(
                title = songs.firstOrNull()?.album ?: "Album",
                songs = songs,
                currentSongId = currentSong?.id,
                onBack = { navController.popBackStack() },
                onPlaySong = { song -> playerViewModel.playQueue(songs, songs.indexOf(song)) },
                onOpenDetails = { song -> navController.navigate(Routes.details(song.id)) },
                onPlayAll = { playerViewModel.playQueue(songs, 0) },
                onShuffleAll = {
                    if (songs.isNotEmpty()) {
                        playerViewModel.startShufflePlayback(songs, songs.indices.random())
                    }
                },
                headerArtUri = songs.firstOrNull()?.albumArtUri,
                headerSubtitle = songs.firstOrNull()?.artist
            )
        }

        composable(
            route = Routes.ARTIST,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("artistName") ?: return@composable
            val artistName = java.net.URLDecoder.decode(encodedName, "UTF-8")
            val songs = libraryViewModel.songsForArtist(artistName)
            FilteredSongsScreen(
                title = artistName,
                songs = songs,
                currentSongId = currentSong?.id,
                onBack = { navController.popBackStack() },
                onPlaySong = { song -> playerViewModel.playQueue(songs, songs.indexOf(song)) },
                onOpenDetails = { song -> navController.navigate(Routes.details(song.id)) },
                onPlayAll = { playerViewModel.playQueue(songs, 0) },
                onShuffleAll = {
                    if (songs.isNotEmpty()) {
                        playerViewModel.startShufflePlayback(songs, songs.indices.random())
                    }
                }
            )
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("songId") { type = NavType.LongType }),
            // No route motion: the details surface is a dialog that owns one
            // symmetric open/close animation. Any tween here would double-drive
            // it (the same reason the lyrics route overrides these).
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getLong("songId") ?: return@composable
            val song = libraryViewModel.songById(songId) ?: return@composable
            DetailsScreen(song = song, onBack = { navController.popBackStack() })
        }

        composable(Routes.PLAYER) {
            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize())
        }

        composable(
            Routes.LYRICS,
            // No route motion: LyricsScreen owns its single vertical
            // open/close animation. Any NavHost tween here would double-drive
            // the card (the old fall-stick-slide-left bug).
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            if (isLandscape) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterEnd)
                        .zIndex(4f)
                ) {
                    LyricsScreen(
                        playbackFlow = playerViewModel.uiState,
                        lyricsState = lyricsState,
                        onSeekTo = playerViewModel::seekTo,
                        albumArtPaletteStyle = appSettings.albumArtPaletteStyle,
                        // Back always lands on the player card: pop to PLAYER
                        // when it's in the stack (tap/drag-open path), else
                        // rebuild it above Home (drag-open path has no PLAYER
                        // entry, and a failed pop would strand the back press).
                        onBack = {
                            val popped =
                                navController.popBackStack(Routes.PLAYER, inclusive = false)
                            if (!popped) {
                                navController.navigate(Routes.PLAYER) {
                                    popUpTo(Routes.LIBRARY)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            } else {
                LyricsScreen(
                    playbackFlow = playerViewModel.uiState,
                    lyricsState = lyricsState,
                    onSeekTo = playerViewModel::seekTo,
                    albumArtPaletteStyle = appSettings.albumArtPaletteStyle,
                    onBack = {
                        val popped =
                            navController.popBackStack(Routes.PLAYER, inclusive = false)
                        if (!popped) {
                            navController.navigate(Routes.PLAYER) {
                                popUpTo(Routes.LIBRARY)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    }


        // Read the sheet progress through derived state: drag/settle frames must
        // not recompose this whole graph (library list included) — only the
        // boolean handoffs below may trigger composition. The translationY
        // itself stays in graphicsLayer (layout phase, no recomposition).
        val playerSheetVisible by remember(sheetState, hasActiveSong, currentRoute) {
            derivedStateOf {
                hasActiveSong &&
                    (currentRoute == Routes.PLAYER || currentRoute == Routes.LYRICS ||
                        sheetState.progress > 0.001f)
            }
        }
        val playerSheetInteractive by remember(sheetState, currentRoute, queueDrawerOpen) {
            derivedStateOf {
                currentRoute != Routes.LYRICS && sheetState.progress > 0.001f && !queueDrawerOpen
            }
        }
        val miniPlayerVisible by remember(sheetState, currentRoute) {
            derivedStateOf {
                currentRoute != Routes.LYRICS &&
                    (currentRoute != Routes.PLAYER || sheetState.progress < 0.999f)
            }
        }
        // Back at Home with the sheet dragged up collapses the sheet instead
        // of exiting the app: every back press must close the current layer
        // and reveal the previous one (lyrics -> player -> home -> exit).
        val sheetExpandedAtHome by remember(sheetState, currentRoute) {
            derivedStateOf {
                currentRoute == Routes.LIBRARY && sheetState.progress > 0.5f
            }
        }
        androidx.activity.compose.BackHandler(enabled = sheetExpandedAtHome) {
            scope.launch {
                sheetState.settle(
                    velocityPxPerSecond = 0f,
                    targetProgressOverride = 0f,
                    onExpanded = {},
                    onCollapsed = {}
                )
            }
        }

        if (playerSheetVisible) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (1f - sheetState.progress) * fullHeightPx
                        // The player surface remains opaque while moving; its own
                        // album-art-derived background is left unchanged.
                        alpha = 1f
                    }
                    // Keep the player surface beneath Lyrics, but above the stable Home base.
                    .zIndex(2f)
                    .then(
                        if (playerSheetInteractive) {
                            sheetDragModifier
                        } else {
                            androidx.compose.ui.Modifier
                        }
                    )
            ) {
                CompositionLocalProvider(LocalMiniMusicHaptics provides appSettings.hapticFeedback) {
                PlayerScreen(
                    playbackFlow = playerViewModel.uiState,
                    queueSnapshot = queueSnapshot,
                    showAudioQualityBadge = appSettings.showAudioQualityBadge,
                    centeredTitle = appSettings.centeredTitle,
                    albumArtPaletteStyle = appSettings.albumArtPaletteStyle,
                    artworkShadowEnabled = appSettings.playerArtworkShadowEnabled,
                    artworkShadowDp = appSettings.playerArtworkShadowDp,
                    sleepTimerState = sleepTimerState,
                    onBack = { navController.popBackStack() },
                    onSwipeToMiniplayer = {},
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onSkipNext = playerViewModel::skipToNext,
                    onSkipPrevious = playerViewModel::skipToPrevious,
                    onSeekTo = playerViewModel::seekTo,
                    onToggleShuffle = playerViewModel::toggleShuffle,
                    onCycleRepeat = playerViewModel::cycleRepeatMode,
                    onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                    onQueueItemClick = playerViewModel::playFromQueue,
                    onQueueEntryClick = playerViewModel::playQueueEntry,
                    onReorderQueue = playerViewModel::moveQueueEntry,
                    onRemoveQueueEntry = playerViewModel::removeQueueEntry,
                    onClearQueue = {
                        playerViewModel.clearQueue()
                        navController.popBackStack(Routes.LIBRARY, inclusive = false)
                    },
                    onStartSleepTimer = playerViewModel::startSleepTimer,
                    onCancelSleepTimer = playerViewModel::cancelSleepTimer,
                    onQueueOpenChange = { queueDrawerOpen = it }
                )
                }
            }
        }

        if (miniPlayerVisible) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .graphicsLayer {
                        translationY = sheetState.progress * miniPlayerHeightPx
                        // Keep the miniplayer opaque until it fully hands off to
                        // the player sheet, avoiding a translucent cross-fade.
                        alpha = 1f
                    }
                    .zIndex(1f)
                    .then(if (hasActiveSong && !queueDrawerOpen) sheetDragModifier else androidx.compose.ui.Modifier)
            ) {
                CompositionLocalProvider(LocalMiniMusicHaptics provides appSettings.hapticFeedback) {
                MiniPlayer(
                    playbackFlow = playerViewModel.uiState,
                    onTogglePlayPause = playerViewModel::togglePlayPause,
                    onSkipNext = playerViewModel::skipToNext,
                    onClick = ::openPlayerWithSheetTransition,
                    onSwipeToPlayer = {},
                    albumArtPaletteStyle = appSettings.albumArtPaletteStyle
                )
                }
            }
        }
    }
}

/**
 * Forward and backward navigation, the M3 *"forward and backward"* pattern.
 *
 * The guide's rule for this pattern is to use the platform default, and
 * Android's default is *"a fade as screens slide"*: the screens travel part of
 * the width so the amount of motion stays small, and the fade carries the rest.
 * Four things follow from that, and every hierarchy edge in this graph — a
 * Settings push, an Album push, an Artist push, and their pops — now uses
 * exactly these two recipes, mirrored, instead of the previous mix of
 * full-half-width slides, a zero-travel exit hold, and a Settings-only fork
 * that made *returning from Settings* a different move from *returning from
 * Album*.
 *
 * 1. **Unified direction.** Entering and exiting elements move along one axis
 *    as a group, the incoming screen travelling further than the outgoing one
 *    drifts, so the pair reads as a single gesture rather than two independent
 *    animations.
 * 2. **Exits accelerate, entries decelerate** — [MiniMusicMotion.navExitEasing]
 *    for anything leaving, [MiniMusicMotion.navEnterEasing] for anything
 *    arriving. An exit on the decelerate curve is what makes a dismissal feel
 *    like it is being dragged rather than released.
 * 3. **A real fade on the exiting screen.** It used to run a slide with zero
 *    travel for the full duration purely to stay visible, which is a hold
 *    dressed up as an animation; the Library layer already sits beneath this
 *    graph, so the fade dissolves onto it the way the pattern intends.
 * 4. **Reduced motion collapses to the fade alone**, per the guide's first
 *    characteristic.
 */
// Nav slides use the M3 emphasized tween (standard+accelerate curves,
// 300/250ms) — NextPlayer and the material.io spec both use easing tweens
// for route transitions; springs on full-width slides either crawl their
// settle tail ("stuck") or rush. Enter uses emphasized-decelerate, exit
// emphasized-accelerate, the standard pairing.
private const val NavEnterMs = 300
private const val NavExitMs = 250

private fun navEnterSpec() = tween<IntOffset>(
    NavEnterMs, easing = MiniMusicMotion.navEnterEasing
)

private fun navExitSpec() = tween<IntOffset>(
    NavExitMs, easing = MiniMusicMotion.navExitEasing
)

private fun pushEnter(reduced: Boolean): EnterTransition {
    // No fade on enter: the incoming surface must stay fully opaque over the
    // composed Library base layer or it reads translucent (the "settings
    // opens over the library" bug). Slide only.
    return if (reduced) EnterTransition.None
    else slideInHorizontally(animationSpec = navEnterSpec()) { it / 4 }
}

private fun pushExit(reduced: Boolean): ExitTransition {
    // No fade on exit: the Library base layer is always composed underneath
    // the NavHost, so fading the outgoing surface lets it bleed through and
    // reads as a translucent overlay (the reported bug). The surface slides
    // away opaque instead; reduced motion holds it still.
    return if (reduced) ExitTransition.None
    else slideOutHorizontally(animationSpec = navExitSpec()) { -it / 6 }
}

private fun backEnter(reduced: Boolean): EnterTransition {
    // The Library layer already sits beneath the NavHost, so a returning
    // destination does not need a fade to appear — sliding it in from the
    // leading edge as an opaque surface reads as the reverse of the push.
    // A fade here is what made the back transition look translucent: the
    // incoming screen blended with the stale layer underneath.
    return if (reduced) EnterTransition.None
    else slideInHorizontally(animationSpec = navEnterSpec()) { -it / 6 }
}

private fun backExit(reduced: Boolean): ExitTransition {
    // Same rule as pushExit: opaque slide, never a fade over the live
    // Library layer underneath — the fade was the translucent-overlay bug.
    return if (reduced) ExitTransition.None
    else slideOutHorizontally(animationSpec = navExitSpec()) { it / 4 }
}
