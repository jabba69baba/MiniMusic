package com.example.minimusic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    const val SETTINGS = "settings"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistName}"
    fun album(albumId: Long) = "album/$albumId"
    fun artist(artistName: String) = "artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}"
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

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {

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
                onRetryDelete = libraryViewModel::deleteSong,
                onAlbumClick = { album -> navController.navigate(Routes.album(album.id)) },
                onArtistClick = { artist -> navController.navigate(Routes.artist(artist.name)) },
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onSkipNext = playerViewModel::skipToNext,
                onSkipPrevious = playerViewModel::skipToPrevious,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
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
                onAutoShowLyricsChange = settingsViewModel::setAutoShowLyrics,
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
                onPlaySong = { song -> playerViewModel.playQueue(songs, songs.indexOf(song)) }
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
                onPlaySong = { song -> playerViewModel.playQueue(songs, songs.indexOf(song)) }
            )
        }

        composable(Routes.PLAYER) {
            val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()
            PlayerScreen(
                playbackState = playbackState,
                queueSnapshot = queueSnapshot,
                showLyricsInitially = appSettings.autoShowLyrics,
                showAudioQualityBadge = appSettings.showAudioQualityBadge,
                sleepTimerState = sleepTimerState,
                onBack = { navController.popBackStack() },
                onTogglePlayPause = playerViewModel::togglePlayPause,
                onSkipNext = playerViewModel::skipToNext,
                onSkipPrevious = playerViewModel::skipToPrevious,
                onSeekTo = playerViewModel::seekTo,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeatMode,
                onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                onQueueItemClick = playerViewModel::playFromQueue,
                onQueueEntryClick = playerViewModel::playQueueEntry,
                onMoveQueueEntry = playerViewModel::moveQueueEntry,
                onRemoveQueueEntry = playerViewModel::removeQueueEntry,
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
