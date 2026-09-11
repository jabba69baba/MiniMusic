package com.example.minimusic

import android.app.Application
import com.example.minimusic.data.LyricsReader
import com.example.minimusic.data.MusicRepository
import com.example.minimusic.data.SettingsRepository
import com.example.minimusic.playback.PlayerController

/**
 * Minimal hand-rolled DI container. The app is small enough that a DI framework
 * would add more ceremony than value — this keeps one Repository and one
 * PlayerController alive for the process lifetime.
 */
class MainApplication : Application() {

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var playerController: PlayerController
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var lyricsReader: LyricsReader
        private set

    override fun onCreate() {
        super.onCreate()
        musicRepository = MusicRepository(this)
        playerController = PlayerController(this)
        settingsRepository = SettingsRepository(this)
        lyricsReader = LyricsReader(this)
    }
}
