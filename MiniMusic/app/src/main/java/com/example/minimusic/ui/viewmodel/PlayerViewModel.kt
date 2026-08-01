package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.LyricsReader
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.PlaybackUiState
import com.example.minimusic.playback.PlayerController
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

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val controller: PlayerController = (application as MainApplication).playerController
    private val lyricsReader: LyricsReader = (application as MainApplication).lyricsReader

    val uiState: StateFlow<PlaybackUiState> = controller.uiState

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

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

    fun playFromQueue(index: Int) = controller.playFromQueue(index)

    fun togglePlayPause() = controller.togglePlayPause()

    fun skipToNext() = controller.skipToNext()

    fun skipToPrevious() = controller.skipToPrevious()

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun toggleShuffle() = controller.toggleShuffle()

    fun cycleRepeatMode() = controller.cycleRepeatMode()

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}
