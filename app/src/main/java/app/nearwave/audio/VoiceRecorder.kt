package app.nearwave.audio

import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import android.util.Log

/** Captures the microphone while the talk button is held and hands out 40 ms mu-law frames. */
class VoiceRecorder(private val onFrame: (ByteArray) -> Unit) {

    @Volatile private var running = false
    private var thread: Thread? = null

    @SuppressLint("MissingPermission") // RECORD_AUDIO is requested during setup.
    fun start(): Boolean {
        if (running) return true
        val minBuffer = AudioRecord.getMinBufferSize(AudioConfig.SAMPLE_RATE, AudioConfig.CHANNEL_IN, AudioConfig.ENCODING)
        if (minBuffer <= 0) return false
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                AudioConfig.SAMPLE_RATE,
                AudioConfig.CHANNEL_IN,
                AudioConfig.ENCODING,
                maxOf(minBuffer, AudioConfig.FRAME_SAMPLES * 2 * 4),
            )
        } catch (e: Exception) {
            Log.w(TAG, "mic unavailable", e)
            return false
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        if (NoiseSuppressor.isAvailable()) runCatching { NoiseSuppressor.create(record.audioSessionId)?.enabled = true }
        if (AutomaticGainControl.isAvailable()) runCatching { AutomaticGainControl.create(record.audioSessionId)?.enabled = true }

        running = true
        thread = Thread({ loop(record) }, "nearwave-mic").also { it.start() }
        return true
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    private fun loop(record: AudioRecord) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val pcm = ShortArray(AudioConfig.FRAME_SAMPLES)
        try {
            record.startRecording()
            while (running) {
                var filled = 0
                while (filled < pcm.size && running) {
                    val n = record.read(pcm, filled, pcm.size - filled)
                    if (n <= 0) break
                    filled += n
                }
                if (filled == pcm.size) onFrame(MuLaw.encode(pcm)) else if (running) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "recording stopped", e)
        } finally {
            runCatching { record.stop() }
            record.release()
            running = false
        }
    }

    companion object {
        private const val TAG = "VoiceRecorder"
    }
}
