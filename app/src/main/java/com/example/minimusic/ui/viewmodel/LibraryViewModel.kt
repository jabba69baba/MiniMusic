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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Sort order applied to the Songs tab's list. */
enum class SongSortOrder {
    NAME_A_Z, NAME_Z_A,
    ARTIST_A_Z, ARTIST_Z_A,
    DATE_ADDED_NEWEST, DATE_ADDED_OLDEST
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val allSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SongSortOrder = SongSortOrder.NAME_A_Z
) {
    val filteredSongs: List<Song>
        get() {
            val base = if (searchQuery.isBlank()) allSongs else allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
            }
            return when (sortOrder) {
                SongSortOrder.NAME_A_Z -> base.sortedBy { it.title.lowercase() }
                SongSortOrder.NAME_Z_A -> base.sortedByDescending { it.title.lowercase() }
                SongSortOrder.ARTIST_A_Z -> base.sortedBy { it.artist.lowercase() }
                SongSortOrder.ARTIST_Z_A -> base.sortedByDescending { it.artist.lowercase() }
                SongSortOrder.DATE_ADDED_NEWEST -> base.sortedByDescending { it.dateAddedSeconds }
                SongSortOrder.DATE_ADDED_OLDEST -> base.sortedBy { it.dateAddedSeconds }
            }
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
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val minDuration = settingsRepository.settings.first().minDurationSeconds
                val songs = repository.loadSongs(minDuration)
                val (albums, artists) = withContext(Dispatchers.Default) {
                    repository.deriveAlbums(songs) to repository.deriveArtists(songs)
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = null,
                    allSongs = songs,
                    albums = albums,
                    artists = artists
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "We couldn't load your music library."
                )
            }
        }
    }

    /** Re-reads MediaStore from scratch — used by the "Rescan library" action in Settings. */
    fun rescanLibrary() = loadLibrary()

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onSortOrderChange(sortOrder: SongSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = sortOrder)
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
                    val nextSongs = _uiState.value.allSongs.filterNot { it.id == song.id }
                    val (albums, artists) = withContext(Dispatchers.Default) {
                        repository.deriveAlbums(nextSongs) to repository.deriveArtists(nextSongs)
                    }
                    _uiState.value = _uiState.value.copy(
                        allSongs = nextSongs,
                        albums = albums,
                        artists = artists
                    )
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
