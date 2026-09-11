package com.example.minimusic.playback

import android.content.Context
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
import com.example.minimusic.widget.MiniMusicWidgetProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

        const val ACTION_FADE_OUT = "com.example.minimusic.action.FADE_OUT"
        const val ACTION_FADE_IN = "com.example.minimusic.action.FADE_IN"
        const val EXTRA_FADE_DURATION_MS = "fade_duration_ms"

        const val ACTION_SET_MONO = "com.example.minimusic.action.SET_MONO"
        const val EXTRA_MONO_ENABLED = "mono_enabled"
    }

    private val freshShuffleCommand = SessionCommand(ACTION_FRESH_SHUFFLE, Bundle())
    private val applyShuffleOrderCommand = SessionCommand(ACTION_APPLY_SHUFFLE_ORDER, Bundle())
    private val fadeOutCommand = SessionCommand(ACTION_FADE_OUT, Bundle())
    private val fadeInCommand = SessionCommand(ACTION_FADE_IN, Bundle())
    private val setMonoCommand = SessionCommand(ACTION_SET_MONO, Bundle())
    private var mediaSession: MediaSession? = null
    private var monoEnabled = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeJob: Job? = null

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(freshShuffleCommand)
                .add(applyShuffleOrderCommand)
                .add(fadeOutCommand)
                .add(fadeInCommand)
                .add(setMonoCommand)
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
            if (customCommand.customAction == ACTION_APPLY_SHUFFLE_ORDER) {                val requestedOrder = args.getIntArray(EXTRA_SHUFFLE_ORDER)
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
            if (customCommand.customAction == ACTION_FADE_OUT) {
                startVolumeRamp(
                    targetVolume = 0f,
                    durationMs = args.getLong(EXTRA_FADE_DURATION_MS, 0L)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == ACTION_FADE_IN) {
                startVolumeRamp(
                    targetVolume = 1f,
                    durationMs = args.getLong(EXTRA_FADE_DURATION_MS, 0L)
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == ACTION_SET_MONO) {
                setMonoEnabled(args.getBoolean(EXTRA_MONO_ENABLED, false))
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val player = buildPlayer(monoEnabled = monoEnabled)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
        MiniMusicWidgetProvider.updateFromPlayer(this, player)
    }

    private fun buildPlayer(monoEnabled: Boolean): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this, MonoCapableRenderersFactory(this, monoEnabled))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
            .also { exoPlayer ->
                exoPlayer.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        MiniMusicWidgetProvider.requestUpdate(this@MusicService)
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        // Catch-all against stuck-low volume (pause mid-fade,
                        // seek-back, dismissed queue): whenever playback starts
                        // and no ramp is running, the level must be full.
                        if (isPlaying && fadeJob?.isActive != true) {
                            exoPlayer.volume = 1f
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            fadeJob?.cancel()
                            exoPlayer.volume = 1f
                        }
                    }
                })
            }
        return player
    }

    /**
     * Linear volume ramp on the session player. A new ramp always cancels the
     * in-flight one and starts from the CURRENT level (never from an assumed
     * 0 or 1), so interrupted fades hand off seamlessly. Duration 0 snaps.
     */
    private fun startVolumeRamp(targetVolume: Float, durationMs: Long) {
        fadeJob?.cancel()
        val player = mediaSession?.player ?: return
        if (durationMs <= 0L) {
            player.volume = targetVolume.coerceIn(0f, 1f)
            return
        }
        val startVolume = player.volume
        val target = targetVolume.coerceIn(0f, 1f)
        fadeJob = serviceScope.launch {
            val steps = (durationMs / 50L).coerceIn(1L, 400L).toInt()
            repeat(steps) { step ->
                delay(50L)
                player.volume = startVolume + (target - startVolume) * (step + 1) / steps
            }
            player.volume = target
        }
    }

    /**
     * Rebuilds the player with or without the mono downmix processor. ExoPlayer
     * bakes processors in at construction, so toggling requires a fresh
     * instance — the timeline, position, mode flags and volume carry over, and
     * playback resumes in the same state. No-op when nothing changed.
     */
    private fun setMonoEnabled(enabled: Boolean) {
        if (enabled == monoEnabled && mediaSession != null) return
        monoEnabled = enabled
        val session = mediaSession ?: return
        val oldPlayer = session.player as? ExoPlayer ?: return
        val itemCount = oldPlayer.mediaItemCount
        val items = (0 until itemCount).map { oldPlayer.getMediaItemAt(it) }
        val index = oldPlayer.currentMediaItemIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0))
        val position = oldPlayer.currentPosition.coerceAtLeast(0L)
        val playWhenReady = oldPlayer.playWhenReady
        val repeat = oldPlayer.repeatMode
        val shuffle = oldPlayer.shuffleModeEnabled
        val volume = oldPlayer.volume
        session.release()
        val newPlayer = buildPlayer(monoEnabled = enabled)
        if (items.isNotEmpty()) {
            newPlayer.setMediaItems(items, index, position)
            newPlayer.prepare()
        }
        newPlayer.repeatMode = repeat
        newPlayer.shuffleModeEnabled = shuffle
        newPlayer.volume = volume
        newPlayer.playWhenReady = playWhenReady
        mediaSession = MediaSession.Builder(this, newPlayer)
            .setCallback(sessionCallback)
            .build()
        MiniMusicWidgetProvider.updateFromPlayer(this, newPlayer)
        oldPlayer.release()
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

    override fun onDestroy() {
        isRunning = false
        fadeJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

/**
 * Renderers factory identical to the default except for one switch: when mono
 * is on, the mono downmix processor joins the audio chain. Everything else
 * (float output, playback params, offload defaults) mirrors
 * [DefaultRenderersFactory.buildAudioSink] exactly.
 */
private class MonoCapableRenderersFactory(
    context: Context,
    private val monoEnabled: Boolean
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParams: Boolean
    ): AudioSink {
        val builder = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioOutputPlaybackParams)
        if (monoEnabled) {
            builder.setAudioProcessors(arrayOf(MonoAudioProcessor()))
        }
        return builder.build()
    }
}
