package com.example.minimusic.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minimusic.data.model.Song
import com.example.minimusic.data.playlist.Playlist
import com.example.minimusic.data.playlist.PlaylistDao
import com.example.minimusic.data.playlist.PlaylistDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PlaylistWithSongs(
    val playlist: Playlist,
    val songs: List<Song>
)

/**
 * Owns the playlist database. Songs are resolved against the live library on
 * read, so deleting a file removes it from every playlist naturally.
 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: PlaylistDao = PlaylistDatabase.get(application).playlistDao()

    fun observeAll(): Flow<List<Playlist>> = dao.observeAll()

    fun observePlaylistWithSongs(playlistId: Long, allSongsProvider: () -> List<Song>): Flow<PlaylistWithSongs?> =
        combine(dao.observeById(playlistId), dao.observeSongIds(playlistId)) { playlist, ids ->
            val byId = allSongsProvider().associateBy { it.id }
            playlist?.let { p -> PlaylistWithSongs(p, ids.mapNotNull { byId[it] }) }
        }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = dao.insertPlaylist(
                Playlist(
                    name = name.trim().ifEmpty { "New playlist" },
                    createdAtSeconds = System.currentTimeMillis() / 1000
                )
            )
            if (id > 0) onCreated(id)
        }
    }

    /** Create without any follow-up action (Playlists tab button). */
    fun createPlaylistNameOnly(name: String) = createPlaylist(name)

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { dao.deletePlaylist(id) }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch { dao.appendSongs(playlistId, songIds) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            val remaining = dao.rawSongIds(playlistId).filter { it != songId }
            dao.setSongs(playlistId, remaining)
        }
    }
}
