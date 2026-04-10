package it.lmqv.livematchcam.services.stream.audio

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import com.pedro.encoder.input.audio.CustomAudioEffect
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge

/**
 * A [CustomAudioEffect] that mirrors the incoming microphone PCM data
 * to an [AudioTrack] for real-time monitoring through headphones / speakers.
 *
 * This effect is a **pass-through**: it never modifies the PCM buffer
 * delivered to the encoder, it only writes a copy to the playback track.
 *
 * Thread-safety: [process] is called on the MicrophoneManager recording
 * thread. [AudioTrack.write] with [AudioTrack.WRITE_NON_BLOCKING] ensures
 * production is never stalled by the playback side.
 */
class AudioMonitorEffect : CustomAudioEffect() {

    @Volatile
    private var enabled = false
    private var audioTrack: AudioTrack? = null
    private var outputDevice: AudioDeviceInfo? = null

    /**
     * Start audio monitoring playback.
     *
     * @param sampleRate   Sample rate matching the AudioRecord source (e.g. 44100).
     * @param isStereo     Whether the source is stereo.
     * @param outputDevice Optional preferred output device (headphones).
     */
    fun start(sampleRate: Int, isStereo: Boolean, outputDevice: AudioDeviceInfo? = null) {
        if (enabled) return
        Logd("AudioMonitorEffect :: start sampleRate=$sampleRate stereo=$isStereo")

        this.outputDevice = outputDevice
        val channelConfig = if (isStereo)
            AudioFormat.CHANNEL_OUT_STEREO
        else
            AudioFormat.CHANNEL_OUT_MONO

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            val builder = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            audioTrack = builder.build().also { track ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    track.setPreferredDevice(outputDevice)
                }
                track.play()
            }
            enabled = true
        } catch (e: Exception) {
            Loge("AudioMonitorEffect :: Failed to create AudioTrack: ${e.message}")
            audioTrack = null
            enabled = false
        }
    }

    /**
     * Stop monitoring playback and release the AudioTrack.
     */
    fun stop() {
        if (!enabled) return
        Logd("AudioMonitorEffect :: stop")
        enabled = false
        try {
            audioTrack?.stop()
        } catch (_: IllegalStateException) {
            // Already stopped
        }
        audioTrack?.release()
        audioTrack = null
    }

    /**
     * Change the output device while monitoring is active.
     * Has no effect if monitoring is not running.
     */
    fun setOutputDevice(device: AudioDeviceInfo?) {
        outputDevice = device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack?.setPreferredDevice(device)
        }
    }

    fun isEnabled(): Boolean = enabled

    /**
     * Release all resources. Call from Service.onDestroy().
     */
    fun release() {
        stop()
    }

    // --- CustomAudioEffect ---

    private var lastLogTime = 0L

    override fun process(pcmBuffer: ByteArray): ByteArray {
        if (enabled) {
            val track = audioTrack
            if (track != null) {
                // write to audio track with non-blocking mode for lowest latency
                val written = track.write(pcmBuffer, 0, pcmBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
                
                if (written < 0) {
                    Loge("AudioMonitorEffect :: write error: $written")
                } else if (written == 0) {
                    // Buffer might be full, or track might be in wrong state
                    if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        try { track.play() } catch (_: Exception) {}
                    }
                }
                
                // Log every second to avoid spamming logcat
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 1000) {
                    Logd("AudioMonitorEffect :: process called, bufferSize=${pcmBuffer.size}, written=$written")
                    lastLogTime = now
                }
            } else {
                val now = System.currentTimeMillis()
                if (now - lastLogTime > 1000) {
                    Logd("AudioMonitorEffect :: process called but audioTrack is null")
                    lastLogTime = now
                }
            }
        }
        return pcmBuffer // pass-through: never modify the stream data
    }
}
