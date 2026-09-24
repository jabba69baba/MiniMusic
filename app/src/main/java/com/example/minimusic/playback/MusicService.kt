package com.example.minimusic.playback

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.example.minimusic.data.SettingsRepository
import com.example.minimusic.widget.MiniMusicWidgetProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background service that owns the single ExoPlayer instance and exposes it through
 * a MediaSession. Media3 handles the playback notification, lock-screen controls and
 * audio focus automatically once this is registered. Everything plays from local
 * content:// URIs, so there is no networking involved at any point.
 */
class MusicService : MediaSessionService() {
    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
        const val ACTION_UPDATE_WIDGET = "com.example.minimusic.widget.UPDATE_WIDGET"

        const val ACTION_FRESH_SHUFFLE = "com.example.minimusic.action.FRESH_SHUFFLE"
        const val ACTION_APPLY_SHUFFLE_ORDER = "com.example.minimusic.action.APPLY_SHUFFLE_ORDER"
        const val EXTRA_SHUFFLE_ORDER = "shuffle_order"
    }

    private val freshShuffleCommand = SessionCommand(ACTION_FRESH_SHUFFLE, Bundle())
    private val applyShuffleOrderCommand = SessionCommand(ACTION_APPLY_SHUFFLE_ORDER, Bundle())
    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val monoProcessor = MonoAudioProcessor()
    private lateinit var settingsRepository: SettingsRepository
    private var crossfadeEngine: CrossfadeEngine? = null

    /** Late-bound: assigned in onCreate right after the player is built. */
    private var activePlayer: Player? = null

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
        isRunning = true
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val crossfadeEngine = CrossfadeEngine(
            positionProvider = { activePlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L }
        )
        this.crossfadeEngine = crossfadeEngine
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(monoProcessor, crossfadeEngine)
                    )
                    .build()
            }
        }.setEnableAudioFloatOutput(false)
        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            // setPauseAtEndOfMediaItems is intentionally NOT enabled: the sleep
            // timer needs to pause after exactly one song (PlayerController's
            // pauseAtNextTransition), not after every item. ExoPlayer's default
            // gapless transition between prepared adjacent items is kept as-is.
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
            .also { exoPlayer ->
                activePlayer = exoPlayer
                exoPlayer.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        MiniMusicWidgetProvider.requestUpdate(this@MusicService)
                        // Re-derive the fade window on EVERY event batch: the
                        // duration only becomes known once the source is prepared,
                        // and a stale duration (0 or the previous track's) with an
                        // active fade is what previously muted playback. Keeping
                        // the window in sync with the real item is cheap (a couple
                        // of volatile writes) and cannot desync.
                        crossfadeEngine.configure(
                            enabledSecondsMs = crossfadeEngine.currentFadeMs,
                            currentDurationMs = player.duration
                        )
                    }
                })
            }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
        MiniMusicWidgetProvider.updateFromPlayer(this, player)

        // Apply the persisted Audio settings the moment the player exists, then
        // keep applying them live whenever the user toggles a setting.
        settingsRepository = SettingsRepository(this)
        serviceScope.launch {
            settingsRepository.settings.collect { settings ->
                monoProcessor.setEnabled(settings.monoAudio)
                crossfadeEngine.configure(
                    enabledSecondsMs = if (settings.crossfadeEnabled) settings.crossfadeSeconds * 1000L else 0L,
                    currentDurationMs = player.duration
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_WIDGET -> mediaSession?.player?.let { MiniMusicWidgetProvider.updateFromPlayer(this, it) }
            MiniMusicWidgetProvider.ACTION_PREVIOUS -> mediaSession?.player?.seekToPreviousMediaItem()
            MiniMusicWidgetProvider.ACTION_PLAY_PAUSE -> mediaSession?.player?.let { player ->
                if (player.isPlaying) player.pause() else player.play()
            }
            MiniMusicWidgetProvider.ACTION_NEXT -> mediaSession?.player?.seekToNextMediaItem()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Stop-on-dismiss: when the user swipes the app away, either keep playing
        // (default) or stop and tear down the foreground service per the setting.
        val stopOnDismiss = runCatching {
            kotlinx.coroutines.runBlocking { settingsRepository.settings.first().stopOnDismiss }
        }.getOrDefault(false)
        if (stopOnDismiss && (player == null || !player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        } else if (stopOnDismiss) {
            player?.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        activePlayer = null
        crossfadeEngine = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
