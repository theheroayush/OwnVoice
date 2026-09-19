package com.example.ownvoice.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

class AudioRecordStreamer(
    private val sampleRate: Int = 16000
) {
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val bufferSize = maxOf(minBufferSize, 2048)

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    private val _amplitudeFlow = MutableStateFlow(0.0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val pcmOutputStream = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording(
        sensitivity: Float = 0.5f,
        silenceDurationSeconds: Float = 0f,
        onChunkAvailable: ((ByteArray) -> Unit)? = null,
        onSilenceDetected: (() -> Unit)? = null
    ): Boolean {
        if (isRecording.get()) return true

        try {
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (ignored: Exception) {}

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            audioRecord = record
            pcmOutputStream.reset()
            isRecording.set(true)
            audioRecord?.startRecording()

            // Dynamic gain from sensitivity slider (0.0 to 1.0 -> 1.0x to 4.0x)
            val dynamicGain = 1.0f + (sensitivity.coerceIn(0.0f, 1.0f) * 3.0f)

            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                var hasSpoken = false
                var silenceStartTime = 0L
                val silenceThreshold = 400.0f // RMS threshold for silence

                while (isRecording.get()) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        applyDynamicGain(buffer, readBytes, gainFactor = dynamicGain)

                        synchronized(pcmOutputStream) {
                            pcmOutputStream.write(buffer, 0, readBytes)
                        }

                        val rms = calculateRms(buffer, readBytes)
                        _amplitudeFlow.value = (rms / 20000.0f).coerceIn(0.0f, 1.0f)

                        // VAD / Silence detection
                        if (silenceDurationSeconds > 0.5f && onSilenceDetected != null) {
                            if (rms > silenceThreshold * 1.5f) {
                                hasSpoken = true
                                silenceStartTime = 0L
                            } else if (hasSpoken) {
                                val now = System.currentTimeMillis()
                                if (silenceStartTime == 0L) {
                                    silenceStartTime = now
                                } else if (now - silenceStartTime >= (silenceDurationSeconds * 1000).toLong()) {
                                    // Trigger silence stop callback on separate thread
                                    onSilenceDetected.invoke()
                                    break
                                }
                            }
                        }

                        onChunkAvailable?.invoke(buffer.copyOf(readBytes))
                    }
                }
                _amplitudeFlow.value = 0.0f
            }, "OwnVoiceAudioRecordThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            return true
        } catch (e: Exception) {
            isRecording.set(false)
            audioRecord?.release()
            audioRecord = null
            return false
        }
    }

    fun stopRecording(): ByteArray {
        isRecording.set(false)
        try {
            recordingThread?.join(500)
        } catch (ignored: InterruptedException) {}
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null

        val pcmBytes: ByteArray
        synchronized(pcmOutputStream) {
            pcmBytes = pcmOutputStream.toByteArray()
            pcmOutputStream.reset()
        }

        if (pcmBytes.isEmpty()) return ByteArray(0)
        return WavEncoder.encodePcmToWav(pcmBytes, sampleRate)
    }

    fun cancelRecording() {
        isRecording.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        synchronized(pcmOutputStream) {
            pcmOutputStream.reset()
        }
        _amplitudeFlow.value = 0.0f
    }

    fun isCurrentlyRecording(): Boolean = isRecording.get()

    private fun applyDynamicGain(buffer: ByteArray, length: Int, gainFactor: Float) {
        var i = 0
        while (i < length - 1) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            val boosted = (sample * gainFactor).toInt().coerceIn(-32768, 32767).toShort()
            buffer[i] = (boosted.toInt() and 0xFF).toByte()
            buffer[i + 1] = ((boosted.toInt() shr 8) and 0xFF).toByte()
            i += 2
        }
    }

    private fun calculateRms(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        var count = 0
        var i = 0
        while (i < length - 1) {
            val sample = ((buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)).toShort()
            sum += sample * sample
            count++
            i += 2
        }
        return if (count > 0) sqrt(sum / count).toFloat() else 0.0f
    }
}
