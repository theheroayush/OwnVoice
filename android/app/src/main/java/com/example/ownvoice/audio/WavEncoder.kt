package com.example.ownvoice.audio

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {

    fun encodePcmToWav(pcmData: ByteArray, sampleRate: Int = 16000, channels: Short = 1, bitsPerSample: Short = 16): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            // RIFF header
            put("RIFF".toByteArray())
            putInt(totalDataLen)
            put("WAVE".toByteArray())

            // fmt subchunk
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size for PCM
            putShort(1) // AudioFormat (1 = PCM)
            putShort(channels)
            putInt(sampleRate)
            putInt(byteRate)
            putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
            putShort(bitsPerSample)

            // data subchunk
            put("data".toByteArray())
            putInt(totalAudioLen)
        }.array()

        val output = ByteArrayOutputStream(header.size + pcmData.size)
        output.write(header)
        output.write(pcmData)
        return output.toByteArray()
    }
}
