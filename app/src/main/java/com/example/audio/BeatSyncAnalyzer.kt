package com.example.audio

import com.example.model.ClipMediaType
import com.example.model.MontageProject
import com.example.model.MusicTrack
import com.example.model.TransitionType
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object BeatSyncAnalyzer {

    fun generateBeatGrid(bpm: Int, totalDurationMs: Long): List<Long> {
        val safeBpm = bpm.coerceIn(60, 190)
        val intervalMs = (60_000.0 / safeBpm).toLong().coerceAtLeast(250L)
        val beats = mutableListOf<Long>()
        var t = 0L
        val limit = totalDurationMs.coerceAtLeast(5_000L)
        while (t <= limit) {
            beats.add(t)
            t += intervalMs
        }
        return beats
    }

    fun detectOrGenerateBeats(track: MusicTrack, targetDurationMs: Long): List<Long> {
        if (track.beatMarksMs.isNotEmpty()) return track.beatMarksMs
        val wavBeats = track.audioFilePath?.let { path ->
            val f = File(path)
            if (f.exists() && f.extension.equals("wav", ignoreCase = true)) {
                detectWavOnsetBeats(f, track.bpm)
            } else null
        }
        return if (!wavBeats.isNullOrEmpty()) wavBeats else generateBeatGrid(track.bpm, targetDurationMs)
    }

    private fun detectWavOnsetBeats(file: File, fallbackBpm: Int): List<Long> {
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= 44) return emptyList()
            val sampleRate = 44100
            val totalSamples = (bytes.size - 44) / 2
            val bb = ByteBuffer.wrap(bytes, 44, totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            val windowSamples = sampleRate / 20 // 50ms windows
            val energies = ArrayList<Float>()
            var cursor = 0
            while (cursor < totalSamples) {
                var sumAbs = 0L
                val count = minOf(windowSamples, totalSamples - cursor)
                for (i in 0 until count) {
                    sumAbs += abs(bb.short.toInt())
                }
                energies.add(sumAbs.toFloat() / count.coerceAtLeast(1))
                cursor += count
            }
            if (energies.size < 4) return emptyList()

            val avgEnergy = energies.average().toFloat().coerceAtLeast(1f)
            val minGapMs = (60_000.0 / fallbackBpm.coerceIn(60, 190) * 0.65).toLong()
            val peaks = mutableListOf<Long>()
            var lastBeatMs = -minGapMs
            for (i in 1 until energies.lastIndex) {
                val e = energies[i]
                if (e > avgEnergy * 1.28f && e >= energies[i - 1] && e >= energies[i + 1]) {
                    val tMs = i * 50L
                    if (tMs - lastBeatMs >= minGapMs) {
                        peaks.add(tMs)
                        lastBeatMs = tMs
                    }
                }
            }
            peaks
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun applyAutoBeatSync(project: MontageProject): MontageProject {
        if (project.clips.isEmpty()) return project
        val music = project.music
        val bpm = music?.bpm?.coerceIn(60, 190) ?: 120
        val singleBeatMs = (60_000.0 / bpm).toLong().coerceAtLeast(300L)
        // Use 2 beats per clip for high BPM so each photo/video has comfortable rhythm
        val defaultSlotMs = if (bpm >= 112) singleBeatMs * 2 else singleBeatMs
        val rawBeats = if (music != null) {
            detectOrGenerateBeats(music, maxOf(project.totalDurationMs, defaultSlotMs * project.clips.size))
        } else {
            generateBeatGrid(bpm, defaultSlotMs * project.clips.size)
        }

        val beatTransitions = listOf(
            TransitionType.FLASH_BEAT,
            TransitionType.ZOOM_IN,
            TransitionType.SLIDE_LEFT,
            TransitionType.GLITCH_SHAKE,
            TransitionType.ZOOM_OUT
        )

        var timelineCursor = 0L
        val updatedClips = project.clips.mapIndexed { idx, clip ->
            val targetEnd = findNextBeatAfter(
                beats = rawBeats,
                currentMs = timelineCursor,
                idealDeltaMs = defaultSlotMs
            )
            var clipDur = (targetEnd - timelineCursor).coerceIn(500L, 4500L)
            if (clip.mediaType == ClipMediaType.VIDEO) {
                val maxVideoSpan = (((clip.trimEndMs - clip.trimStartMs).takeIf { it > 0L } ?: clip.videoDurationMs) / clip.playbackSpeed.coerceAtLeast(0.25f))
                    .toLong()
                    .coerceAtLeast(600L)
                clipDur = clipDur.coerceAtMost(maxVideoSpan)
            }
            timelineCursor += clipDur
            val transitionType = if (idx < project.clips.lastIndex) {
                beatTransitions[idx % beatTransitions.size]
            } else {
                TransitionType.CROSSFADE
            }
            clip.copy(
                durationMs = clipDur,
                transition = transitionType,
                transitionDurationMs = (clipDur * 0.28f).toLong().coerceIn(180L, 420L)
            )
        }

        val updatedMusic = music?.copy(beatMarksMs = rawBeats)
        return project.copy(
            clips = updatedClips,
            music = updatedMusic
        )
    }

    private fun findNextBeatAfter(beats: List<Long>, currentMs: Long, idealDeltaMs: Long): Long {
        if (beats.isEmpty()) return currentMs + idealDeltaMs
        val target = currentMs + idealDeltaMs
        var bestBeat: Long? = null
        var bestDist = Long.MAX_VALUE
        for (b in beats) {
            if (b > currentMs + 420L) {
                val d = abs(b - target)
                if (d < bestDist) {
                    bestDist = d
                    bestBeat = b
                }
            }
        }
        return if (bestBeat != null && bestDist <= idealDeltaMs) bestBeat else currentMs + idealDeltaMs
    }
}
