package com.example.minimusic.ui.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.content.IntentSender
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.MainApplication
import com.example.minimusic.data.DeleteResult
import com.example.minimusic.data.model.Album
import com.example.minimusic.data.model.Artist
import com.example.minimusic.data.model.Song
import com.example.minimusic.ui.components.MiniMusicImageLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/**
 * Sort order applied to the Artists tab's list.
 *
 * The three tabs are peers of one set, so each carries its own order rather
 * than only Songs being sortable. Every order here is meaningful to an artist
 * row: its name, or the two counts the row already displays.
 */
enum class ArtistSortOrder {
    NAME_A_Z, NAME_Z_A,
    SONGS_MOST, SONGS_FEWEST,
    ALBUMS_MOST, ALBUMS_FEWEST
}

/**
 * Sort order applied to the Albums tab's list. As with
 * [ArtistSortOrder], the options are the ones an album row can actually be
 * judged by: its title, its artist, or how many songs it holds.
 */
enum class AlbumSortOrder {
    TITLE_A_Z, TITLE_Z_A,
    ARTIST_A_Z, ARTIST_Z_A,
    SONGS_MOST, SONGS_FEWEST
}

/** Applies an [ArtistSortOrder] to the artists list. */
fun sortArtists(artists: List<Artist>, order: ArtistSortOrder): List<Artist> = when (order) {
    ArtistSortOrder.NAME_A_Z -> artists.sortedBy { it.name.lowercase() }
    ArtistSortOrder.NAME_Z_A -> artists.sortedByDescending { it.name.lowercase() }
    ArtistSortOrder.SONGS_MOST -> artists.sortedWith(
        compareByDescending<Artist> { it.songCount }.thenBy { it.name.lowercase() }
    )
    ArtistSortOrder.SONGS_FEWEST -> artists.sortedWith(
        compareBy<Artist> { it.songCount }.thenBy { it.name.lowercase() }
    )
    ArtistSortOrder.ALBUMS_MOST -> artists.sortedWith(
        compareByDescending<Artist> { it.albumCount }.thenBy { it.name.lowercase() }
    )
    ArtistSortOrder.ALBUMS_FEWEST -> artists.sortedWith(
        compareBy<Artist> { it.albumCount }.thenBy { it.name.lowercase() }
    )
}

/** Applies an [AlbumSortOrder] to the albums list. */
fun sortAlbums(albums: List<Album>, order: AlbumSortOrder): List<Album> = when (order) {
    AlbumSortOrder.TITLE_A_Z -> albums.sortedBy { it.title.lowercase() }
    AlbumSortOrder.TITLE_Z_A -> albums.sortedByDescending { it.title.lowercase() }
    AlbumSortOrder.ARTIST_A_Z -> albums.sortedWith(
        compareBy<Album> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
    )
    AlbumSortOrder.ARTIST_Z_A -> albums.sortedWith(
        compareByDescending<Album> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
    )
    AlbumSortOrder.SONGS_MOST -> albums.sortedWith(
        compareByDescending<Album> { it.songCount }.thenBy { it.title.lowercase() }
    )
    AlbumSortOrder.SONGS_FEWEST -> albums.sortedWith(
        compareBy<Album> { it.songCount }.thenBy { it.title.lowercase() }
    )
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

    private var loadJob: Job? = null
    private var reloadJob: Job? = null
    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scheduleLibraryReload()
        }
    }

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    init {
        // MediaStore broadcasts changes for imported, deleted, and edited audio.
        // Debouncing prevents a batch copy or tag edit from restarting the query
        // once per row while keeping the library fresh without a manual rescan.
        getApplication<Application>().contentResolver.registerContentObserver(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver
        )
    }

    private fun scheduleLibraryReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(350L)
            loadLibrary()
        }
    }

    /** Call once the READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE permission has been granted. */
    fun loadLibrary() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadError = null)
            try {
                val minDuration = settingsRepository.settings.first().minDurationSeconds
                val songs = repository.loadSongs(minDuration)
                val currentState = _uiState.value

                // Derive the first published list with the active query and sort
                // order already applied. This avoids a visible flash of the raw
                // MediaStore order during a rescan.
                val (albums, artists, filteredSongs) = withContext(Dispatchers.Default) {
                    Triple(
                        repository.deriveAlbums(songs),
                        repository.deriveArtists(songs),
                        filterAndSortSongs(songs, currentState.searchQuery, currentState.sortOrder)
                    )
                }
                // Keep the skeleton up until the first screenful of artwork is
                // warm in the memory cache too: publishing the list while its
                // tiles are still decoding is exactly the "loading tiles" look
                // the skeleton is supposed to cover.
                val appContext = getApplication<Application>()
                val artUris = songs.asSequence()
                    .mapNotNull { it.albumArtUri }
                    .distinct()
                    .take(64)
                    .toList()
                artUris.forEach { uri ->
                    runCatching {
                        coil.request.ImageRequest.Builder(appContext)
                            .data(uri)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .size(96)
                            .build()
                            .let { MiniMusicImageLoader.get(appContext).enqueue(it) }
                    }
                }
                _uiState.value = currentState.copy(
                    isLoading = false,
                    loadError = null,
                    allSongs = songs,
                    albums = albums,
                    artists = artists,
                    filteredSongs = filteredSongs
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

    /** Clears local artwork memory and re-reads MediaStore for the Settings action. */
    fun rescanLibrary() {
        // The app-owned stack, not the default singleton: rows, grid, player
        // and warmups all read from MiniMusicImageLoader's cache.
        MiniMusicImageLoader.get(getApplication()).memoryCache?.clear()
        loadLibrary()
    }

    private var searchJob: Job? = null

    fun onSearchQueryChange(query: String) {
        val state = _uiState.value
        _uiState.value = state.copy(searchQuery = query)
        // Debounced so each keystroke doesn't reorder the list under the
        // finger: rapid typing used to glide rows via animateItem on every
        // character, and tapping mid-glide played the wrong row.
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(250L)
            recomputeFilteredSongs(query, _uiState.value.sortOrder, _uiState.value.allSongs)
        }
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
            .sortedWith(compareBy<Song> { it.trackNumber.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })

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

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaObserver)
        reloadJob?.cancel()
        loadJob?.cancel()
        searchJob?.cancel()
        super.onCleared()
    }
}
