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
    private var currentQueueEntries: List<QueueEntry> = emptyList()
    private var nextQueueEntryId = 1L
    private var timelineMutationDepth = 0
    private var syncRequestedAfterMutation = false
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

    private val _queueSnapshot = MutableStateFlow(QueueSnapshot())
    val queueSnapshot: StateFlow<QueueSnapshot> = _queueSnapshot.asStateFlow()

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

        currentQueueEntries = orderedSongs.map { song ->
            QueueEntry(entryId = nextQueueEntryId++, song = song)
        }
        currentQueue = currentQueueEntries.map { it.song }
        pendingSeekPositionMs = null
        val mediaItems = currentQueueEntries.map { entry ->
            entry.song.toMediaItem(mediaId = entry.entryId.toString())
        }
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
        val entry = currentQueueEntries
            .getOrNull(_uiState.value.queue.getOrNull(index)?.let { song ->
                currentQueueEntries.indexOfFirst { it.song.id == song.id }
            } ?: -1)
            ?: return
        playQueueEntry(entry.entryId)
    }

    fun playQueueEntry(entryId: Long) {
        val c = controller ?: return
        val timelineIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (timelineIndex < 0) return

        holdPlaybackStateAcrossTransition()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        c.seekTo(timelineIndex, 0L)
        c.play()
    }

    /** Move a queue entry by stable identity, keeping Media3 and the local mirror aligned. */
    fun moveQueueEntry(entryId: Long, toIndex: Int) {
        val c = controller ?: return
        val fromIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (fromIndex < 0 || currentQueueEntries.isEmpty()) return
        val destination = toIndex.coerceIn(currentQueueEntries.indices)
        if (fromIndex == destination) return

        runTimelineMutation {
            c.moveMediaItem(fromIndex, destination)
            currentQueueEntries = currentQueueEntries.toMutableList().apply {
                add(destination, removeAt(fromIndex))
            }
            currentQueue = currentQueueEntries.map { it.song }
            refreshCurrentItem()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        currentQueueEntries.getOrNull(fromIndex)?.let { entry ->
            moveQueueEntry(entry.entryId, toIndex)
        }
    }

    fun removeQueueEntry(entryId: Long) {
        val c = controller ?: return
        val removedPosition = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (removedPosition < 0) return
        val wasCurrent = currentQueueEntries.getOrNull(c.currentMediaItemIndex)?.entryId == entryId
        val nextPosition = when {
            !wasCurrent -> -1
            removedPosition + 1 < currentQueueEntries.size -> removedPosition + 1
            c.repeatMode == Player.REPEAT_MODE_ALL && currentQueueEntries.size > 1 -> 0
            else -> -1
        }

        runTimelineMutation {
            c.removeMediaItem(removedPosition)
            currentQueueEntries = currentQueueEntries.toMutableList().apply { removeAt(removedPosition) }
            currentQueue = currentQueueEntries.map { it.song }
            if (wasCurrent && nextPosition >= 0 && currentQueueEntries.isNotEmpty()) {
                val adjustedPosition = if (nextPosition > removedPosition) nextPosition - 1 else nextPosition
                holdPlaybackStateAcrossTransition()
                c.seekTo(adjustedPosition.coerceAtMost(currentQueueEntries.lastIndex), 0L)
                c.play()
            }
            refreshCurrentItem()
        }
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
        val insertAt = (c.currentMediaItemIndex + 1).coerceIn(0, currentQueueEntries.size)
        val entry = QueueEntry(entryId = nextQueueEntryId++, song = song)
        c.addMediaItem(insertAt, song.toMediaItem(mediaId = entry.entryId.toString()))
        currentQueueEntries = currentQueueEntries.toMutableList().apply { add(insertAt, entry) }
        currentQueue = currentQueueEntries.map { it.song }
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
        val entry = QueueEntry(entryId = nextQueueEntryId++, song = song)
        c.addMediaItem(
            currentQueueEntries.size,
            song.toMediaItem(mediaId = entry.entryId.toString())
        )
        currentQueueEntries = currentQueueEntries + entry
        currentQueue = currentQueueEntries.map { it.song }
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
            // A new media item always starts its UI position at zero. Keeping a
            // prior seek target here makes the seekbar briefly rubberband to the
            // previous track before the ticker catches up.
            pendingSeekPositionMs = 0L
            _uiState.value = _uiState.value.copy(positionMs = 0L)
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
        if (timelineMutationDepth > 0) {
            syncRequestedAfterMutation = true
            return
        }
        val c = controller ?: return
        if (normalizingTimeline) {
            refreshCurrentItem()
            return
        }

        if (c.mediaItemCount > 0 && currentQueue.isNotEmpty()) {
            val entriesByMediaId = currentQueueEntries.associateBy { it.entryId.toString() }
            val controllerEntries = (0 until c.mediaItemCount).mapNotNull { itemIndex ->
                entriesByMediaId[c.getMediaItemAt(itemIndex).mediaId]
            }
            if (controllerEntries.size == c.mediaItemCount) {
                currentQueueEntries = controllerEntries
                currentQueue = controllerEntries.map { it.song }
            }
        }

        if (!c.shuffleModeEnabled) normalizeTimelineAlphabetically(c)
        refreshCurrentItem()
    }

    private fun runTimelineMutation(block: () -> Unit) {
        timelineMutationDepth += 1
        try {
            block()
        } finally {
            timelineMutationDepth -= 1
            if (timelineMutationDepth == 0 && syncRequestedAfterMutation) {
                syncRequestedAfterMutation = false
                syncQueueFromController()
            }
        }
    }

    private fun normalizeTimelineAlphabetically(c: Player?) {
        if (c == null || c.mediaItemCount < 2 || normalizingTimeline) return
        val sortedEntries = currentQueueEntries.sortedWith(compareBy<QueueEntry> {
            it.song.title.trim().lowercase()
        }.thenBy { it.song.artist.trim().lowercase() }.thenBy { it.song.id })
        if (sortedEntries.size != c.mediaItemCount) return

        val targetIds = sortedEntries.map { it.entryId.toString() }
        val currentIds = (0 until c.mediaItemCount)
            .map { index -> c.getMediaItemAt(index).mediaId }
        if (currentIds == targetIds) {
            currentQueueEntries = sortedEntries
            currentQueue = sortedEntries.map { it.song }
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
            currentQueueEntries = sortedEntries
            currentQueue = sortedEntries.map { it.song }
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

        val entriesByMediaId = currentQueueEntries.associateBy { it.entryId.toString() }
        val displaySongs = mutableListOf<Song>()
        val visitedTimelineIndices = mutableSetOf<Int>()
        var timelineIndex = currentTimelineIndex

        while (
            timelineIndex != C.INDEX_UNSET &&
            timelineIndex in 0 until c.mediaItemCount &&
            visitedTimelineIndices.add(timelineIndex)
        ) {
            entriesByMediaId[c.getMediaItemAt(timelineIndex).mediaId]?.song?.let(displaySongs::add)
            val displayRepeatMode = if (c.repeatMode == Player.REPEAT_MODE_ONE) {
                Player.REPEAT_MODE_OFF
            } else {
                c.repeatMode
            }
            timelineIndex = c.currentTimeline.getNextWindowIndex(
                timelineIndex,
                displayRepeatMode,
                c.shuffleModeEnabled
            )
        }

        return if (displaySongs.isEmpty()) currentQueue to -1 else displaySongs to 0
    }

    private fun refreshCurrentItem() {
        val c = controller ?: return
        val currentEntry = resolveCurrentEntry(c)
        val currentTimelineIndex = currentEntry?.let { currentQueueEntries.indexOf(it) } ?: -1
        val currentSong = currentEntry?.song
        val (displayQueue, displayIndex) = playbackQueueForDisplay(c)
        _queueSnapshot.value = QueueSnapshot(
            entries = currentQueueEntries,
            currentPosition = currentTimelineIndex.takeIf { it in currentQueueEntries.indices } ?: -1,
            currentEntryId = currentEntry?.entryId
        )
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

    private fun resolveCurrentEntry(c: Player): QueueEntry? {
        val mediaId = c.currentMediaItem?.mediaId
        return currentQueueEntries.firstOrNull { it.entryId.toString() == mediaId }
            ?: currentQueueEntries.getOrNull(c.currentMediaItemIndex)
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

    private fun Song.toMediaItem(mediaId: String = id.toString()): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
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
