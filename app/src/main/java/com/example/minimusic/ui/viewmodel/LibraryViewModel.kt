package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val allSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = ""
) {
    val filteredSongs: List<Song>
        get() = if (searchQuery.isBlank()) allSongs else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
        }
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MainApplication).musicRepository
    private val settingsRepository = (application as MainApplication).settingsRepository

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /** Call once the READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE permission has been granted. */
    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val minDuration = settingsRepository.settings.first().minDurationSeconds
            val songs = repository.loadSongs(minDuration)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                allSongs = songs,
                albums = repository.deriveAlbums(songs),
                artists = repository.deriveArtists(songs)
            )
        }
    }

    /** Re-reads MediaStore from scratch — used by the "Rescan library" action in Settings. */
    fun rescanLibrary() = loadLibrary()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun songsForAlbum(albumId: Long): List<Song> =
        _uiState.value.allSongs.filter { it.albumId == albumId }

    fun songsForArtist(artistName: String): List<Song> =
        _uiState.value.allSongs.filter { it.artist == artistName }
}
