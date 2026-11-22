package app.nearwave.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Plays incoming voice through the loudspeaker (or headphones if plugged in).
 * A small queue absorbs network jitter; when the queue backs up, the oldest audio is dropped
 * so we stay close to real time.
 */
class VoicePlayer {

    private val queue = LinkedBlockingQueue<ByteArray>(MAX_QUEUED_FRAMES)
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "nearwave-speaker").also { it.start() }
    }

    fun stop() {
        running = false
        thread?.interrupt()
        thread = null
        queue.clear()
    }

    fun feed(ulaw: ByteArray) {
        while (!queue.offer(ulaw)) queue.poll()
    }

    private fun loop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val minBuffer = AudioTrack.getMinBufferSize(AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_OUT, AudioConfig.ENCODING)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioConfig.ENCODING)
                        .setSampleRate(AudioConfig.SAMPLE_RATE)
                        .setChannelMask(AudioConfig.CHANNEL_OUT)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, AudioConfig.FRAME_SAMPLES * 2 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "speaker unavailable", e)
            return
        }

        var playing = false
        var lastFrameAt = 0L
        try {
            while (running) {
                val frame = queue.poll(250, TimeUnit.MILLISECONDS)
                if (frame == null) {
                    if (playing && System.currentTimeMillis() - lastFrameAt > IDLE_STOP_MS) {
                        track.pause()
                        track.flush()
                        playing = false
                    }
                    continue
                }
                if (!playing) {
                    // Let two frames pile up first so the start of a sentence doesn't stutter.
                    Thread.sleep(AudioConfig.FRAME_MS.toLong() * 2)
                    track.play()
                    playing = true
                }
                lastFrameAt = System.currentTimeMillis()
                val pcm = MuLaw.decode(frame)
                track.write(pcm, 0, pcm.size)
            }
        } catch (e: InterruptedException) {
            // Shutting down.

        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    companion object {
        private const val TAG = "VoicePlayer"
        private const val MAX_QUEUED_FRAMES = 8 // about 320 ms
        private const val IDLE_STOP_MS = 1_000L
    }
}
