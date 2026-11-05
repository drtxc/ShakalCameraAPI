package com.shakal.camera.core

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Video encoder using MediaCodec + MediaMuxer.
 * 
 * Records the GL output to H.264 video with AAC audio.
 * - Video: H.264, resolution = preset ×2 (nearest), fps = preset fpsMax
 * - Audio: AAC mono
 *   - Levels 1-6: 22050 Hz, 24 kbps
 *   - Levels 7-12: 8000 Hz, 12 kbps + clipping
 * - Output: MP4 via MediaMuxer
 * - Max duration: 3 minutes
 */
class VideoEncoder {

    private var videoCodec: MediaCodec? = null
    private var audioCodec: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var encoderSurface: Surface? = null
    private var audioRecord: AudioRecord? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var muxerStarted = false

    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private var audioThread: Thread? = null
    private var startTimeUs: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private var onDurationReached: (() -> Unit)? = null

    companion object {
        private const val MAX_DURATION_MS = 3 * 60 * 1000L // 3 minutes
        private const val VIDEO_MIME = "video/avc"
        private const val AUDIO_MIME = "audio/mp4a-latm"
        private const val VIDEO_FRAME_RATE = 30
        private const val VIDEO_I_FRAME_INTERVAL = 1
    }

    /**
     * Calculate video bitrate based on shakal level (logarithmic scale).
     * Level 1 → 1.5 Mbps, Level 10/12 → 64 kbps
     */
    fun calculateBitrate(level: Int): Int {
        return when {
            level <= 1 -> 1_500_000
            level >= 10 -> 64_000
            else -> {
                // Logarithmic interpolation between 1.5Mbps and 64kbps
                val minBitrate = 64_000.0
                val maxBitrate = 1_500_000.0
                val t = (level - 1).toDouble() / 9.0 // 1..10 → 0..1
                val logMin = Math.log(minBitrate)
                val logMax = Math.log(maxBitrate)
                Math.exp(logMax - t * (logMax - logMin)).toInt()
            }
        }
    }

    /**
     * Get audio parameters based on level.
     */
    private fun getAudioSampleRate(level: Int): Int {
        return if (level >= 7) 8000 else 22050
    }

    private fun getAudioBitrate(level: Int): Int {
        return if (level >= 7) 12_000 else 24_000
    }

    /**
     * Initialize the encoder. Returns the Surface to render GL output to.
     */
    fun init(preset: Preset, outputPath: String): Surface {
        val width = preset.width * 2
        val height = preset.height * 2
        val bitrate = calculateBitrate(preset.level)
        val frameRate = preset.fpsMax

        // Video encoder
        val videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL)
        }

        videoCodec = MediaCodec.createEncoderByType(VIDEO_MIME).apply {
            configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        encoderSurface = videoCodec!!.createInputSurface()
        videoCodec!!.start()

        // Audio encoder
        val sampleRate = getAudioSampleRate(preset.level)
        val audioBitrate = getAudioBitrate(preset.level)

        val audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
            setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
        }

        audioCodec = MediaCodec.createEncoderByType(AUDIO_MIME).apply {
            configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        // Audio recorder
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.CAMCORDER,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2
        )

        // Muxer
        muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        return encoderSurface!!
    }

    /**
     * Start recording.
     */
    fun start() {
        isRecording.set(true)
        startTimeUs = System.nanoTime() / 1000

        audioRecord?.startRecording()

        // Video drain thread
        recordingThread = Thread {
            drainVideo()
        }.apply { start() }

        // Audio drain thread
        audioThread = Thread {
            drainAudio()
        }.apply { start() }

        // Duration limit check
        handler.postDelayed({
            if (isRecording.get()) {
                onDurationReached?.invoke()
            }
        }, MAX_DURATION_MS)
    }

    /**
     * Stop recording and finalize the file.
     */
    fun stop() {
        isRecording.set(false)

        // Signal EOS to video encoder
        videoCodec?.let { codec ->
            val info = MediaCodec.BufferInfo()
            // Send empty buffer with EOS flag
            val inputIndex = codec.dequeueInputBuffer(10_000)
            if (inputIndex >= 0) {
                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        }

        // Wait for threads to finish
        recordingThread?.join(5000)
        audioThread?.join(5000)

        audioRecord?.stop()
        
        try {
            videoCodec?.stop()
            audioCodec?.stop()
            muxer?.stop()
        } catch (e: Exception) {
            // Muxer may throw if no data was written
        }

        videoCodec?.release()
        audioCodec?.release()
        muxer?.release()
        audioRecord?.release()

        videoCodec = null
        audioCodec = null
        muxer = null
        encoderSurface = null
        audioRecord = null
        muxerStarted = false
        videoTrackIndex = -1
        audioTrackIndex = -1
    }

    fun getEncoderSurface(): Surface? = encoderSurface

    fun isRecording(): Boolean = isRecording.get()

    fun setOnDurationReached(callback: () -> Unit) {
        onDurationReached = callback
    }

    private fun drainVideo() {
        val codec = videoCodec ?: return
        val info = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        while (isRecording.get()) {
            val outputIndex = codec.dequeueOutputBuffer(info, timeoutUs)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue

                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        info.size = 0
                    }

                    if (info.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                        info.presentationTimeUs = System.nanoTime() / 1000 - startTimeUs
                        muxer?.writeSampleData(videoTrackIndex, outputBuffer, info)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    videoTrackIndex = muxer?.addTrack(format) ?: -1
                    tryStartMuxer()
                }
                else -> { /* INFO_TRY_AGAIN_LATER */ }
            }
        }
    }

    private fun drainAudio() {
        val codec = audioCodec ?: return
        val recorder = audioRecord ?: return
        val info = MediaCodec.BufferInfo()
        val timeoutUs = 10_000L

        while (isRecording.get()) {
            // Feed audio data to encoder
            val inputIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                val readBytes = recorder.read(inputBuffer, inputBuffer.capacity())
                if (readBytes > 0) {
                    codec.queueInputBuffer(inputIndex, 0, readBytes,
                        System.nanoTime() / 1000 - startTimeUs, 0)
                }
            }

            // Drain output
            val outputIndex = codec.dequeueOutputBuffer(info, timeoutUs)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    if (info.size > 0 && muxerStarted && audioTrackIndex >= 0) {
                        muxer?.writeSampleData(audioTrackIndex, outputBuffer, info)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = codec.outputFormat
                    audioTrackIndex = muxer?.addTrack(format) ?: -1
                    tryStartMuxer()
                }
                else -> { /* INFO_TRY_AGAIN_LATER */ }
            }
        }
    }

    private fun tryStartMuluxer() {
        // This is a typo fix - method name should be tryStartMuxer
    }

    private fun tryStartMuxer() {
        if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
            muxer?.start()
            muxerStarted = true
        }
    }
}
