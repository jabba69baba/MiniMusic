package com.example.minimusic.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.minimusic.ui.screens.DetailsScreen
import com.example.minimusic.ui.screens.FilteredSongsScreen
import com.example.minimusic.ui.screens.LibraryScreen
import com.example.minimusic.ui.screens.LyricsScreen
import com.example.minimusic.ui.screens.PlayerScreen
import com.example.minimusic.ui.screens.SettingsScreen
import com.example.minimusic.ui.viewmodel.LibraryViewModel
import com.example.minimusic.ui.viewmodel.PlayerViewModel
import com.example.minimusic.ui.viewmodel.SettingsViewModel

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
    navController: NavHostController = rememberNavController()
) {
    val libraryState by libraryViewModel.uiState.collectAsState()
    val playbackState by playerViewModel.uiState.collectAsState()
    val queueSnapshot by playerViewModel.queueSnapshot.collectAsState()
    val lyricsState by playerViewModel.lyricsState.collectAsState()
    val appSettings by settingsViewModel.settings.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        composable(Routes.LIBRARY) {
            LibraryScreen(
                uiState = libraryState,
                playbackState = playbackState,
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
                onOpenPlayer = {
                    navController.navigate(Routes.PLAYER) {
                        launchSingleTop = true
                    }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onRetryLoad = libraryViewModel::loadLibrary
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = appSettings,
                appVersion = settingsViewModel.appVersion,
                onBack = { navController.popBackStack() },
                onDynamicColorChange = settingsViewModel::setDynamicColorEnabled,
                onThemeModeChange = settingsViewModel::setThemeMode,
                onResumeOnLaunchChange = settingsViewModel::setResumeOnLaunch,
                onShowAudioQualityBadgeChange = settingsViewModel::setShowAudioQualityBadge,
                onMinDurationChange = settingsViewModel::setMinDurationSeconds,
                onRescanLibrary = libraryViewModel::rescanLibrary
            )
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
                currentSongId = playbackState.currentSong?.id,
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
                currentSongId = playbackState.currentSong?.id,
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

        composable(
            route = Routes.PLAYER,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing))
            }
        ) {
            val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()
            PlayerScreen(
                playbackState = playbackState,
                queueSnapshot = queueSnapshot,
                showAudioQualityBadge = appSettings.showAudioQualityBadge,
                sleepTimerState = sleepTimerState,
                onBack = { navController.popBackStack() },
                onSwipeToMiniplayer = { navController.popBackStack() },
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
                onCancelSleepTimer = playerViewModel::cancelSleepTimer
            )
        }

        composable(Routes.LYRICS) {
            LyricsScreen(
                playbackState = playbackState,
                lyricsState = lyricsState,
                onSeekTo = playerViewModel::seekTo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
