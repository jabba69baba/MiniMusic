package com.example.minimusic.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.DeleteResult
import coil.imageLoader
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
    ALBUM_A_Z, ALBUM_Z_A,
    DURATION_SHORTEST, DURATION_LONGEST,
    DATE_ADDED_NEWEST, DATE_ADDED_OLDEST
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val allSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SongSortOrder = SongSortOrder.NAME_A_Z,
    val filteredSongs: List<Song> = emptyList()
)

private fun filterAndSortSongs(
    songs: List<Song>,
    query: String,
    sortOrder: SongSortOrder
): List<Song> {
    val base = if (query.isBlank()) songs else songs.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
    }
    return when (sortOrder) {
        SongSortOrder.NAME_A_Z -> base.sortedBy { it.title.lowercase() }
        SongSortOrder.NAME_Z_A -> base.sortedByDescending { it.title.lowercase() }
        SongSortOrder.ARTIST_A_Z -> base.sortedBy { it.artist.lowercase() }
        SongSortOrder.ARTIST_Z_A -> base.sortedByDescending { it.artist.lowercase() }
        SongSortOrder.ALBUM_A_Z -> base.sortedBy { it.album.lowercase() }
        SongSortOrder.ALBUM_Z_A -> base.sortedByDescending { it.album.lowercase() }
        SongSortOrder.DURATION_SHORTEST -> base.sortedBy { it.durationMs }
        SongSortOrder.DURATION_LONGEST -> base.sortedByDescending { it.durationMs }
        SongSortOrder.DATE_ADDED_NEWEST -> base.sortedByDescending { it.dateAddedSeconds }
        SongSortOrder.DATE_ADDED_OLDEST -> base.sortedBy { it.dateAddedSeconds }
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
                val currentState = _uiState.value

                // The repository query already returns title-sorted Song objects with
                // title/artist/album metadata populated. Publish that first frame as
                // soon as the local query completes so the home list becomes
                // interactive immediately instead of waiting for derived tabs and a
                // synthetic loading delay.
                _uiState.value = currentState.copy(
                    isLoading = false,
                    loadError = null,
                    allSongs = songs,
                    filteredSongs = songs
                )

                // Albums, artists, and non-default filtering are secondary work. Keep
                // them off the UI thread and update only if this load still owns the
                // current song list, so they cannot stall first-render scrolling.
                val (albums, artists, filteredSongs) = withContext(Dispatchers.Default) {
                    Triple(
                        repository.deriveAlbums(songs),
                        repository.deriveArtists(songs),
                        filterAndSortSongs(songs, currentState.searchQuery, currentState.sortOrder)
                    )
                }
                if (_uiState.value.allSongs == songs) {
                    _uiState.value = _uiState.value.copy(
                        albums = albums,
                        artists = artists,
                        filteredSongs = filteredSongs
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    loadError = "We couldn't load your music library."
                )
            }
        }
    }

    /** Clears local artwork memory and re-reads MediaStore for the Settings action. */
    fun rescanLibrary() {
        getApplication<Application>().imageLoader.memoryCache?.clear()
        loadLibrary()
    }

    fun onSearchQueryChange(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(searchQuery = query)
        recomputeFilteredSongs(query, state.sortOrder, state.allSongs)
    }

    fun onSortOrderChange(sortOrder: SongSortOrder) {
        val state = _uiState.value
        _uiState.value = state.copy(sortOrder = sortOrder)
        recomputeFilteredSongs(state.searchQuery, sortOrder, state.allSongs)
    }

    private fun recomputeFilteredSongs(
        query: String,
        sortOrder: SongSortOrder,
        songs: List<Song>
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = filterAndSortSongs(songs, query, sortOrder)
            if (_uiState.value.searchQuery == query && _uiState.value.sortOrder == sortOrder) {
                _uiState.value = _uiState.value.copy(filteredSongs = filtered)
            }
        }
    }

    fun songById(songId: Long): Song? =
        _uiState.value.allSongs.firstOrNull { it.id == songId }

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
                    val state = _uiState.value
                    val nextSongs = state.allSongs.filterNot { it.id == song.id }
                    val (albums, artists, filteredSongs) = withContext(Dispatchers.Default) {
                        Triple(
                            repository.deriveAlbums(nextSongs),
                            repository.deriveArtists(nextSongs),
                            filterAndSortSongs(nextSongs, state.searchQuery, state.sortOrder)
                        )
                    }
                    _uiState.value = state.copy(
                        allSongs = nextSongs,
                        albums = albums,
                        artists = artists,
                        filteredSongs = filteredSongs
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
