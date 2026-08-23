package com.example.minimusic.playback

import android.content.ComponentName
import android.os.Bundle
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
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
    /** Logical Shuffle button state; may be false while native shuffle carries a manual order. */
    private var shuffleActive = false
    /** Explicit display traversal after a manual move in a generated shuffled queue. */
    private var manualQueueOrderEntryIds: List<Long>? = null
    private var nextQueueEntryId = 1L
    private var timelineMutationDepth = 0
    private var syncRequestedAfterMutation = false
    private val freshShuffleCommand = SessionCommand(
        MusicService.ACTION_FRESH_SHUFFLE,
        Bundle()
    )
    private val applyShuffleOrderCommand = SessionCommand(
        MusicService.ACTION_APPLY_SHUFFLE_ORDER,
        Bundle()
    )
    private val alphabeticalSongComparator = compareBy<Song> {
        it.title.trim().lowercase()
    }.thenBy { it.artist.trim().lowercase() }
        .thenBy { it.id }
    private var positionTicker: Job? = null
    private var playbackTransitionToken = 0L
    private var suppressIsPlayingUntilMs = 0L
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
        val selectedIndex = selectedId?.let { id ->
            orderedSongs.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        }
        val queueSongs = if (selectedIndex != null) {
            orderedSongs.drop(selectedIndex) + orderedSongs.take(selectedIndex)
        } else {
            orderedSongs
        }

        currentQueueEntries = queueSongs.map { song ->
            QueueEntry(entryId = nextQueueEntryId++, song = song)
        }
        manualQueueOrderEntryIds = null
        c.shuffleModeEnabled = false
        shuffleActive = false
        currentQueue = currentQueueEntries.map { it.song }
        pendingSeekPositionMs = null
        val mediaItems = currentQueueEntries.map { entry ->
            entry.song.toMediaItem(mediaId = entry.entryId.toString())
        }
        c.setMediaItems(mediaItems, 0, 0L)
        c.prepare()
        c.play()
    }

    /** Loads and starts a fresh queue with shuffle enabled as one transaction. */
    fun startShufflePlayback(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val selectedId = songs.getOrNull(startIndex)?.id
        val orderedSongs = songs.sortedWith(alphabeticalSongComparator)
        val orderedStartIndex = selectedId?.let { id ->
            orderedSongs.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        } ?: 0
        val newEntries = orderedSongs.map { song ->
            QueueEntry(entryId = nextQueueEntryId++, song = song)
        }
        val mediaItems = newEntries.map { entry ->
            entry.song.toMediaItem(mediaId = entry.entryId.toString())
        }

        runTimelineMutation {
            currentQueueEntries = newEntries
            currentQueue = newEntries.map { it.song }
            manualQueueOrderEntryIds = null
            shuffleActive = true
            pendingSeekPositionMs = null
            c.setMediaItems(mediaItems, orderedStartIndex, 0L)
            c.shuffleModeEnabled = true
            c.prepare()
            c.play()
            refreshCurrentItem()
        }
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
        val selectedEntry = currentQueueEntries.firstOrNull { it.entryId == entryId } ?: return
        if (shuffleActive || manualQueueOrderEntryIds != null) {
            val timelineIndex = currentQueueEntries.indexOf(selectedEntry)
            if (timelineIndex < 0) return
            holdPlaybackStateAcrossTransition()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            c.seekTo(timelineIndex, 0L)
            c.play()
            return
        }

        val selectedIndex = currentQueueEntries.indexOfFirst { it.entryId == selectedEntry.entryId }
        if (selectedIndex < 0) return
        val rotatedEntries = if (selectedIndex == 0) {
            currentQueueEntries
        } else {
            currentQueueEntries.drop(selectedIndex) + currentQueueEntries.take(selectedIndex)
        }

        if (selectedIndex > 0) {
            runTimelineMutation {
                // A single range move preserves playback continuity and avoids
                // the callback storm caused by repeatedly moving every row.
                c.moveMediaItems(0, selectedIndex, c.mediaItemCount)
                currentQueueEntries = rotatedEntries
                currentQueue = rotatedEntries.map { it.song }
                refreshCurrentItem()
            }
        }

        val timelineIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (timelineIndex < 0) return
        holdPlaybackStateAcrossTransition()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        c.seekTo(timelineIndex, 0L)
        c.play()
    }

    /**
     * Moves one displayed queue entry by identity. The display list may be a
     * native shuffled traversal, so its indices are translated back to the
     * physical Media3 timeline before the single move is issued.
     */
    fun moveQueueEntry(entryId: Long, toDisplayIndex: Int) {
        val c = controller ?: return
        val displayEntries = manualQueueOrderEntryIds
            ?.mapNotNull { id -> currentQueueEntries.firstOrNull { it.entryId == id } }
            ?: if (c.shuffleModeEnabled) nativePlaybackOrder(c) else currentQueueEntries
        val fromDisplayIndex = displayEntries.indexOfFirst { it.entryId == entryId }
        if (fromDisplayIndex < 0 || toDisplayIndex !in displayEntries.indices || fromDisplayIndex == toDisplayIndex) {
            return
        }

        val targetEntry = displayEntries[toDisplayIndex]
        val fromPhysicalIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        val toPhysicalIndex = currentQueueEntries.indexOfFirst { it.entryId == targetEntry.entryId }
        if (fromPhysicalIndex < 0 || toPhysicalIndex < 0 || fromPhysicalIndex == toPhysicalIndex) return

        val movedDisplayEntries = displayEntries.toMutableList().apply {
            add(toDisplayIndex, removeAt(fromDisplayIndex))
        }
        val preserveDisplayOrder = c.shuffleModeEnabled
        runTimelineMutation {
            c.moveMediaItem(fromPhysicalIndex, toPhysicalIndex)
            currentQueueEntries = currentQueueEntries.toMutableList().apply {
                add(toPhysicalIndex, removeAt(fromPhysicalIndex))
            }
            currentQueue = currentQueueEntries.map { it.song }

            if (preserveDisplayOrder) {
                // The generated traversal remains authoritative after this one
                // physical move. Media3 stays in native shuffle mode so Next and
                // automatic transitions continue to use the edited shuffled order.
                manualQueueOrderEntryIds = movedDisplayEntries.map { it.entryId }
                // The generated shuffled traversal remains active after a manual
                // move, so the Player Shuffle button must remain visibly enabled.
                shuffleActive = true
                _uiState.value = _uiState.value.copy(isShuffled = true)
                val physicalOrder = manualQueueOrderEntryIds.orEmpty().mapNotNull { id ->
                    currentQueueEntries.indexOfFirst { it.entryId == id }
                        .takeIf { it >= 0 }
                }.toIntArray()
                if (physicalOrder.size == currentQueueEntries.size) {
                    val args = Bundle().apply {
                        putIntArray(MusicService.EXTRA_SHUFFLE_ORDER, physicalOrder)
                    }
                    c.sendCustomCommand(applyShuffleOrderCommand, args)
                        .addListener({ refreshCurrentItem() }, MoreExecutors.directExecutor())
                }
            }
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
        runTimelineMutation {
            // Media3 owns next-item selection here, including shuffle and repeat rules.
            // We only remove the stable entry and let the transition callback publish
            // the new visible order.
            if (wasCurrent) holdPlaybackStateAcrossTransition()
            c.removeMediaItem(removedPosition)
            currentQueueEntries = currentQueueEntries.toMutableList().apply { removeAt(removedPosition) }
            manualQueueOrderEntryIds = manualQueueOrderEntryIds?.filterNot { it == entryId }
            currentQueue = currentQueueEntries.map { it.song }
            refreshCurrentItem()
        }
    }

    /** Stops playback and empties the active queue without releasing the controller. */
    fun clearQueue() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
        currentQueueEntries = emptyList()
        currentQueue = emptyList()
        shuffleActive = false
        manualQueueOrderEntryIds = null
        pendingSeekPositionMs = null
        _queueSnapshot.value = QueueSnapshot()
        _uiState.value = PlaybackUiState()
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
        manualQueueOrderEntryIds = manualQueueOrderEntryIds?.toMutableList()?.apply {
            add(entry.entryId)
        }
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
        manualQueueOrderEntryIds = manualQueueOrderEntryIds?.plus(entry.entryId)
        currentQueue = currentQueueEntries.map { it.song }
        refreshCurrentItem()
    }

    fun toggleShuffle() {
        val c = controller ?: return
        if (c.mediaItemCount < 2) return
        val enabled = !shuffleActive
        try {
            // Native shuffle changes only Media3's traversal metadata. The active
            // playlist, item, position, duration, and play state remain untouched.
            manualQueueOrderEntryIds = null
            c.shuffleModeEnabled = enabled
            shuffleActive = enabled
            _uiState.value = _uiState.value.copy(isShuffled = enabled)
            if (enabled) {
                c.sendCustomCommand(freshShuffleCommand, Bundle())
                    .addListener({ refreshCurrentItem() }, MoreExecutors.directExecutor())
            } else {
                refreshCurrentItem()
            }
        } catch (_: RuntimeException) {
            // Restore the button state if the controller rejects the mode change.
            manualQueueOrderEntryIds = null
            shuffleActive = c.shuffleModeEnabled
            _uiState.value = _uiState.value.copy(isShuffled = shuffleActive)
        }
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
            // Queue history is derived from the active entry's position in the
            // complete order; it is not an event log that can drop skipped songs.
            // A new media item always starts its UI position at zero. Keeping a
            // prior seek target here makes the seekbar briefly rubberband to the
            // previous track before the ticker catches up.
            // A media transition starts the new track at zero, but zero must not
            // remain as a pending seek target while the new track progresses.
            // Otherwise the ticker keeps rendering 0 after shuffle/reorder.
            pendingSeekPositionMs = null
            _uiState.value = _uiState.value.copy(positionMs = 0L)
            holdPlaybackStateAcrossTransition()
            syncQueueFromController()
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            syncQueueFromController()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            // A manual queue order intentionally keeps native shuffle enabled while
            // the Shuffle button is logically off. If native shuffle is turned off
            // externally, the manual traversal is no longer executable and must go.
            if (!shuffleModeEnabled) {
                manualQueueOrderEntryIds = null
                shuffleActive = false
            } else if (manualQueueOrderEntryIds == null) {
                shuffleActive = true
            }
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



    private fun refreshCurrentItem() {
        val c = controller ?: return
        if (manualQueueOrderEntryIds == null) {
            shuffleActive = c.shuffleModeEnabled
        }
        val currentEntry = resolveCurrentEntry(c)
        val currentTimelineIndex = currentEntry?.let { currentQueueEntries.indexOf(it) } ?: -1
        val currentSong = currentEntry?.song
        val displayEntries = manualQueueOrderEntryIds
            ?.mapNotNull { id -> currentQueueEntries.firstOrNull { it.entryId == id } }
            ?: if (c.shuffleModeEnabled) nativePlaybackOrder(c) else currentQueueEntries
        val displayQueue = displayEntries.map { it.song }
        val displayIndex = currentEntry?.let { displayEntries.indexOf(it) } ?: -1
        val history = displayIndex.takeIf { it > 0 }
            ?.let { displayEntries.take(it) }
            ?: emptyList()
        _queueSnapshot.value = QueueSnapshot(
            entries = currentQueueEntries,
            currentPosition = currentTimelineIndex.takeIf { it in currentQueueEntries.indices } ?: -1,
            currentEntryId = currentEntry?.entryId,
            historyEntries = history,
            visibleEntries = displayEntries
        )
        _uiState.value = _uiState.value.copy(
            currentSong = currentSong,
            queue = displayQueue,
            currentIndex = displayIndex,
            isShuffled = shuffleActive,
            repeatMode = when (c.repeatMode) {
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                else -> RepeatMode.OFF
            },
            durationMs = c.duration.coerceAtLeast(0L)
        )
    }

    private fun nativePlaybackOrder(c: Player): List<QueueEntry> {
        val entriesByMediaId = currentQueueEntries.associateBy { it.entryId.toString() }
        val currentIndex = c.currentMediaItemIndex
        if (currentIndex !in 0 until c.mediaItemCount) return currentQueueEntries

        val timeline = c.currentTimeline
        val previousIndices = mutableListOf<Int>()
        val previousSeen = mutableSetOf<Int>()
        var previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            true
        )
        while (previousIndex >= 0 && previousSeen.add(previousIndex)) {
            previousIndices += previousIndex
            previousIndex = timeline.getPreviousWindowIndex(
                previousIndex,
                Player.REPEAT_MODE_OFF,
                true
            )
        }

        val orderedIndices = previousIndices.asReversed().toMutableList().apply {
            add(currentIndex)
        }
        val followingSeen = orderedIndices.toMutableSet()
        var nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            true
        )
        while (nextIndex >= 0 && followingSeen.add(nextIndex)) {
            orderedIndices += nextIndex
            nextIndex = timeline.getNextWindowIndex(
                nextIndex,
                Player.REPEAT_MODE_OFF,
                true
            )
        }

        return orderedIndices.mapNotNull { index ->
            entriesByMediaId[c.getMediaItemAt(index).mediaId]
        }.ifEmpty { currentQueueEntries }
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
