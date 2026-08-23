package com.example.minimusic.playback

import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Background service that owns the single ExoPlayer instance and exposes it through
 * a MediaSession. Media3 handles the playback notification, lock-screen controls and
 * audio focus automatically once this is registered. Everything plays from local
 * content:// URIs, so there is no networking involved at any point.
 */
class MusicService : MediaSessionService() {

    companion object {
        const val ACTION_FRESH_SHUFFLE = "com.example.minimusic.action.FRESH_SHUFFLE"
        const val ACTION_APPLY_SHUFFLE_ORDER = "com.example.minimusic.action.APPLY_SHUFFLE_ORDER"
        const val EXTRA_SHUFFLE_ORDER = "shuffle_order"
    }

    private val freshShuffleCommand = SessionCommand(ACTION_FRESH_SHUFFLE, Bundle())
    private val applyShuffleOrderCommand = SessionCommand(ACTION_APPLY_SHUFFLE_ORDER, Bundle())
    private var mediaSession: MediaSession? = null

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(freshShuffleCommand)
                .add(applyShuffleOrderCommand)
                .build()
            return MediaSession.ConnectionResult.accept(
                commands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_FRESH_SHUFFLE) {
                (session.player as? ExoPlayer)?.let { player ->
                    if (player.mediaItemCount > 1) {
                        player.setShuffleOrder(
                            DefaultShuffleOrder(player.mediaItemCount, System.nanoTime())
                        )
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == ACTION_APPLY_SHUFFLE_ORDER) {
                val requestedOrder = args.getIntArray(EXTRA_SHUFFLE_ORDER)
                val player = session.player as? ExoPlayer
                if (player == null || requestedOrder == null ||
                    requestedOrder.size != player.mediaItemCount ||
                    requestedOrder.toSet().size != requestedOrder.size ||
                    requestedOrder.any { it !in 0 until player.mediaItemCount }
                ) {
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                }
                player.setShuffleOrder(DefaultShuffleOrder(requestedOrder, System.nanoTime()))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
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
