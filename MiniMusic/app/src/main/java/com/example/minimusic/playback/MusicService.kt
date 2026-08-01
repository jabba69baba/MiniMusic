package com.example.minimusic.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.minimusic.MainActivity

/**
 * Background service that owns the single ExoPlayer instance and exposes it through
 * a MediaSession. Media3 handles the playback notification, lock-screen controls and
 * audio focus automatically once this is registered. Everything plays from local
 * content:// URIs, so there is no networking involved at any point.
 *
 * Playback engine notes:
 *
 * - **Audio focus**: [AudioAttributes] with [C.USAGE_MEDIA] / [C.AUDIO_CONTENT_TYPE_MUSIC]
 *   plus `setHandleAudioFocus(true)` hands focus management fully to ExoPlayer — it
 *   requests focus on play, ducks or pauses on transient loss (e.g. a notification
 *   sound), and pauses outright on a permanent loss (e.g. another music app starts).
 * - **Gapless playback**: `setPauseAtEndOfMediaItems(false)` is ExoPlayer's default,
 *   set explicitly and documented here since it's the one flag that silently defeats
 *   gapless transitions between tracks if ever accidentally flipped. Combined with a
 *   [DefaultLoadControl] tuned down from the streaming-oriented defaults (no network
 *   latency to hide for local files), the gap between consecutive tracks stays
 *   effectively zero.
 * - **Audio-becoming-noisy** (headphones unplugged): `setHandleAudioBecomingNoisy(true)`.
 */
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                pauseAtEndOfMediaItems = false // must stay false for gapless transitions
            }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

