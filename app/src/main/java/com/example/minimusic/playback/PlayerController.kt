package com.example.minimusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.minimusic.data.model.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single point of contact between the UI layer and the MediaController connected to
 * [MusicService]. Owns the queue of [Song]s currently loaded and mirrors ExoPlayer's
 * state into a simple [PlaybackUiState] the Compose UI can collect.
 */
class PlayerController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var connecting = false
    private var connectionGeneration = 0L
    private var currentQueue: List<Song> = emptyList()
    private val alphabeticalSongComparator = compareBy<Song> {
        it.title.trim().lowercase()
    }.thenBy { it.artist.trim().lowercase() }
        .thenBy { it.id }
    private var positionTicker: Job? = null
    private var playbackTransitionToken = 0L
    private var suppressIsPlayingUntilMs = 0L
    private var normalizingTimeline = false
    private var pendingSeekPositionMs: Long? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val attempt = ++connectionGeneration
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                try {
                    if (attempt != connectionGeneration) return@addListener
                    controller = future.get().also { it.addListener(playerListener) }
                    startPositionTicker()
                    connecting = false
                } catch (_: Exception) {
                    connecting = false
                    scope.launch {
                        delay(500L)
                        if (attempt == connectionGeneration && controller == null) connect()
                    }
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        connectionGeneration++
        connecting = false
        positionTicker?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    /** Loads [songs] as the new queue and starts playback at [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return

        val selectedId = songs.getOrNull(startIndex)?.id
        val orderedSongs = songs.sortedWith(alphabeticalSongComparator)
        val orderedStartIndex = selectedId?.let { id ->
            orderedSongs.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        } ?: 0

        currentQueue = orderedSongs
        pendingSeekPositionMs = null
        val mediaItems = orderedSongs.map { it.toMediaItem() }
        c.setMediaItems(mediaItems, orderedStartIndex, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        val shouldPlay = !c.isPlaying
        // An explicit user click always wins over a transient Media3 transition.
        suppressIsPlayingUntilMs = 0L
        _uiState.value = _uiState.value.copy(isPlaying = shouldPlay)
        if (shouldPlay) c.play() else c.pause()
    }

    fun skipToNext() {
        controller?.let {
            if (it.hasNextMediaItem()) {
                holdPlaybackStateAcrossTransition()
                it.seekToNextMediaItem()
            }
        }
    }

    fun skipToPrevious() {
        val c = controller ?: return
        // Restart the current song if we're more than 3s in, like most players do.
        if (c.currentPosition > 3000L || !c.hasPreviousMediaItem()) {
            c.seekTo(0L)
        } else {
            holdPlaybackStateAcrossTransition()
            c.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        val c = controller ?: return
        val target = positionMs.coerceAtLeast(0L)
        // Keep the UI on the user's committed position until Media3 reports a
        // nearby value; otherwise the 50ms ticker briefly paints the old
        // position and the seekbar appears to snap backward.
        pendingSeekPositionMs = target
        holdPlaybackStateAcrossTransition()
        c.seekTo(target)
    }

    fun playFromQueue(index: Int) {
        val c = controller ?: return
        val selectedSong = _uiState.value.queue.getOrNull(index) ?: return
        val timelineIndex = currentQueue.indexOfFirst { it.id == selectedSong.id }
        if (timelineIndex < 0) return

        holdPlaybackStateAcrossTransition()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        c.seekTo(timelineIndex, 0L)
        c.play()
    }

    /**
     * Moves the queue item at [fromIndex] to [toIndex], reordering both ExoPlayer's
     * live timeline (so playback order actually changes) and the local [currentQueue]
     * mirror (so the UI stays in sync). No-ops on an out-of-range index rather than
     * throwing, since drag gestures can occasionally report a stale index mid-drag.
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        if (fromIndex == toIndex) return
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return

        c.moveMediaItem(fromIndex, toIndex)

        currentQueue = currentQueue.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }
        refreshCurrentItem()
    }

    /**
     * Inserts [song] immediately after the currently playing item, so it plays
     * right after the current track without disturbing the rest of the queue.
     * If nothing is playing yet, falls back to starting a fresh single-song queue.
     */
    fun playNext(song: Song) {
        val c = controller ?: return
        if (currentQueue.isEmpty()) {
            playQueue(listOf(song), 0)
            return
        }
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, currentQueue.size)
        c.addMediaItem(insertAt, song.toMediaItem())
        currentQueue = currentQueue.toMutableList().apply { add(insertAt, song) }
        refreshCurrentItem()
    }

    /**
     * Appends [song] to the end of the current queue. If nothing is playing yet,
     * falls back to starting a fresh single-song queue.
     */
    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (currentQueue.isEmpty()) {
            playQueue(listOf(song), 0)
            return
        }
        c.addMediaItem(song.toMediaItem())
        currentQueue = currentQueue + song
        refreshCurrentItem()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        val enabled = !c.shuffleModeEnabled
        c.shuffleModeEnabled = enabled
        if (!enabled) normalizeTimelineAlphabetically(c)
        syncQueueFromController()
    }

    fun cycleRepeatMode() {
        val c = controller ?: return
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        c.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _uiState.value = _uiState.value.copy(repeatMode = next)
        syncQueueFromController()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (System.currentTimeMillis() < suppressIsPlayingUntilMs) return
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            pendingSeekPositionMs = null
            holdPlaybackStateAcrossTransition()
            syncQueueFromController()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            syncQueueFromController()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (!shuffleModeEnabled) normalizeTimelineAlphabetically(controller)
            syncQueueFromController()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mode = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            }
            _uiState.value = _uiState.value.copy(repeatMode = mode)
            syncQueueFromController()
        }
    }

    private fun holdPlaybackStateAcrossTransition() {
        val token = ++playbackTransitionToken
        suppressIsPlayingUntilMs = System.currentTimeMillis() + 280L
        scope.launch {
            delay(300L)
            if (token == playbackTransitionToken) {
                controller?.let { c ->
                    _uiState.value = _uiState.value.copy(isPlaying = c.isPlaying)
                }
            }
        }
    }

    private fun syncQueueFromController() {
        val c = controller ?: return
        if (normalizingTimeline) {
            refreshCurrentItem()
            return
        }

        if (c.mediaItemCount > 0 && currentQueue.isNotEmpty()) {
            val songsByMediaId = currentQueue.associateBy { it.id.toString() }
            val controllerSongs = (0 until c.mediaItemCount).mapNotNull { itemIndex ->
                songsByMediaId[c.getMediaItemAt(itemIndex).mediaId]
            }
            if (controllerSongs.size == c.mediaItemCount) {
                val controllerIds = controllerSongs.map { it.id }.toSet()
                currentQueue = currentQueue.filter { it.id in controllerIds } +
                    controllerSongs.filterNot { song -> currentQueue.any { it.id == song.id } }
            }
        }

        if (!c.shuffleModeEnabled) normalizeTimelineAlphabetically(c)
        refreshCurrentItem()
    }

    private fun normalizeTimelineAlphabetically(c: Player?) {
        if (c == null || c.mediaItemCount < 2 || normalizingTimeline) return
        val sortedSongs = currentQueue.sortedWith(alphabeticalSongComparator)
        if (sortedSongs.size != c.mediaItemCount) return

        val targetIds = sortedSongs.map { it.id.toString() }
        val currentIds = (0 until c.mediaItemCount)
            .map { index -> c.getMediaItemAt(index).mediaId }
        if (currentIds == targetIds) {
            currentQueue = sortedSongs
            return
        }

        normalizingTimeline = true
        try {
            val workingIds = currentIds.toMutableList()
            targetIds.forEachIndexed { targetIndex, targetId ->
                val fromIndex = workingIds.indexOf(targetId)
                if (fromIndex >= 0 && fromIndex != targetIndex) {
                    c.moveMediaItem(fromIndex, targetIndex)
                    workingIds.add(targetIndex, workingIds.removeAt(fromIndex))
                }
            }
            currentQueue = sortedSongs
        } finally {
            normalizingTimeline = false
        }
    }

    private fun playbackQueueForDisplay(c: Player): Pair<List<Song>, Int> {
        if (currentQueue.isEmpty()) return emptyList<Song>() to -1
        val currentTimelineIndex = c.currentMediaItemIndex
        if (currentTimelineIndex !in 0 until c.mediaItemCount) {
            return currentQueue to -1
        }

        val songsByMediaId = currentQueue.associateBy { it.id.toString() }
        val displaySongs = mutableListOf<Song>()
        val visitedTimelineIndices = mutableSetOf<Int>()
        var timelineIndex = currentTimelineIndex

        while (
            timelineIndex != C.INDEX_UNSET &&
            timelineIndex in 0 until c.mediaItemCount &&
            visitedTimelineIndices.add(timelineIndex)
        ) {
            songsByMediaId[c.getMediaItemAt(timelineIndex).mediaId]?.let(displaySongs::add)
            timelineIndex = c.currentTimeline.getNextWindowIndex(
                timelineIndex,
                c.repeatMode,
                c.shuffleModeEnabled
            )
        }

        return if (displaySongs.isEmpty()) currentQueue to -1 else displaySongs to 0
    }

    private fun refreshCurrentItem() {
        val c = controller ?: return
        val currentTimelineIndex = c.currentMediaItemIndex
        val currentSong = currentQueue.getOrNull(currentTimelineIndex)
        val (displayQueue, displayIndex) = playbackQueueForDisplay(c)
        _uiState.value = _uiState.value.copy(
            currentSong = currentSong,
            queue = displayQueue,
            currentIndex = displayIndex,
            isShuffled = c.shuffleModeEnabled,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            durationMs = c.duration.coerceAtLeast(0L)
        )
    }

    private fun startPositionTicker() {
        positionTicker?.cancel()
        positionTicker = scope.launch {
            while (true) {
                val c = controller
                if (c != null) {
                    val actualPosition = c.currentPosition.coerceAtLeast(0L)
                    val pendingPosition = pendingSeekPositionMs
                    val displayPosition = if (pendingPosition != null) {
                        if (kotlin.math.abs(actualPosition - pendingPosition) <= 750L) {
                            pendingSeekPositionMs = null
                            actualPosition
                        } else {
                            pendingPosition
                        }
                    } else {
                        actualPosition
                    }
                    _uiState.value = _uiState.value.copy(
                        positionMs = displayPosition,
                        durationMs = c.duration.coerceAtLeast(0L)
                    )
                }
                // Keep lyric highlighting responsive while avoiding a busy loop when paused.
                delay(if (c?.isPlaying == true) 50L else 250L)
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .build()
            )
            .build()
}
