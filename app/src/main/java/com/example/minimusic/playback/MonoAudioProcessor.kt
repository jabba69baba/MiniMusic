package com.example.minimusic.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Downmixes PCM audio to a single mono channel by averaging samples. Only
 * 16-bit PCM with 2+ channels is actually mixed (the format virtually every
 * local decoder emits); anything else passes through byte-identical so the
 * toggle can never break playback of an exotic file — mono there is simply a
 * no-op. Best-effort by design, documented in settings.
 */
class MonoAudioProcessor : BaseAudioProcessor() {

    private var downmixActive = false

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        downmixActive =
            inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount > 1
        return if (downmixActive) {
            AudioProcessor.AudioFormat(
                inputAudioFormat.sampleRate,
                /* channelCount= */ 1,
                inputAudioFormat.encoding
            )
        } else {
            inputAudioFormat
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        if (!downmixActive) {
            if (limit > position) {
                val output = replaceOutputBuffer(limit - position)
                val copy = inputBuffer.duplicate()
                output.put(copy)
                output.flip()
                inputBuffer.position(limit)
            }
            return
        }
        val channels = inputAudioFormat.channelCount
        val frameBytes = channels * 2
        val frames = (limit - position) / frameBytes
        if (frames == 0) return
        val output = replaceOutputBuffer(frames * 2)
        var index = position
        repeat(frames) {
            var sum = 0
            repeat(channels) {
                sum += inputBuffer.getShort(index).toInt()
                index += 2
            }
            output.putShort((sum / channels).toShort())
        }
        inputBuffer.position(index)
        output.flip()
    }

    override fun onReset() {
        downmixActive = false
    }
}
