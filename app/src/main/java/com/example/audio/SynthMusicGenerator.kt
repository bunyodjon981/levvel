package com.example.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

object SynthMusicGenerator {

    private const val SAMPLE_RATE = 44100

    suspend fun ensureBuiltInTrackFile(
        context: Context,
        preset: String,
        bpm: Int,
        durationMs: Long
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "built_in_audio").apply { mkdirs() }
        val outFile = File(dir, "${preset}_v1.wav")
        if (outFile.exists() && outFile.length() > 4096L) {
            return@withContext outFile
        }
        generateWavFile(outFile, preset, bpm, durationMs)
        outFile
    }

    private fun generateWavFile(file: File, preset: String, bpm: Int, durationMs: Long) {
        val totalSamples = ((durationMs / 1000.0) * SAMPLE_RATE).toInt().coerceAtLeast(SAMPLE_RATE)
        val beatSec = 60.0 / bpm.coerceIn(60, 180)
        val pcm = ShortArray(totalSamples)

        val chords: Array<DoubleArray> = when (preset) {
            "neon_pulse" -> arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 440.0), // Am
                doubleArrayOf(174.61, 220.0, 261.63, 349.23), // F
                doubleArrayOf(130.81, 196.0, 261.63, 329.63), // C
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)   // G
            )
            "golden_horizon" -> arrayOf(
                doubleArrayOf(261.63, 329.63, 392.0, 493.88), // Cmaj7
                doubleArrayOf(220.0, 261.63, 329.63, 392.0),  // Am7
                doubleArrayOf(174.61, 220.0, 261.63, 329.63), // Fmaj7
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)   // G
            )
            "cinema_epic" -> arrayOf(
                doubleArrayOf(146.83, 220.0, 293.66, 349.23), // Dm
                doubleArrayOf(116.54, 174.61, 233.08, 293.66), // Bb
                doubleArrayOf(130.81, 196.0, 261.63, 329.63), // C
                doubleArrayOf(146.83, 220.0, 293.66, 440.0)   // Dm
            )
            "lofi_memory" -> arrayOf(
                doubleArrayOf(196.0, 246.94, 293.66, 369.99), // Gmaj7
                doubleArrayOf(164.81, 196.0, 246.94, 293.66), // Em7
                doubleArrayOf(220.0, 261.63, 329.63, 392.0),  // Am7
                doubleArrayOf(146.83, 220.0, 261.63, 329.63)  // D9
            )
            "trap_reels" -> arrayOf(
                doubleArrayOf(164.81, 196.0, 246.94, 329.63), // Em
                doubleArrayOf(130.81, 164.81, 196.0, 261.63), // C
                doubleArrayOf(146.83, 185.0, 220.0, 293.66),  // D
                doubleArrayOf(164.81, 196.0, 246.94, 329.63)  // Em
            )
            else -> arrayOf(
                doubleArrayOf(261.63, 329.63, 392.0, 523.25),
                doubleArrayOf(220.0, 261.63, 329.63, 440.0),
                doubleArrayOf(174.61, 220.0, 261.63, 349.23),
                doubleArrayOf(196.0, 246.94, 293.66, 392.0)
            )
        }

        var noiseSeed = 1337
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val beatFloat = t / beatSec
            val beatIndex = beatFloat.toInt()
            val beatPhase = beatFloat - beatIndex
            val barIndex = (beatIndex / 4) % chords.size
            val chord = chords[barIndex]

            // Bass note (octave down from chord root)
            val bassFreq = chord[0] * 0.5
            val bassEnv = when (preset) {
                "trap_reels", "neon_pulse" -> exp(-beatPhase * 4.5)
                else -> 0.75 + 0.25 * sin(2.0 * PI * beatPhase)
            }
            val bass = sin(2.0 * PI * bassFreq * t) * 0.30 * bassEnv

            // Warm pad / chord harmony
            var pad = 0.0
            for (n in chord.indices) {
                val freq = chord[n]
                pad += sin(2.0 * PI * freq * t) * 0.08
            }

            // Melodic arpeggio / pluck on 8th notes
            val eighthFloat = beatFloat * 2.0
            val eighthIdx = eighthFloat.toInt()
            val eighthPhase = eighthFloat - eighthIdx
            val arpFreq = chord[eighthIdx % chord.size] * 2.0
            val arpEnv = exp(-eighthPhase * when (preset) {
                "lofi_memory" -> 5.0
                "minimal_ambient" -> 7.0
                else -> 6.0
            })
            val arp = (sin(2.0 * PI * arpFreq * t) + 0.25 * sin(4.0 * PI * arpFreq * t)) * 0.14 * arpEnv

            // Kick drum on beat 0 & 2 (or every beat for neon_pulse)
            val isKickBeat = when (preset) {
                "neon_pulse" -> true
                "cinema_epic" -> (beatIndex % 4 == 0 || beatIndex % 4 == 2)
                else -> (beatIndex % 4 == 0 || beatIndex % 4 == 2)
            }
            val kick = if (isKickBeat && beatPhase < 0.25) {
                val kickTime = beatPhase * beatSec
                val kickFreq = 48.0 + 110.0 * exp(-kickTime * 35.0)
                sin(2.0 * PI * kickFreq * kickTime) * exp(-kickTime * 16.0) * 0.42
            } else 0.0

            // Hi-hat / snare noise burst
            noiseSeed = (noiseSeed * 1103515245 + 12345) and 0x7fffffff
            val whiteNoise = (noiseSeed / 1073741824.0) - 1.0
            val hatPhase = (beatFloat * 2.0) - (beatFloat * 2.0).toInt()
            val hiHat = if (preset != "cinema_epic" && hatPhase < 0.08) {
                whiteNoise * exp(-hatPhase * 45.0) * 0.09
            } else 0.0

            val snare = if ((beatIndex % 4 == 1 || beatIndex % 4 == 3) && beatPhase < 0.16) {
                whiteNoise * exp(-beatPhase * 24.0) * 0.18
            } else 0.0

            // Smooth overall fade-in/out at track boundaries
            val fadeIn = (t / 0.25).coerceIn(0.0, 1.0)
            val fadeOut = (((durationMs / 1000.0) - t) / 0.4).coerceIn(0.0, 1.0)
            val mixed = tanh((bass + pad + arp + kick + hiHat + snare) * 1.1) * fadeIn * fadeOut
            pcm[i] = (mixed * 29000.0).toInt().coerceIn(-32767, 32767).toShort()
        }

        writeWavHeaderAndData(file, pcm)
    }

    private fun writeWavHeaderAndData(file: File, pcm: ShortArray) {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // PCM chunk size
        header.putShort(1) // PCM format
        header.putShort(1) // mono
        header.putInt(SAMPLE_RATE)
        header.putInt(SAMPLE_RATE * 2)
        header.putShort(2) // block align
        header.putShort(16) // bits per sample
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        BufferedOutputStream(FileOutputStream(file)).use { out ->
            out.write(header.array())
            val buf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            var idx = 0
            while (idx < pcm.size) {
                buf.clear()
                while (idx < pcm.size && buf.remaining() >= 2) {
                    buf.putShort(pcm[idx++])
                }
                out.write(buf.array(), 0, buf.position())
            }
        }
    }
}
