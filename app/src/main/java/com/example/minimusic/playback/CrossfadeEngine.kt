package com.example.minimusic.playback

import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * Linear crossfade between consecutive tracks: the outgoing track's volume ramps
 * down over the last [fadeMs] of the item and the incoming track ramps up over
 * its first [fadeMs], implemented as a positional gain AudioProcessor installed
 * in the audio sink chain. The player's own position drives the curve.
 */
class CrossfadeEngine(private val positionProvider: () -> Long) : BaseAudioProcessor() {

    private var fadeMs: Long = 0L
    private var durationMs: Long = 0L

    /** Currently configured fade length in ms (0 = disabled); exposed for re-configuration. */
    val currentFadeMs: Long get() = fadeMs

    fun configure(enabledSecondsMs: Long, currentDurationMs: Long) {
        fadeMs = enabledSecondsMs
        durationMs = currentDurationMs
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    override fun isActive(): Boolean = fadeMs > 0 && durationMs > fadeMs * 2

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        val gain = if (isActive()) currentGain() else 1f
        val out = replaceOutputBuffer(remaining)
        if (gain >= 1f) {
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
     */
    private fun currentGain(): Float {
        val positionMs = positionProvider().coerceAtLeast(0L)
        val fadeStart = durationMs - fadeMs
        return when {
            positionMs < fadeMs -> positionMs.toFloat() / fadeMs.toFloat()
            positionMs > fadeStart -> 1f - min((positionMs - fadeStart).toFloat() / fadeMs.toFloat(), 1f)
            else -> 1f
        }
    }
}
