package com.example.minimusic.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.graphics.TransformOrigin
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
        if (libraryState.allSongs.isNotEmpty()) {
            playerViewModel.restoreLastSession(
                libraryState.allSongs,
                playOnLaunch = appSettings.resumeOnLaunch
            )
        }
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val predictiveBackRoute = currentRoute == Routes.SETTINGS ||
        currentRoute == Routes.DETAILS || currentRoute == Routes.ALBUM || currentRoute == Routes.ARTIST
    val predictiveBackProgress = remember { Animatable(0f) }

    PredictiveBackHandler(enabled = predictiveBackRoute) { progress ->
        var completed = false
        try {
            progress.collect { event ->
                predictiveBackProgress.snapTo(event.progress)
            }
            completed = true
        } catch (_: CancellationException) {
            // A cancelled edge gesture returns the current destination to rest.
        } finally {
            if (completed) {
                navController.popBackStack()
                predictiveBackProgress.snapTo(0f)
            } else {
                predictiveBackProgress.animateTo(0f, animationSpec = tween(180))
            }
        }
    }

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
        val fullWidthPx = with(density) { maxWidth.toPx() }
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
            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            onRetryLoad = libraryViewModel::loadLibrary
        )
        }

        NavHost(
            navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .then(
                if (currentRoute == Routes.SETTINGS ||
                    currentRoute == Routes.DETAILS ||
                    currentRoute == Routes.ALBUM ||
                    currentRoute == Routes.ARTIST
                ) {
                    androidx.compose.ui.Modifier.background(MaterialTheme.colorScheme.background)
                } else {
                    androidx.compose.ui.Modifier
                }
            )
            .graphicsLayer {
                // Keep the previous Library layer visible underneath while
                // an approved destination follows the predictive-back edge.
                val progress = predictiveBackProgress.value
                translationX = progress * fullWidthPx * 0.18f
                scaleX = 1f - progress * 0.04f
                scaleY = 1f - progress * 0.04f
            }
            .zIndex(3f),
        enterTransition = {
            // Shared-axis push, no fade: the entering screen slides in
            // half-width over the exiting one with a slight scale-up, all on
            // the decelerate curve. Screens overlap like cards throughout.
            slideInHorizontally(
                initialOffsetX = { (it * 0.5f).toInt() },
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            ) + scaleIn(
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            )
        },
        exitTransition = {
            // Exiting screen holds its pixels (zero-travel slide) while the
            // entering screen covers it — overlap, never a fade.
            slideOutHorizontally(
                targetOffsetX = { 0 },
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navExitEasing
                )
            )
        },
        popEnterTransition = {
            if (initialState.destination.route == Routes.SETTINGS ||
                initialState.destination.route?.startsWith("details/") == true
            ) fadeIn(tween(220, easing = MiniMusicMotion.navEnterEasing)) +
                slideInHorizontally(initialOffsetX = { -(it * 0.08f).toInt() }, animationSpec = tween(220))
            else slideInHorizontally(
                initialOffsetX = { -(it * 0.25f).toInt() },
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            ) + scaleIn(
                initialScale = 0.95f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            )
        },
        popExitTransition = {
            if (initialState.destination.route == Routes.SETTINGS ||
                initialState.destination.route?.startsWith("details/") == true
            ) fadeOut(tween(180, easing = MiniMusicMotion.navExitEasing)) +
                slideOutHorizontally(targetOffsetX = { (it * 0.08f).toInt() }, animationSpec = tween(180))
            else slideOutHorizontally(
                targetOffsetX = { (it * 0.5f).toInt() },
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            ) + scaleOut(
                targetScale = 0.92f,
                transformOrigin = TransformOrigin(0.5f, 0.5f),
                animationSpec = tween(
                    MiniMusicMotion.navTransitionDurationMillis,
                    easing = MiniMusicMotion.navEnterEasing
                )
            )
        }
    ) {

        composable(Routes.LIBRARY) {
            Box(modifier = androidx.compose.ui.Modifier.fillMaxSize())
        }

        composable(Routes.SETTINGS) {
            CompositionLocalProvider(LocalMiniMusicHaptics provides appSettings.hapticFeedback) {
            SettingsScreen(
                settings = appSettings,
                libraryState = libraryState,
                appVersion = settingsViewModel.appVersion,
                onBack = { navController.popBackStack() },
                onDynamicColorChange = settingsViewModel::setDynamicColorEnabled,
                onThemeModeChange = settingsViewModel::setThemeMode,
                onAmoledBlackModeChange = settingsViewModel::setAmoledBlackMode,
                onResumeOnLaunchChange = settingsViewModel::setResumeOnLaunch,
                onShowAudioQualityBadgeChange = settingsViewModel::setShowAudioQualityBadge,
                onCenteredTitleChange = settingsViewModel::setCenteredTitle,
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
                onOpenDetails = { song -> navController.navigate(Routes.details(song.id)) }
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
                onOpenDetails = { song -> navController.navigate(Routes.details(song.id)) }
            )
        }

        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
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
                    onSwipeToPlayer = {}
                )
                }
            }
        }
    }
}
