package com.example.minimusic.playback

import com.example.minimusic.data.model.Song

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)
