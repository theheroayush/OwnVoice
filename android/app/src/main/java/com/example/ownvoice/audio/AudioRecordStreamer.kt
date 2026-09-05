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
    fun startRecording(onChunkAvailable: ((ByteArray) -> Unit)? = null): Boolean {
        if (isRecording.get()) return true

        try {
            // Try VOICE_RECOGNITION first (enables hardware noise suppression & echo cancel), fallback to MIC
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

            recordingThread = Thread({
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        // Apply 3x digital Auto-Gain Control (AGC) with soft limiting for mobile microphones
                        applyDynamicGain(buffer, readBytes, gainFactor = 2.5f)

                        synchronized(pcmOutputStream) {
                            pcmOutputStream.write(buffer, 0, readBytes)
                        }

                        // Calculate RMS amplitude for visualizer
                        val rms = calculateRms(buffer, readBytes)
                        _amplitudeFlow.value = (rms / 20000.0f).coerceIn(0.0f, 1.0f)

                        // Dispatch streaming chunk if listener attached
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
