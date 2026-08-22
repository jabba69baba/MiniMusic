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
import kotlinx.coroutines.yield
import kotlin.random.Random

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
    private var shuffleActive = false
    private var nextQueueEntryId = 1L
    private var timelineMutationDepth = 0
    private var syncRequestedAfterMutation = false
    private var shuffleMutationInProgress = false
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
        val selectedSong = selectedId?.let { id -> orderedSongs.firstOrNull { it.id == id } }
        val queueSongs = if (selectedSong != null) {
            listOf(selectedSong) + orderedSongs.filterNot { it.id == selectedSong.id }
        } else {
            orderedSongs
        }

        currentQueueEntries = queueSongs.map { song ->
            QueueEntry(entryId = nextQueueEntryId++, song = song)
        }
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
        val selectedEntry = newEntries[orderedStartIndex]
        val shuffledEntries = newEntries.shuffled(Random(System.nanoTime().toInt())).toMutableList().apply {
            remove(selectedEntry)
            add(0, selectedEntry)
        }
        val mediaItems = shuffledEntries.map { entry ->
            entry.song.toMediaItem(mediaId = entry.entryId.toString())
        }

        runTimelineMutation {
            currentQueueEntries = shuffledEntries
            currentQueue = shuffledEntries.map { it.song }
            shuffleActive = true
            pendingSeekPositionMs = null
            c.setMediaItems(mediaItems, 0, 0L)
            c.shuffleModeEnabled = false
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
        val timelineIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (timelineIndex < 0) return

        holdPlaybackStateAcrossTransition()
        _uiState.value = _uiState.value.copy(isPlaying = true)
        c.seekTo(timelineIndex, 0L)
        c.play()
    }

    /** Move a queue entry by stable identity, keeping Media3 and the local mirror aligned. */
    fun moveQueueEntry(entryId: Long, toIndex: Int) {
        if (shuffleMutationInProgress) return
        val c = controller ?: return
        val fromIndex = currentQueueEntries.indexOfFirst { it.entryId == entryId }
        if (fromIndex < 0 || currentQueueEntries.isEmpty()) return

        val destinationEntry = currentQueueEntries.getOrNull(toIndex)
        val destination = destinationEntry?.let { entry ->
            currentQueueEntries.indexOfFirst { it.entryId == entry.entryId }
        }?.takeIf { it >= 0 } ?: currentQueueEntries.lastIndex
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

    /** Applies the complete released queue order, preserving active identity by media ID. */
    fun reorderQueue(finalEntryIds: List<Long>) {
        if (shuffleMutationInProgress) return
        val c = controller ?: return
        if (finalEntryIds.size != currentQueueEntries.size ||
            finalEntryIds.toSet().size != finalEntryIds.size ||
            finalEntryIds.toSet() != currentQueueEntries.map { it.entryId }.toSet() ||
            c.mediaItemCount != currentQueueEntries.size
        ) return

        val targetEntries = finalEntryIds.mapNotNull { id ->
            currentQueueEntries.firstOrNull { it.entryId == id }
        }
        if (targetEntries.size != currentQueueEntries.size) return

        runTimelineMutation {
            reorderTimelineEntries(c, targetEntries)
            refreshCurrentItem()
        }
    }

    fun removeQueueEntry(entryId: Long) {
        if (shuffleMutationInProgress) return
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
        if (shuffleMutationInProgress || c.mediaItemCount < 2) return
        if (currentQueueEntries.size != c.mediaItemCount) {
            syncQueueFromController()
            if (currentQueueEntries.size != c.mediaItemCount) return
        }

        shuffleMutationInProgress = true
        val previousShuffleActive = shuffleActive
        val enabled = !shuffleActive
        // Publish the control state before the timeline work begins so the
        // player button responds on the tap frame rather than after all moves.
        shuffleActive = enabled
        _uiState.value = _uiState.value.copy(isShuffled = enabled)
        val targetEntries = if (enabled) {
            freshShuffleEntries(c)
        } else {
            currentQueueEntries.sortedWith(compareBy<QueueEntry> {
                it.song.title.trim().lowercase()
            }.thenBy { it.song.artist.trim().lowercase() }.thenBy { it.song.id })
        }

        // Publish the target order immediately. The active entry is resolved by
        // stable media ID, so this does not disturb the current playback clock
        // while the controller reconciles its physical timeline below.
        currentQueueEntries = targetEntries
        currentQueue = targetEntries.map { it.song }
        refreshCurrentItem()

        // Move one existing item at a time and yield between moves. Media3 must
        // receive these calls on the main thread, but keeping the whole loop in
        // one synchronous callback starves Compose and the position ticker.
        scope.launch {
            try {
                runTimelineMutationSuspend {
                    reorderTimelineEntriesWithoutBlocking(c, targetEntries)
                    c.shuffleModeEnabled = false
                    syncQueueFromController()
                }
            } catch (_: RuntimeException) {
                // Keep the controller usable if Media3 rejects a transient move;
                // restore the previous visual state and republish the timeline.
                shuffleActive = previousShuffleActive
                _uiState.value = _uiState.value.copy(isShuffled = previousShuffleActive)
                syncQueueFromController()
            } finally {
                shuffleMutationInProgress = false
            }
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
            // The command above owns the mutation transaction. This callback may be
            // synchronous on some Media3 versions, so never normalize or publish a
            // partially-mutated timeline from inside the callback.
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

    private suspend fun runTimelineMutationSuspend(block: suspend () -> Unit) {
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

    private fun freshShuffleEntries(c: Player): List<QueueEntry> {
        val activeEntry = resolveCurrentEntry(c)
        val shuffledEntries = currentQueueEntries
            .shuffled(Random(System.nanoTime().toInt()))
            .toMutableList()
        activeEntry?.let { active ->
            shuffledEntries.removeAll { it.entryId == active.entryId }
            shuffledEntries.add(0, active)
        }
        return shuffledEntries
    }

    private suspend fun reorderTimelineEntriesWithoutBlocking(
        c: Player,
        targetEntries: List<QueueEntry>
    ) {
        if (targetEntries.size != c.mediaItemCount || targetEntries.isEmpty()) return
        pendingSeekPositionMs = null
        targetEntries.forEachIndexed { targetIndex, targetEntry ->
            val currentIndex = (targetIndex until c.mediaItemCount).firstOrNull { index ->
                c.getMediaItemAt(index).mediaId == targetEntry.entryId.toString()
            } ?: return@forEachIndexed
            if (currentIndex != targetIndex) {
                c.moveMediaItem(currentIndex, targetIndex)
                yield()
            }
        }
    }

    private fun reorderTimelineEntries(c: Player, targetEntries: List<QueueEntry>) {
        if (targetEntries.size != c.mediaItemCount || targetEntries.isEmpty()) return

        // Reorder the existing Media3 timeline in place. Replacing the entire
        // timeline with setMediaItems() forces a prepare cycle, which briefly
        // reports duration and position as zero and can pause playback.
        pendingSeekPositionMs = null
        targetEntries.forEachIndexed { targetIndex, targetEntry ->
            val currentIndex = (targetIndex until c.mediaItemCount).firstOrNull { index ->
                c.getMediaItemAt(index).mediaId == targetEntry.entryId.toString()
            } ?: return@forEachIndexed
            if (currentIndex != targetIndex) {
                c.moveMediaItem(currentIndex, targetIndex)
            }
        }
        currentQueueEntries = targetEntries
        currentQueue = targetEntries.map { it.song }
    }



    private fun refreshCurrentItem() {
        val c = controller ?: return
        val currentEntry = resolveCurrentEntry(c)
        val currentTimelineIndex = currentEntry?.let { currentQueueEntries.indexOf(it) } ?: -1
        val currentSong = currentEntry?.song
        val displayEntries = currentQueueEntries
        val displayQueue = displayEntries.map { it.song }
        val displayIndex = currentEntry?.let { displayEntries.indexOf(it) } ?: -1
        val history = currentTimelineIndex.takeIf { it > 0 }
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
