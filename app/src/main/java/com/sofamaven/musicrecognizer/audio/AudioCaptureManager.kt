package com.sofamaven.musicrecognizer.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.sofamaven.musicrecognizer.data.model.AudioInputSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.max

class AudioCaptureManager(private val context: Context) {

    companion object {
        const val RECORDING_DURATION_MS = 7_000L // 7 seconds
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    /**
     * Records audio for exactly 7 seconds, writes to a temporary WAV file,
     * immediately releases the microphone to free Android TV system resources,
     * and invokes onProgressUpdate every second for the UI countdown.
     */
    @SuppressLint("MissingPermission")
    suspend fun captureAudioClip(
        source: AudioInputSource,
        onCountdownTick: (remainingSeconds: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "temp_music_clip_${System.currentTimeMillis()}.wav")

        val audioSource = when (source) {
            AudioInputSource.REMOTE_MIC -> {
                // On Android TV, VOICE_RECOGNITION or MIC targets remote microphone
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaRecorder.AudioSource.VOICE_RECOGNITION
                } else {
                    MediaRecorder.AudioSource.MIC
                }
            }
            AudioInputSource.USB_STICK -> {
                // Fixed AudioSource.MIC routed to USB audio on TV
                MediaRecorder.AudioSource.MIC
            }
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = max(minBufferSize * 2, 4096)

        var audioRecord: AudioRecord? = null
        var outputStream: FileOutputStream? = null

        try {
            audioRecord = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext Result.failure(
                    IllegalStateException("Не удалось инициализировать микрофон. Проверьте подключение и разрешения.")
                )
            }

            outputStream = FileOutputStream(outputFile)
            // Write placeholder 44-byte WAV header (will be filled after recording)
            writeWavHeader(outputStream, 0, 0, SAMPLE_RATE, 1, 16)

            audioRecord.startRecording()

            val buffer = ByteArray(bufferSize)
            var totalAudioBytesWritten = 0L
            val startTime = System.currentTimeMillis()
            var lastReportedSecond = -1

            while (isActive && (System.currentTimeMillis() - startTime) < RECORDING_DURATION_MS) {
                val elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                val remainingSeconds = 7 - elapsedSeconds
                if (remainingSeconds != lastReportedSecond && remainingSeconds in 1..7) {
                    lastReportedSecond = remainingSeconds
                    onCountdownTick(remainingSeconds)
                }

                val readBytes = audioRecord.read(buffer, 0, buffer.size)
                if (readBytes > 0) {
                    outputStream.write(buffer, 0, readBytes)
                    totalAudioBytesWritten += readBytes
                } else if (readBytes < 0) {
                    break
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Update WAV header with final size
            updateWavHeaderSizes(outputFile, totalAudioBytesWritten)

            Result.success(outputFile)
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(Exception("Ошибка записи звука: ${e.localizedMessage ?: "Сбой микрофона"}"))
        } finally {
            // CRITICAL: Immediately release the microphone to avoid blocking OS audio resources
            try {
                outputStream?.close()
            } catch (_: Exception) {}

            try {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
            } catch (_: Exception) {}

            try {
                audioRecord?.release()
            } catch (_: Exception) {}
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Int,
        channels: Int,
        byteRate: Int
    ) {
        val header = ByteArray(44)
        val bitsPerSample = 16
        val subChunk2Size = totalAudioLen
        val chunkSize = 36 + subChunk2Size
        val byteRateCalc = longSampleRate * channels * bitsPerSample / 8

        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (chunkSize and 0xff).toByte()
        header[5] = (chunkSize shr 8 and 0xff).toByte()
        header[6] = (chunkSize shr 16 and 0xff).toByte()
        header[7] = (chunkSize shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRateCalc and 0xff).toByte()
        header[29] = (byteRateCalc shr 8 and 0xff).toByte()
        header[30] = (byteRateCalc shr 16 and 0xff).toByte()
        header[31] = (byteRateCalc shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (subChunk2Size and 0xff).toByte()
        header[41] = (subChunk2Size shr 8 and 0xff).toByte()
        header[42] = (subChunk2Size shr 16 and 0xff).toByte()
        header[43] = (subChunk2Size shr 24 and 0xff).toByte()

        out.write(header, 0, 44)
    }

    private fun updateWavHeaderSizes(file: File, totalAudioBytes: Long) {
        val chunkSize = totalAudioBytes + 36
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4)
            raf.write(
                byteArrayOf(
                    (chunkSize and 0xff).toByte(),
                    (chunkSize shr 8 and 0xff).toByte(),
                    (chunkSize shr 16 and 0xff).toByte(),
                    (chunkSize shr 24 and 0xff).toByte()
                )
            )
            raf.seek(40)
            raf.write(
                byteArrayOf(
                    (totalAudioBytes and 0xff).toByte(),
                    (totalAudioBytes shr 8 and 0xff).toByte(),
                    (totalAudioBytes shr 16 and 0xff).toByte(),
                    (totalAudioBytes shr 24 and 0xff).toByte()
                )
            )
        }
    }
}
