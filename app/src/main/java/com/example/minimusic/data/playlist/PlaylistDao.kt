package com.example.minimusic.data.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAtSeconds DESC")
    fun observeAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun observeById(id: Long): Flow<Playlist?>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun observeSongCount(playlistId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<PlaylistSong>)

    /** Replaces the playlist's songs with the given ordered ids. */
    @Transaction
    suspend fun setSongs(playlistId: Long, songIds: List<Long>) {
        clearSongs(playlistId)
        insertSongs(songIds.mapIndexed { index, songId ->
            PlaylistSong(playlistId = playlistId, songId = songId, position = index)
        })
    }

    /** Appends songs at the end of the playlist's current order. */
    @Transaction
    suspend fun appendSongs(playlistId: Long, songIds: List<Long>) {
        val current = rawSongIds(playlistId)
        insertSongs(songIds.mapIndexed { index, songId ->
            PlaylistSong(playlistId = playlistId, songId = songId, position = current.size + index)
        })
    }

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun rawSongIds(playlistId: Long): List<Long>
}
