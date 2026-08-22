package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.LyricsReader
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.PlayerController
import com.example.minimusic.playback.QueueSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data class Found(val text: String) : LyricsState
    data object NotFound : LyricsState
}

/** Sleep timer state exposed to the Player screen: null when no timer is running. */
data class SleepTimerState(
    val remainingMs: Long,
    val totalMs: Long
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val controller: PlayerController = (application as MainApplication).playerController
    private val lyricsReader: LyricsReader = (application as MainApplication).lyricsReader

    val uiState: StateFlow<PlaybackUiState> = controller.uiState
    val queueSnapshot: StateFlow<QueueSnapshot> = controller.queueSnapshot

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow<SleepTimerState?>(null)
    val sleepTimerState: StateFlow<SleepTimerState?> = _sleepTimerState.asStateFlow()
    private var sleepTimerJob: Job? = null

    init {
        // Reload lyrics automatically whenever the currently-playing song changes.
        viewModelScope.launch {
            uiState
                .map { it.currentSong }
                .distinctUntilChangedBy { it?.id }
                .collect { song -> loadLyricsFor(song) }
        }
    }

    private fun loadLyricsFor(song: Song?) {
        if (song == null) {
            _lyricsState.value = LyricsState.Idle
            return
        }
        _lyricsState.value = LyricsState.Loading
        viewModelScope.launch {
            val lyrics = lyricsReader.readLyrics(song)
            _lyricsState.value = if (lyrics != null) LyricsState.Found(lyrics) else LyricsState.NotFound
        }
    }

    fun connect() = controller.connect()
    fun playQueue(songs: List<Song>, startIndex: Int) = controller.playQueue(songs, startIndex)
    fun startShufflePlayback(songs: List<Song>, startIndex: Int) =
        controller.startShufflePlayback(songs, startIndex)
    fun playNext(song: Song) = controller.playNext(song)
    fun addToQueue(song: Song) = controller.addToQueue(song)
    fun playFromQueue(index: Int) = controller.playFromQueue(index)
    fun playQueueEntry(entryId: Long) = controller.playQueueEntry(entryId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = controller.moveQueueItem(fromIndex, toIndex)
    fun moveQueueEntry(entryId: Long, toIndex: Int) = controller.moveQueueEntry(entryId, toIndex)
    fun removeQueueEntry(entryId: Long) = controller.removeQueueEntry(entryId)
    fun togglePlayPause() = controller.togglePlayPause()
    fun skipToNext() = controller.skipToNext()
    fun skipToPrevious() = controller.skipToPrevious()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeatMode() = controller.cycleRepeatMode()

    /**
     * Starts (or replaces) a sleep timer for [durationMs]. Counts down on a 1s tick;
     * when it reaches zero, pauses playback (only if currently playing — never toggles
     * an already-paused state back on) and clears the timer.
     */
    fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        _sleepTimerState.value = SleepTimerState(remainingMs = durationMs, totalMs = durationMs)
        sleepTimerJob = viewModelScope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                delay(1_000)
                remaining = (remaining - 1_000).coerceAtLeast(0)
                _sleepTimerState.value = _sleepTimerState.value?.copy(remainingMs = remaining)
            }
            if (uiState.value.isPlaying) {
                controller.togglePlayPause()
            }
            _sleepTimerState.value = null
        }
    }

    /** Cancels any running sleep timer without affecting playback. */
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerState.value = null
    }

    override fun onCleared() {
        sleepTimerJob?.cancel()
        controller.release()
        super.onCleared()
    }
}
