package com.example.minimusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Linear crossfade between consecutive tracks: the outgoing track's volume ramps
 * down over the last [fadeMs] of the item and the incoming track ramps up over
 * its first [fadeMs], implemented as a positional gain AudioProcessor installed
 * in the audio sink chain. The player's own position drives the curve.
 *
 * Silence-safety rules (the reason crossfade used to mute songs entirely):
 * - The engine only ever attenuates while the configured duration is KNOWN
 *   (> 0) and the reported position is inside the current item's timeline
 *   (0..duration). A stale or reset position outside that range — exactly what
 *   Media3 reports for a few frames right after an item transition — must read
 *   as full gain, never as zero.
 * - While the player has not yet confirmed a real duration for the current
 *   item (duration still TIME_UNSET at the very start of a track), the engine
 *   is inactive and passes audio through untouched.
 */
class CrossfadeEngine(private val positionProvider: () -> Long) : BaseAudioProcessor() {

    @Volatile
    private var fadeMs: Long = 0L

    @Volatile
    private var durationMs: Long = 0L

    /** Currently configured fade length in ms (0 = disabled); exposed for re-configuration. */
    val currentFadeMs: Long get() = fadeMs

    fun configure(enabledSecondsMs: Long, currentDurationMs: Long) {
        fadeMs = enabledSecondsMs.coerceAtLeast(0L)
        durationMs = currentDurationMs.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    override fun isActive(): Boolean =
        fadeMs > 0 && durationMs > fadeMs * 2

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        val out = replaceOutputBuffer(remaining)
        val gain = if (isActive()) currentGain() else 1f
        if (gain >= 0.999f) {
            out.put(inputBuffer).flip()
            return
        }
        while (inputBuffer.remaining() >= 2) {
            val sample = inputBuffer.short
            out.putShort((sample * gain).toInt().toShort())
        }
        // Trailing partial byte should not occur with 16-bit PCM.
        while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        out.flip()
    }

    /**
     * Mix of both fade styles: fade-in over the first [fadeMs] of a track and
     * fade-out over the last [fadeMs], peaking at full volume in between.
     *
     * Any position outside the configured item window (negative, or beyond the
     * duration — a leftover read during a transition) yields full gain so the
     * processor can never sustain digital silence over a playing track.
     */
    private fun currentGain(): Float {
        val positionMs = positionProvider()
        if (positionMs !in 0..durationMs) return 1f
        val fadeStart = durationMs - fadeMs
        return when {
            positionMs < fadeMs -> (positionMs.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)
            positionMs > fadeStart -> 1f - min((positionMs - fadeStart).toFloat() / fadeMs.toFloat(), 1f)
            else -> 1f
        }
    }
}
