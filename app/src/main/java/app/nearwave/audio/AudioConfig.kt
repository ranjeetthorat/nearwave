package app.nearwave.audio

import android.media.AudioFormat

/**
 * Voice format for v0.1: 16 kHz mono, compressed with G.711 mu-law (8 bits per sample).
 * That is 128 kbps: simple, zero-dependency and clear. If voice stutters at long range,
 * drop SAMPLE_RATE to 8_000 (64 kbps). Opus is the planned upgrade (about 16 kbps).
 */
object AudioConfig {
    const val SAMPLE_RATE = 16_000
    const val FRAME_MS = 40
    const val FRAME_SAMPLES = SAMPLE_RATE * FRAME_MS / 1000
    const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
    const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    /** Safety cap so a stuck button never transmits forever. */
    const val MAX_TALK_MS = 60_000L
}
