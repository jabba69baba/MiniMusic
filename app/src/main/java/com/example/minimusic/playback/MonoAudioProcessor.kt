package com.example.minimusic.playback

import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Downmixes stereo (or multi-channel) PCM to mono by averaging channels.
 * Toggled live via [enabled] from the Audio settings; when disabled or
 * unconfigured the processor passes input through untouched.
 */
class MonoAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var enabled: Boolean = false

    private var channelCount = 0

    override fun onConfigure(inputAudioFormat: androidx.media3.common.audio.AudioProcessor.AudioFormat): androidx.media3.common.audio.AudioProcessor.AudioFormat {
        channelCount = inputAudioFormat.channelCount
        return if (enabled && channelCount >= 2) inputAudioFormat
        else androidx.media3.common.audio.AudioProcessor.AudioFormat.NOT_SET_SPECIFIED
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (!enabled || channelCount < 2) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer).flip()
            return
        }
        val bytesPerFrame = channelCount * 2 // 16-bit PCM
        val out = replaceOutputBuffer(remaining / bytesPerFrame * 2)
        while (inputBuffer.remaining() >= bytesPerFrame) {
            var sumL = 0
            var sumR = 0
            for (ch in 0 until channelCount) {
                val sample = inputBuffer.short
                when (ch) {
                    0 -> sumL = sample.toInt()
                    1 -> sumR = sample.toInt()
                    else -> { sumL += sample / 2; sumR += sample / 2 }
                }
            }
            out.putShort(((sumL + sumR) / 2).toShort())
        }
        // Trailing partial frame (shouldn't occur with PCM) passes through.
        while (inputBuffer.hasRemaining()) out.put(inputBuffer.get())
        out.flip()
    }
}
