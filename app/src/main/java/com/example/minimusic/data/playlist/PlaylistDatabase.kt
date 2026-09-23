package com.example.minimusic.data.playlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Playlist::class, PlaylistSong::class], version = 1, exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile private var instance: PlaylistDatabase? = null

        fun get(context: Context): PlaylistDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlaylistDatabase::class.java,
                    "minimusic-playlists.db"
                ).build().also { instance = it }
            }
    }
}
