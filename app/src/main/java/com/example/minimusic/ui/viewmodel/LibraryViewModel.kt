package com.example.minimusic.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.DeleteResult
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

/** One-off events the Library screen should react to but shouldn't be replayed on recomposition. */
sealed interface LibraryEvent {
    /** Launch this IntentSender via an ActivityResultLauncher; on success, retry deleting [song]. */
    data class RequestDeletePermission(val intentSender: IntentSender, val song: Song) : LibraryEvent
    data class DeleteFailed(val message: String) : LibraryEvent
    data class SongDeleted(val song: Song) : LibraryEvent
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as MainApplication).musicRepository
    private val settingsRepository = (application as MainApplication).settingsRepository

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

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

    /**
     * Deletes [song] from the device. On Android 10+ this may need one round-trip
     * through a system confirmation dialog — the screen should observe [events]
     * for [LibraryEvent.RequestDeletePermission] and call this again after the
     * user confirms.
     */
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            when (val result = repository.deleteSong(song)) {
                is DeleteResult.Deleted -> {
                    _uiState.value = _uiState.value.copy(
                        allSongs = _uiState.value.allSongs.filterNot { it.id == song.id }
                    ).let { it.copy(albums = repository.deriveAlbums(it.allSongs), artists = repository.deriveArtists(it.allSongs)) }
                    _events.emit(LibraryEvent.SongDeleted(song))
                }
                is DeleteResult.NeedsPermission ->
                    _events.emit(LibraryEvent.RequestDeletePermission(result.intentSender, song))
                is DeleteResult.Failed ->
                    _events.emit(LibraryEvent.DeleteFailed(result.message))
            }
        }
    }
}
