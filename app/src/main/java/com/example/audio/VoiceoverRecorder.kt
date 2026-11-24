package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceoverRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var activeFile: File? = null
    private var startedAtMs: Long = 0L

    val isRecording: Boolean
        get() = recorder != null

    fun startRecording(outputFile: File): Boolean {
        stopSilently()
        return try {
            outputFile.parentFile?.mkdirs()
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            activeFile = outputFile
            startedAtMs = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("VoiceoverRecorder", "startRecording failed: ${e.message}")
            stopSilently()
            outputFile.delete()
            false
        }
    }

    fun stopRecording(): Pair<File, Long>? {
        val mr = recorder ?: return null
        val file = activeFile
        val elapsed = System.currentTimeMillis() - startedAtMs
        recorder = null
        activeFile = null
        if (elapsed < 450L) {
            try {
                mr.reset()
            } catch (_: Exception) {
            }
            try {
                mr.release()
            } catch (_: Exception) {
            }
            file?.delete()
            return null
        }
        return try {
            mr.stop()
            mr.release()
            if (file != null && file.exists() && file.length() > 0L) {
                file to elapsed
            } else {
                file?.delete()
                null
            }
        } catch (e: Exception) {
            Log.w("VoiceoverRecorder", "stopRecording error: ${e.message}")
            try {
                mr.reset()
            } catch (_: Exception) {
            }
            try {
                mr.release()
            } catch (_: Exception) {
            }
            file?.delete()
            null
        }
    }

    fun cancelRecording() {
        val file = activeFile
        stopSilently()
        file?.delete()
    }

    private fun stopSilently() {
        val mr = recorder ?: return
        recorder = null
        activeFile = null
        try {
            mr.reset()
        } catch (_: Exception) {
        }
        try {
            mr.release()
        } catch (_: Exception) {
        }
    }
}
