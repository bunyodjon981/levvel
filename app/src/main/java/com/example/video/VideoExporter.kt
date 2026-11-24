package com.example.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.core.content.FileProvider
import com.example.audio.SynthMusicGenerator
import com.example.model.ClipMediaType
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.MontageProject
import com.example.model.MusicTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min

class VideoExporter(private val context: Context) {

    data class Mp4ExportResult(
        val displayPath: String,
        val shareUriString: String,
        val fileName: String
    )

    suspend fun exportToMp4(
        project: MontageProject,
        requestedFileName: String,
        resolution: ExportResolution = ExportResolution.HD_720,
        fpsOption: ExportFps = ExportFps.FPS_30,
        onProgress: (String, Float) -> Unit
    ): Mp4ExportResult = withContext(Dispatchers.IO) {
        val safeBase = requestedFileName
            .trim()
            .removeSuffix(".mp4")
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .trim()
            .ifBlank { "lavelle_video" }
        val finalFileName = "$safeBase.mp4"

        val (width, height) = project.aspectRatio.dimensionsFor(resolution)
        val fps = fpsOption.fps
        val bitrate = if (fps >= 60) resolution.bitrate60 else resolution.bitrate30
        val totalDurationMs = project.totalDurationMs.coerceAtLeast(1000L)
        val totalFrames = ((totalDurationMs * fps) / 1000L).toInt().coerceAtLeast(fps)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tempMp4 = File(exportsDir, finalFileName)
        if (tempMp4.exists()) tempMp4.delete()

        var selectedWidth = width
        var selectedHeight = height
        var selectedFps = fps
        var selectedBitrate = bitrate

        var videoEncoder: MediaCodec? = null
        var inputSurface: Surface? = null
        var eglHelper: EglInputSurfaceRenderer? = null
        var muxer: MediaMuxer? = null
        var audioTrackIndex = -1
        var videoTrackIndex = -1
        var muxerStarted = false
        var muxerStopped = false
        var videoSamplesWritten = 0
        var audioSamplesWritten = 0

        onProgress("${resolution.label} (${fps} FPS) tayyorlanmoqda…", 0.05f)

        val bitmapCache = MontageBitmapCache(context)
        val frameRenderer = MontageFrameRenderer(context, bitmapCache)

        onProgress("Musiqa, ovoz va video audio aralashtirilmoqda…", 0.12f)
        val pcmAudio = mixProjectAudioToMonoPcm(project, totalDurationMs)
        val encodedAudio = if (pcmAudio != null && pcmAudio.isNotEmpty()) {
            encodePcmToAacPackets(pcmAudio)
        } else null

        var frameBitmap: Bitmap? = null

        try {
            val encoderPair = createConfiguredVideoEncoder(
                desiredWidth = width,
                desiredHeight = height,
                desiredFps = fps,
                desiredBitrate = bitrate,
                fallbackAspectRatio = project.aspectRatio
            )
            videoEncoder = encoderPair.encoder
            selectedWidth = encoderPair.width
            selectedHeight = encoderPair.height
            selectedFps = encoderPair.fps
            selectedBitrate = encoderPair.bitrate

            inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            eglHelper = EglInputSurfaceRenderer(inputSurface, selectedWidth, selectedHeight)
            eglHelper.makeCurrent()

            muxer = MediaMuxer(tempMp4.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // Add audio track BEFORE muxer.start() so muxer can start as soon as video format arrives
            val audioPackets = encodedAudio?.packets.orEmpty()
            if (encodedAudio?.format != null && audioPackets.isNotEmpty()) {
                audioTrackIndex = muxer.addTrack(encodedAudio.format)
            }

            val actualTotalFrames = ((totalDurationMs * selectedFps) / 1000L).toInt().coerceAtLeast(selectedFps)
            val createdBitmap = Bitmap.createBitmap(selectedWidth, selectedHeight, Bitmap.Config.ARGB_8888)
            frameBitmap = createdBitmap
            val frameCanvas = Canvas(createdBitmap)
            val videoBufferInfo = MediaCodec.BufferInfo()
            var aacCursor = 0

            val writeAudioUpToPts: (Long) -> Unit = { maxPtsUs ->
                val m = muxer
                if (m != null && muxerStarted && audioTrackIndex >= 0) {
                    while (aacCursor < audioPackets.size && audioPackets[aacCursor].ptsUs <= maxPtsUs) {
                        val pkt = audioPackets[aacCursor]
                        val info = MediaCodec.BufferInfo().apply {
                            set(0, pkt.data.size, pkt.ptsUs, pkt.flags)
                        }
                        m.writeSampleData(audioTrackIndex, ByteBuffer.wrap(pkt.data), info)
                        audioSamplesWritten++
                        aacCursor++
                    }
                }
            }

            val drainVideo: (Boolean) -> Unit = { eos ->
                val encoder = videoEncoder
                val m = muxer
                if (encoder != null && m != null) {
                    var tryAgainCount = 0
                    while (true) {
                        val outIdx = encoder.dequeueOutputBuffer(videoBufferInfo, if (eos) 10_000L else 0L)
                        when {
                            outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                                if (!eos) break
                                tryAgainCount++
                                if (tryAgainCount > 120) break
                            }
                            outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                if (!muxerStarted) {
                                    videoTrackIndex = m.addTrack(encoder.outputFormat)
                                    m.start()
                                    muxerStarted = true
                                }
                            }
                            outIdx >= 0 -> {
                                tryAgainCount = 0
                                val data = encoder.getOutputBuffer(outIdx)
                                val isConfig = (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                                if (data != null && !isConfig && videoBufferInfo.size > 0 && muxerStarted && videoTrackIndex >= 0) {
                                    data.position(videoBufferInfo.offset)
                                    data.limit(videoBufferInfo.offset + videoBufferInfo.size)
                                    m.writeSampleData(videoTrackIndex, data, videoBufferInfo)
                                    videoSamplesWritten++
                                    writeAudioUpToPts(videoBufferInfo.presentationTimeUs)
                                }
                                encoder.releaseOutputBuffer(outIdx, false)
                                if ((videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    break
                                }
                            }
                        }
                    }
                }
            }

            for (frameIdx in 0 until actualTotalFrames) {
                val timeMs = (frameIdx * 1000L) / selectedFps
                frameRenderer.drawFrame(
                    canvas = frameCanvas,
                    width = selectedWidth.toFloat(),
                    height = selectedHeight.toFloat(),
                    project = project,
                    timeMs = timeMs,
                    maxBitmapDim = maxOf(selectedWidth, selectedHeight),
                    exactVideoFrames = true
                )

                val ptsNs = (frameIdx * 1_000_000_000L) / selectedFps
                eglHelper.drawBitmapFrame(createdBitmap, ptsNs)
                drainVideo(false)

                if (frameIdx % 6 == 0) {
                    val pct = 0.16f + 0.72f * (frameIdx.toFloat() / actualTotalFrames)
                    onProgress("Video yozilmoqda (${frameIdx + 1}/$actualTotalFrames kadr)…", pct)
                }
            }

            videoEncoder.signalEndOfInputStream()
            drainVideo(true)

            if (muxerStarted && audioTrackIndex >= 0 && aacCursor < audioPackets.size) {
                onProgress("Audio qo'shilmoqda…", 0.91f)
                writeAudioUpToPts(Long.MAX_VALUE)
            }

            if (!muxerStarted || videoSamplesWritten == 0) {
                throw IllegalStateException("Video kadrlarini kodlash amalga oshmadi")
            }
            muxer.stop()
            muxerStopped = true
        } finally {
            if (frameBitmap != null && !frameBitmap.isRecycled) {
                frameBitmap.recycle()
            }
            bitmapCache.clear()
            try {
                eglHelper?.release()
            } catch (_: Exception) {
            }
            try {
                videoEncoder?.stop()
            } catch (_: Exception) {
            }
            try {
                videoEncoder?.release()
            } catch (_: Exception) {
            }
            try {
                // Only call muxer.stop() if all added tracks received at least one sample and stop() wasn't called yet
                val allAddedTracksHaveSamples = videoSamplesWritten > 0 &&
                    (audioTrackIndex < 0 || audioSamplesWritten > 0)
                if (muxerStarted && !muxerStopped && allAddedTracksHaveSamples) {
                    muxer?.stop()
                }
            } catch (_: Exception) {
            }
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            if (!muxerStopped) {
                tempMp4.delete()
            }
        }

        onProgress("Telefon xotirasiga saqlanmoqda…", 0.96f)
        val displayPath = saveMp4ToGalleryAndDownloads(tempMp4, finalFileName)
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempMp4
        ).toString()

        onProgress("Tayyor!", 1.0f)
        Mp4ExportResult(
            displayPath = displayPath,
            shareUriString = shareUri,
            fileName = finalFileName
        )
    }

    private data class ConfiguredVideoEncoder(
        val encoder: MediaCodec,
        val width: Int,
        val height: Int,
        val fps: Int,
        val bitrate: Int
    )

    private fun createConfiguredVideoEncoder(
        desiredWidth: Int,
        desiredHeight: Int,
        desiredFps: Int,
        desiredBitrate: Int,
        fallbackAspectRatio: com.example.model.AspectRatioOption
    ): ConfiguredVideoEncoder {
        val (hdWidth, hdHeight) = fallbackAspectRatio.dimensionsFor(ExportResolution.HD_720)
        val candidates = listOf(
            Triple(desiredWidth to desiredHeight, desiredFps, desiredBitrate),
            Triple(desiredWidth to desiredHeight, 30, minOf(desiredBitrate, 8_000_000)),
            Triple(hdWidth to hdHeight, 30, 4_500_000)
        ).distinct()

        var lastError: Exception? = null
        for ((dims, candFps, candBitrate) in candidates) {
            val (w, h) = dims
            var codec: MediaCodec? = null
            try {
                val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, candBitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, candFps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return ConfiguredVideoEncoder(codec, w, h, candFps, candBitrate)
            } catch (e: Exception) {
                lastError = e
                Log.w("VideoExporter", "Video encoder config failed for ${w}x$h @${candFps}fps: ${e.message}")
                try {
                    codec?.release()
                } catch (_: Exception) {
                }
            }
        }
        throw lastError ?: IllegalStateException("Video encoder ishga tushmadi")
    }

    private data class AacPacket(
        val data: ByteArray,
        val ptsUs: Long,
        val flags: Int
    )

    private data class EncodedAacResult(
        val format: MediaFormat?,
        val packets: List<AacPacket>
    )

    private fun encodePcmToAacPackets(pcm: ShortArray): EncodedAacResult? {
        var audioEncoder: MediaCodec? = null
        return try {
            val af = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            val bufferInfo = MediaCodec.BufferInfo()
            val packets = ArrayList<AacPacket>()
            var outputFormat: MediaFormat? = null
            var sampleCursor = 0
            var eosSent = false
            var eosReceived = false
            val totalSamples = pcm.size
            var idleCycles = 0

            while (!eosReceived && idleCycles < 400) {
                var progressed = false
                if (!eosSent) {
                    val inIdx = audioEncoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        progressed = true
                        val buf = audioEncoder.getInputBuffer(inIdx)!!
                        buf.clear()
                        if (sampleCursor >= totalSamples) {
                            val ptsUs = (sampleCursor * 1_000_000L) / 44100L
                            audioEncoder.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eosSent = true
                        } else {
                            val maxShorts = (buf.remaining() / 2).coerceAtLeast(1)
                            val count = min(maxShorts, totalSamples - sampleCursor)
                            val ptsUs = (sampleCursor * 1_000_000L) / 44100L
                            val byteOrder = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (i in 0 until count) {
                                byteOrder.putShort(pcm[sampleCursor + i])
                            }
                            buf.put(byteOrder.array())
                            audioEncoder.queueInputBuffer(inIdx, 0, count * 2, ptsUs, 0)
                            sampleCursor += count
                        }
                    }
                }

                var drainLoop = true
                while (drainLoop) {
                    val outIdx = audioEncoder.dequeueOutputBuffer(bufferInfo, 5_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> drainLoop = false
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            progressed = true
                            outputFormat = audioEncoder.outputFormat
                        }
                        outIdx >= 0 -> {
                            progressed = true
                            val data = audioEncoder.getOutputBuffer(outIdx)
                            val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (data != null && !isConfig && bufferInfo.size > 0) {
                                val bytes = ByteArray(bufferInfo.size)
                                data.position(bufferInfo.offset)
                                data.limit(bufferInfo.offset + bufferInfo.size)
                                data.get(bytes)
                                packets.add(
                                    AacPacket(
                                        data = bytes,
                                        ptsUs = bufferInfo.presentationTimeUs,
                                        flags = bufferInfo.flags
                                    )
                                )
                            }
                            audioEncoder.releaseOutputBuffer(outIdx, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                eosReceived = true
                                drainLoop = false
                            }
                        }
                    }
                }
                idleCycles = if (progressed) 0 else idleCycles + 1
            }
            EncodedAacResult(outputFormat, packets)
        } catch (e: Exception) {
            Log.w("VideoExporter", "AAC audio encoding skipped: ${e.message}")
            null
        } finally {
            try {
                audioEncoder?.stop()
            } catch (_: Exception) {
            }
            try {
                audioEncoder?.release()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun mixProjectAudioToMonoPcm(
        project: MontageProject,
        totalDurationMs: Long
    ): ShortArray? {
        val targetSampleCount = ((totalDurationMs * 44100L) / 1000L).toInt().coerceAtLeast(4410)
        val mixBuffer = FloatArray(targetSampleCount)
        var anyAudio = false

        // 1. Background music track
        val audioFile = resolveAudioFile(project.music)
        val musicTrack = project.music
        if (audioFile != null && musicTrack != null) {
            val musicPcm = decodeAudioToMonoPcm(audioFile, totalDurationMs, musicTrack)
            if (musicPcm != null) {
                anyAudio = true
                val limit = min(targetSampleCount, musicPcm.size)
                for (i in 0 until limit) {
                    mixBuffer[i] += musicPcm[i].toFloat()
                }
            }
        }

        // 2. Voiceovers placed at startMs
        project.voiceovers.forEach { vo ->
            val voFile = File(vo.filePath)
            if (!voFile.exists()) return@forEach
            val decoded = decodeRawMono44100(voFile, maxMs = vo.durationMs + 1000L) ?: return@forEach
            anyAudio = true
            val offsetSamples = ((vo.startMs * 44100L) / 1000L).toInt().coerceIn(0, targetSampleCount - 1)
            val maxCopy = min(decoded.size, targetSampleCount - offsetSamples)
            val vol = vo.volume.coerceIn(0f, 1.5f)
            for (i in 0 until maxCopy) {
                mixBuffer[offsetSamples + i] += decoded[i] * vol
            }
        }

        // 3. Video clip original audio tracks (respecting trimStartMs, playbackSpeed, and clipVolume)
        var cursorMs = 0L
        project.clips.forEach { clip ->
            if (clip.mediaType == ClipMediaType.VIDEO && !clip.videoPath.isNullOrBlank() && clip.clipVolume > 0.02f) {
                val vidFile = File(clip.videoPath)
                if (vidFile.exists()) {
                    val sourceNeedMs = clip.trimStartMs + (clip.durationMs * clip.playbackSpeed).toLong() + 400L
                    val raw = decodeRawMono44100(vidFile, maxMs = sourceNeedMs)
                    if (raw != null && raw.isNotEmpty()) {
                        anyAudio = true
                        val srcStart = ((clip.trimStartMs * 44100L) / 1000L).toInt().coerceIn(0, raw.lastIndex)
                        val outStart = ((cursorMs * 44100L) / 1000L).toInt().coerceIn(0, targetSampleCount - 1)
                        val outLen = min(((clip.durationMs * 44100L) / 1000L).toInt(), targetSampleCount - outStart)
                        val speed = clip.playbackSpeed.coerceIn(0.25f, 3.0f)
                        val vol = clip.clipVolume.coerceIn(0f, 1f)
                        for (i in 0 until outLen) {
                            val srcIdx = (srcStart + (i * speed).toInt()).coerceIn(0, raw.lastIndex)
                            mixBuffer[outStart + i] += raw[srcIdx] * vol
                        }
                    }
                }
            }
            cursorMs += clip.durationMs.coerceAtLeast(200L)
        }

        if (!anyAudio) return null
        return ShortArray(targetSampleCount) { i ->
            mixBuffer[i].toInt().coerceIn(-32767, 32767).toShort()
        }
    }

    private suspend fun resolveAudioFile(track: MusicTrack?): File? {
        if (track == null) return null
        track.audioFilePath?.let { path ->
            val f = File(path)
            if (f.exists() && f.length() > 0L) return f
        }
        val preset = track.builtInPreset ?: return null
        return SynthMusicGenerator.ensureBuiltInTrackFile(context, preset, track.bpm, track.durationMs.coerceAtLeast(15_000L))
    }

    private fun decodeAudioToMonoPcm(file: File, targetDurationMs: Long, track: MusicTrack): ShortArray? {
        val targetSampleCount = ((targetDurationMs * 44100L) / 1000L).toInt().coerceAtLeast(4410)
        val raw = decodeRawMono44100(file, maxMs = targetDurationMs + track.trimStartMs + 2000L) ?: return null
        return fitAndApplyVolumeEnvelope(raw, targetSampleCount, track)
    }

    private fun decodeRawMono44100(file: File, maxMs: Long): ShortArray? {
        if (file.extension.equals("wav", ignoreCase = true)) {
            val raw = readWavMono44100(file)
            if (raw != null && raw.isNotEmpty()) return raw
        }
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var audioTrackIdx = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    inputFormat = fmt
                    break
                }
            }
            if (audioTrackIdx < 0 || inputFormat == null) return null
            extractor.selectTrack(audioTrackIdx)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val maxSourceSamples = ((maxMs * sampleRate) / 1000L).toInt().coerceAtLeast(4410)
            val decodedShorts = ArrayList<Short>(min(maxSourceSamples, 44100 * 15))
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && decodedShorts.size < maxSourceSamples) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(5_000L)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = decoder.dequeueOutputBuffer(info, 5_000L)
                if (outIdx >= 0) {
                    val outBuf = decoder.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        val shortBuf = outBuf.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        val frameShorts = ShortArray(shortBuf.remaining())
                        shortBuf.get(frameShorts)
                        if (channels <= 1) {
                            for (s in frameShorts) decodedShorts.add(s)
                        } else {
                            var idx = 0
                            while (idx + channels - 1 < frameShorts.size) {
                                var sum = 0
                                for (c in 0 until channels) sum += frameShorts[idx + c]
                                decodedShorts.add((sum / channels).toShort())
                                idx += channels
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }
            decoder.stop()
            decoder.release()
            if (decodedShorts.isEmpty()) return null

            val rawArr = decodedShorts.toShortArray()
            if (sampleRate == 44100) rawArr else resampleMono(rawArr, sampleRate, 44100)
        } catch (e: Exception) {
            Log.w("VideoExporter", "Audio decode skipped: ${e.message}")
            null
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun readWavMono44100(file: File): ShortArray? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= 44) return null
            val pcmCount = (bytes.size - 44) / 2
            val out = ShortArray(pcmCount)
            val bb = ByteBuffer.wrap(bytes, 44, pcmCount * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until pcmCount) out[i] = bb.short
            out
        } catch (_: Exception) {
            null
        }
    }

    private fun resampleMono(input: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outLen = (input.size / ratio).toInt().coerceAtLeast(1)
        val out = ShortArray(outLen)
        for (i in 0 until outLen) {
            val srcIdx = (i * ratio).toInt().coerceIn(0, input.lastIndex)
            out[i] = input[srcIdx]
        }
        return out
    }

    private fun fitAndApplyVolumeEnvelope(source: ShortArray, targetCount: Int, track: MusicTrack): ShortArray {
        val out = ShortArray(targetCount)
        val startOffset = ((track.trimStartMs * 44100L) / 1000L).toInt().coerceIn(0, source.lastIndex)
        val fadeInSamples = ((track.fadeInMs * 44100L) / 1000L).toInt().coerceAtLeast(1)
        val fadeOutSamples = ((track.fadeOutMs * 44100L) / 1000L).toInt().coerceAtLeast(1)

        for (i in 0 until targetCount) {
            val srcIdx = (startOffset + i) % source.size
            val inEnv = (i.toFloat() / fadeInSamples).coerceIn(0f, 1f)
            val outEnv = ((targetCount - 1 - i).toFloat() / fadeOutSamples).coerceIn(0f, 1f)
            val gain = track.volume.coerceIn(0f, 1f) * minOf(inEnv, outEnv)
            out[i] = (source[srcIdx] * gain).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun saveMp4ToGalleryAndDownloads(sourceFile: File, fileName: String): String {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Lavelle")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return "Movies/Lavelle/$fileName"
            }
        }
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        fallbackDir.mkdirs()
        val dest = File(fallbackDir, fileName)
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }
}

private class EglInputSurfaceRenderer(
    private val surface: Surface,
    private val width: Int,
    private val height: Int
) {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private var textureId = 0
    private var posHandle = 0
    private var texHandle = 0

    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(16 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(
                floatArrayOf(
                    -1f, -1f, 0f, 1f,
                    1f, -1f, 1f, 1f,
                    -1f, 1f, 0f, 0f,
                    1f, 1f, 1f, 0f
                )
            )
            position(0)
        }

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            0x3142, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
        if (numConfigs[0] <= 0 || configs[0] == null) {
            val fallbackAttribs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            EGL14.eglChooseConfig(eglDisplay, fallbackAttribs, 0, configs, 0, 1, numConfigs, 0)
        }

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)

        val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
    }

    fun makeCurrent() {
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        setupGl()
    }

    private fun setupGl() {
        val vs = compileShader(
            GLES20.GL_VERTEX_SHADER,
            """
            attribute vec4 aPos;
            attribute vec2 aTex;
            varying vec2 vTex;
            void main() {
                gl_Position = aPos;
                vTex = aTex;
            }
            """.trimIndent()
        )
        val fs = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            """
            precision mediump float;
            varying vec2 vTex;
            uniform sampler2D uTex;
            void main() {
                gl_FragColor = texture2D(uTex, vTex);
            }
            """.trimIndent()
        )
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        posHandle = GLES20.glGetAttribLocation(program, "aPos")
        texHandle = GLES20.glGetAttribLocation(program, "aTex")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }

    fun drawBitmapFrame(bitmap: Bitmap, ptsNs: Long) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(texHandle)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 16, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, ptsNs)
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
        }
        surface.release()
    }
}
