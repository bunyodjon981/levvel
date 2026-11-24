package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.example.model.MusicTrack
import com.example.model.VoiceoverItem
import java.io.File
import kotlin.math.abs

class AudioPlaybackController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var preparedSourceKey: String? = null
    private var isPrepared = false

    private val voiceoverPlayers = mutableMapOf<String, Pair<String, MediaPlayer>>()

    suspend fun prepareIfNeeded(track: MusicTrack?, voiceovers: List<VoiceoverItem> = emptyList()): File? {
        syncVoiceoverPlayers(voiceovers)

        if (track == null) {
            releaseMusicOnly()
            return null
        }

        val resolvedFile = resolveAudioFile(track) ?: return null
        val key = "${resolvedFile.absolutePath}_${resolvedFile.lastModified()}"
        if (key == preparedSourceKey && mediaPlayer != null && isPrepared) {
            applyVolume(track.volume)
            return resolvedFile
        }

        releaseMusicOnly()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(resolvedFile.absolutePath)
                isLooping = true
                prepare()
            }
            mediaPlayer = mp
            preparedSourceKey = key
            isPrepared = true
            applyVolume(track.volume)
        } catch (e: Exception) {
            Log.e("AudioPlayback", "Failed to prepare audio: ${e.message}")
            releaseMusicOnly()
        }
        return resolvedFile
    }

    private fun syncVoiceoverPlayers(voiceovers: List<VoiceoverItem>) {
        val activeIds = voiceovers.map { it.id }.toSet()
        val removed = voiceoverPlayers.keys.filter { it !in activeIds }
        removed.forEach { id ->
            releasePlayer(voiceoverPlayers.remove(id)?.second)
        }

        voiceovers.forEach { vo ->
            val f = File(vo.filePath)
            if (!f.exists() || f.length() == 0L) return@forEach
            val key = "${f.absolutePath}_${f.lastModified()}"
            val existing = voiceoverPlayers[vo.id]
            if (existing != null && existing.first == key) {
                val vol = vo.volume.coerceIn(0f, 1f)
                try {
                    existing.second.setVolume(vol, vol)
                } catch (_: Exception) {
                }
                return@forEach
            }
            if (existing != null) {
                releasePlayer(existing.second)
                voiceoverPlayers.remove(vo.id)
            }
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(f.absolutePath)
                    isLooping = false
                    prepare()
                    val vol = vo.volume.coerceIn(0f, 1f)
                    setVolume(vol, vol)
                }
                voiceoverPlayers[vo.id] = key to mp
            } catch (e: Exception) {
                Log.w("AudioPlayback", "Failed to prepare voiceover ${vo.id}: ${e.message}")
            }
        }
    }

    suspend fun resolveAudioFile(track: MusicTrack): File? {
        track.audioFilePath?.let { path ->
            val f = File(path)
            if (f.exists() && f.length() > 0L) return f
        }
        val preset = track.builtInPreset ?: return null
        return SynthMusicGenerator.ensureBuiltInTrackFile(
            context = context,
            preset = preset,
            bpm = track.bpm,
            durationMs = track.durationMs.coerceAtLeast(15_000L)
        )
    }

    fun startAt(timelineMs: Long, track: MusicTrack?, voiceovers: List<VoiceoverItem> = emptyList()) {
        val mp = mediaPlayer
        if (mp != null && isPrepared && track != null) {
            try {
                val trackDuration = mp.duration.takeIf { it > 0 } ?: 1
                val targetMs = ((timelineMs + track.trimStartMs) % trackDuration).toInt().coerceAtLeast(0)
                if (abs(mp.currentPosition - targetMs) > 180) {
                    mp.seekTo(targetMs)
                }
                applyVolume(track.volume)
                if (!mp.isPlaying) {
                    mp.start()
                }
            } catch (e: Exception) {
                Log.w("AudioPlayback", "startAt error: ${e.message}")
            }
        }

        updateVoiceoversAt(timelineMs, voiceovers, playing = true)
    }

    fun tickVoiceovers(timelineMs: Long, voiceovers: List<VoiceoverItem>) {
        updateVoiceoversAt(timelineMs, voiceovers, playing = true)
    }

    private fun updateVoiceoversAt(timelineMs: Long, voiceovers: List<VoiceoverItem>, playing: Boolean) {
        voiceovers.forEach { vo ->
            val mp = voiceoverPlayers[vo.id]?.second ?: return@forEach
            val endMs = vo.startMs + vo.durationMs
            val inWindow = timelineMs in vo.startMs until endMs
            try {
                val vol = vo.volume.coerceIn(0f, 1f)
                mp.setVolume(vol, vol)
                if (inWindow && playing) {
                    val localMs = (timelineMs - vo.startMs).toInt().coerceIn(0, (mp.duration - 20).coerceAtLeast(0))
                    if (!mp.isPlaying) {
                        mp.seekTo(localMs)
                        mp.start()
                    } else if (abs(mp.currentPosition - localMs) > 260) {
                        mp.seekTo(localMs)
                    }
                } else {
                    if (mp.isPlaying) {
                        mp.pause()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun seekTo(timelineMs: Long, track: MusicTrack?, voiceovers: List<VoiceoverItem> = emptyList()) {
        val mp = mediaPlayer
        if (mp != null && isPrepared && track != null) {
            try {
                val trackDuration = mp.duration.takeIf { it > 0 } ?: 1
                val targetMs = ((timelineMs + track.trimStartMs) % trackDuration).toInt().coerceAtLeast(0)
                mp.seekTo(targetMs)
            } catch (_: Exception) {
            }
        }
        voiceovers.forEach { vo ->
            val vmp = voiceoverPlayers[vo.id]?.second ?: return@forEach
            try {
                if (timelineMs in vo.startMs until (vo.startMs + vo.durationMs)) {
                    val localMs = (timelineMs - vo.startMs).toInt().coerceIn(0, (vmp.duration - 20).coerceAtLeast(0))
                    vmp.seekTo(localMs)
                } else if (vmp.isPlaying) {
                    vmp.pause()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun applyVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(v, v)
        } catch (_: Exception) {
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
        voiceoverPlayers.values.forEach { (_, mp) ->
            try {
                if (mp.isPlaying) mp.pause()
            } catch (_: Exception) {
            }
        }
    }

    private fun releaseMusicOnly() {
        releasePlayer(mediaPlayer)
        mediaPlayer = null
        preparedSourceKey = null
        isPrepared = false
    }

    fun release() {
        releaseMusicOnly()
        voiceoverPlayers.values.forEach { (_, mp) -> releasePlayer(mp) }
        voiceoverPlayers.clear()
    }

    private fun releasePlayer(mp: MediaPlayer?) {
        if (mp == null) return
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Exception) {
        }
        try {
            mp.release()
        } catch (_: Exception) {
        }
    }
}
