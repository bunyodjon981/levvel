package com.example.io

import com.example.model.AnimationCatalog
import com.example.model.AspectRatioOption
import com.example.model.ClipBackgroundStyle
import com.example.model.ClipFitMode
import com.example.model.ClipItem
import com.example.model.ClipMediaType
import com.example.model.FilterPreset
import com.example.model.MontageProject
import com.example.model.MotionEffect
import com.example.model.MusicTrack
import com.example.model.StickerKind
import com.example.model.StickerOverlayItem
import com.example.model.TextAnimationStyle
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.VoiceoverItem
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ProjectJsonCodec {

    fun toJson(
        project: MontageProject,
        clipAssetPaths: Map<String, String> = emptyMap(),
        clipVideoPaths: Map<String, String> = emptyMap(),
        embeddedAudioRelPath: String? = null,
        embeddedVoiceoverPaths: Map<String, String> = emptyMap()
    ): String {
        val root = JSONObject().apply {
            put("format", "lavelle-v2")
            put("id", project.id)
            put("name", project.name)
            put("templateId", project.templateId ?: JSONObject.NULL)
            put("aspectRatio", project.aspectRatio.name)
            put("updatedAt", project.updatedAt)

            put(
                "watermark",
                JSONObject().apply {
                    put("enabled", project.watermark.enabled)
                    put("text", project.watermark.text)
                    put("position", project.watermark.position.name)
                }
            )

            val clipsArr = JSONArray()
            project.clips.forEach { clip ->
                val obj = JSONObject().apply {
                    put("id", clip.id)
                    put("slotLabel", clip.slotLabel)
                    put("mediaType", clip.mediaType.name)
                    put("imagePath", clip.imagePath ?: JSONObject.NULL)
                    put("embeddedAsset", clipAssetPaths[clip.id] ?: JSONObject.NULL)
                    put("videoPath", clip.videoPath ?: JSONObject.NULL)
                    put("embeddedVideo", clipVideoPaths[clip.id] ?: JSONObject.NULL)
                    put("videoDurationMs", clip.videoDurationMs)
                    put("trimStartMs", clip.trimStartMs)
                    put("trimEndMs", clip.trimEndMs)
                    put("clipVolume", clip.clipVolume.toDouble())
                    put("playbackSpeed", clip.playbackSpeed.toDouble())
                    put("sampleDrawableName", clip.sampleDrawableName ?: JSONObject.NULL)
                    put("durationMs", clip.durationMs)
                    put("transition", clip.transition.name)
                    put("transitionDurationMs", clip.transitionDurationMs)
                    put("motion", clip.motion.name)
                    put("animationId", clip.animationId)
                    put("filter", clip.filter.name)
                    put("brightness", clip.brightness.toDouble())
                    put("contrast", clip.contrast.toDouble())
                    put("saturation", clip.saturation.toDouble())
                    put("zoomScale", clip.zoomScale.toDouble())
                    put("rotationQuarterTurns", clip.rotationQuarterTurns)
                    put("flipHorizontal", clip.flipHorizontal)
                    put("flipVertical", clip.flipVertical)
                    put("fitMode", clip.fitMode.name)
                    put("backgroundStyle", clip.backgroundStyle.name)
                }
                clipsArr.put(obj)
            }
            put("clips", clipsArr)

            val textsArr = JSONArray()
            project.texts.forEach { txt ->
                val obj = JSONObject().apply {
                    put("id", txt.id)
                    put("text", txt.text)
                    put("startMs", txt.startMs)
                    put("endMs", txt.endMs)
                    put("xFraction", txt.xFraction.toDouble())
                    put("yFraction", txt.yFraction.toDouble())
                    put("fontSizeSp", txt.fontSizeSp.toDouble())
                    put("scale", txt.scale.toDouble())
                    put("rotationDeg", txt.rotationDeg.toDouble())
                    put("fontStyle", txt.fontStyle.name)
                    put("colorArgb", txt.colorArgb)
                    put("hasBackdrop", txt.hasBackdrop)
                    put("animation", txt.animation.name)
                }
                textsArr.put(obj)
            }
            put("texts", textsArr)

            val stickersArr = JSONArray()
            project.stickers.forEach { st ->
                val obj = JSONObject().apply {
                    put("id", st.id)
                    put("kind", st.kind.name)
                    put("startMs", st.startMs)
                    put("endMs", st.endMs)
                    put("xFraction", st.xFraction.toDouble())
                    put("yFraction", st.yFraction.toDouble())
                    put("scale", st.scale.toDouble())
                    put("rotationDeg", st.rotationDeg.toDouble())
                }
                stickersArr.put(obj)
            }
            put("stickers", stickersArr)

            val voiceoversArr = JSONArray()
            project.voiceovers.forEach { vo ->
                val obj = JSONObject().apply {
                    put("id", vo.id)
                    put("title", vo.title)
                    put("filePath", vo.filePath)
                    put("embeddedVoiceover", embeddedVoiceoverPaths[vo.id] ?: JSONObject.NULL)
                    put("startMs", vo.startMs)
                    put("durationMs", vo.durationMs)
                    put("volume", vo.volume.toDouble())
                }
                voiceoversArr.put(obj)
            }
            put("voiceovers", voiceoversArr)

            if (project.music != null) {
                val m = project.music
                val musicObj = JSONObject().apply {
                    put("id", m.id)
                    put("title", m.title)
                    put("artist", m.artist)
                    put("bpm", m.bpm)
                    put("durationMs", m.durationMs)
                    put("builtInPreset", m.builtInPreset ?: JSONObject.NULL)
                    put("audioFilePath", m.audioFilePath ?: JSONObject.NULL)
                    put("embeddedAudio", embeddedAudioRelPath ?: JSONObject.NULL)
                    put("volume", m.volume.toDouble())
                    put("trimStartMs", m.trimStartMs)
                    put("fadeInMs", m.fadeInMs)
                    put("fadeOutMs", m.fadeOutMs)
                    val beatsArr = JSONArray()
                    m.beatMarksMs.forEach { beatsArr.put(it) }
                    put("beatMarksMs", beatsArr)
                }
                put("music", musicObj)
            } else {
                put("music", JSONObject.NULL)
            }
        }
        return root.toString(2)
    }

    fun fromJson(
        jsonString: String,
        overrideProjectId: String? = null,
        resolveEmbeddedAsset: (String) -> String? = { null },
        resolveEmbeddedAudio: (String) -> String? = { null },
        resolveEmbeddedVideo: (String) -> String? = resolveEmbeddedAsset,
        resolveEmbeddedVoiceover: (String) -> String? = resolveEmbeddedAudio
    ): MontageProject {
        val root = JSONObject(jsonString)
        val projectId = overrideProjectId ?: root.optString("id").ifBlank { UUID.randomUUID().toString() }
        val name = root.optString("name", "Lavelle Montaj")
        val templateId = root.optStringOrNull("templateId")
        val aspectRatio = enumOrDefault(root.optString("aspectRatio"), AspectRatioOption.PORTRAIT_9_16)
        val updatedAt = root.optLong("updatedAt", System.currentTimeMillis())

        val watermarkObj = root.optJSONObject("watermark")
        val watermark = if (watermarkObj != null) {
            WatermarkConfig(
                enabled = watermarkObj.optBoolean("enabled", false),
                text = watermarkObj.optString("text", "@Lavelle"),
                position = enumOrDefault(watermarkObj.optString("position"), WatermarkPosition.BOTTOM_RIGHT)
            )
        } else {
            WatermarkConfig()
        }

        val clips = mutableListOf<ClipItem>()
        val clipsArr = root.optJSONArray("clips") ?: JSONArray()
        for (i in 0 until clipsArr.length()) {
            val c = clipsArr.getJSONObject(i)
            val embeddedAsset = c.optStringOrNull("embeddedAsset")
            val resolvedAssetPath = embeddedAsset?.let(resolveEmbeddedAsset)
            val storedPath = resolvedAssetPath ?: c.optStringOrNull("imagePath")

            val embeddedVideo = c.optStringOrNull("embeddedVideo")
            val resolvedVideoPath = embeddedVideo?.let(resolveEmbeddedVideo)
            val storedVideoPath = resolvedVideoPath ?: c.optStringOrNull("videoPath")

            val mediaType = enumOrDefault(c.optString("mediaType"), ClipMediaType.IMAGE)
            val motion = enumOrDefault(c.optString("motion"), MotionEffect.NONE)
            val animId = c.optStringOrNull("animationId") ?: AnimationCatalog.fromLegacyMotion(motion).id
            val rawVideoDur = c.optLong("videoDurationMs", 0L)
            val trimStart = c.optLong("trimStartMs", 0L).coerceAtLeast(0L)
            val trimEnd = c.optLong("trimEndMs", rawVideoDur).coerceAtLeast(trimStart)

            clips.add(
                ClipItem(
                    id = c.optString("id").ifBlank { UUID.randomUUID().toString() },
                    slotLabel = c.optString("slotLabel", "Kadr ${i + 1}"),
                    mediaType = mediaType,
                    imagePath = storedPath,
                    videoPath = storedVideoPath,
                    videoDurationMs = rawVideoDur,
                    trimStartMs = trimStart,
                    trimEndMs = trimEnd,
                    clipVolume = c.optDouble("clipVolume", 1.0).toFloat().coerceIn(0f, 1f),
                    playbackSpeed = c.optDouble("playbackSpeed", 1.0).toFloat().coerceIn(0.25f, 3.0f),
                    sampleDrawableName = c.optStringOrNull("sampleDrawableName"),
                    durationMs = c.optLong("durationMs", 1600L).coerceIn(300L, 60_000L),
                    transition = enumOrDefault(c.optString("transition"), TransitionType.CROSSFADE),
                    transitionDurationMs = c.optLong("transitionDurationMs", 450L).coerceIn(150L, 1500L),
                    motion = motion,
                    animationId = animId,
                    filter = enumOrDefault(c.optString("filter"), FilterPreset.ORIGINAL),
                    brightness = c.optDouble("brightness", 0.0).toFloat(),
                    contrast = c.optDouble("contrast", 1.0).toFloat(),
                    saturation = c.optDouble("saturation", 1.0).toFloat(),
                    zoomScale = c.optDouble("zoomScale", 1.0).toFloat(),
                    rotationQuarterTurns = c.optInt("rotationQuarterTurns", 0) % 4,
                    flipHorizontal = c.optBoolean("flipHorizontal", false),
                    flipVertical = c.optBoolean("flipVertical", false),
                    fitMode = enumOrDefault(c.optString("fitMode"), ClipFitMode.COVER),
                    backgroundStyle = enumOrDefault(c.optString("backgroundStyle"), ClipBackgroundStyle.BLUR)
                )
            )
        }

        val texts = mutableListOf<TextOverlayItem>()
        val textsArr = root.optJSONArray("texts") ?: JSONArray()
        for (i in 0 until textsArr.length()) {
            val t = textsArr.getJSONObject(i)
            texts.add(
                TextOverlayItem(
                    id = t.optString("id").ifBlank { UUID.randomUUID().toString() },
                    text = t.optString("text", ""),
                    startMs = t.optLong("startMs", 0L),
                    endMs = t.optLong("endMs", 2500L),
                    xFraction = t.optDouble("xFraction", 0.5).toFloat(),
                    yFraction = t.optDouble("yFraction", 0.78).toFloat(),
                    fontSizeSp = t.optDouble("fontSizeSp", 24.0).toFloat(),
                    scale = t.optDouble("scale", 1.0).toFloat().coerceIn(0.4f, 3.5f),
                    rotationDeg = t.optDouble("rotationDeg", 0.0).toFloat(),
                    fontStyle = enumOrDefault(t.optString("fontStyle"), TextFontStyle.DISPLAY),
                    colorArgb = t.optLong("colorArgb", 0xFFFFFFFFL),
                    hasBackdrop = t.optBoolean("hasBackdrop", true),
                    animation = enumOrDefault(t.optString("animation"), TextAnimationStyle.FADE_UP)
                )
            )
        }

        val stickers = mutableListOf<StickerOverlayItem>()
        val stickersArr = root.optJSONArray("stickers") ?: JSONArray()
        for (i in 0 until stickersArr.length()) {
            val s = stickersArr.getJSONObject(i)
            stickers.add(
                StickerOverlayItem(
                    id = s.optString("id").ifBlank { UUID.randomUUID().toString() },
                    kind = enumOrDefault(s.optString("kind"), StickerKind.HEART),
                    startMs = s.optLong("startMs", 0L),
                    endMs = s.optLong("endMs", 3000L),
                    xFraction = s.optDouble("xFraction", 0.5).toFloat(),
                    yFraction = s.optDouble("yFraction", 0.5).toFloat(),
                    scale = s.optDouble("scale", 1.0).toFloat().coerceIn(0.4f, 3.5f),
                    rotationDeg = s.optDouble("rotationDeg", 0.0).toFloat()
                )
            )
        }

        val voiceovers = mutableListOf<VoiceoverItem>()
        val voiceoversArr = root.optJSONArray("voiceovers") ?: JSONArray()
        for (i in 0 until voiceoversArr.length()) {
            val v = voiceoversArr.getJSONObject(i)
            val embeddedVo = v.optStringOrNull("embeddedVoiceover")
            val resolvedVo = embeddedVo?.let(resolveEmbeddedVoiceover) ?: v.optStringOrNull("filePath")
            if (!resolvedVo.isNullOrBlank()) {
                voiceovers.add(
                    VoiceoverItem(
                        id = v.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = v.optString("title", "Ovoz ${i + 1}"),
                        filePath = resolvedVo,
                        startMs = v.optLong("startMs", 0L).coerceAtLeast(0L),
                        durationMs = v.optLong("durationMs", 3000L).coerceAtLeast(300L),
                        volume = v.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1.5f)
                    )
                )
            }
        }

        val musicObj = if (root.isNull("music")) null else root.optJSONObject("music")
        val music = musicObj?.let { m ->
            val embeddedAudio = m.optStringOrNull("embeddedAudio")
            val resolvedAudio = embeddedAudio?.let(resolveEmbeddedAudio) ?: m.optStringOrNull("audioFilePath")
            val beatsArr = m.optJSONArray("beatMarksMs") ?: JSONArray()
            val beats = buildList {
                for (b in 0 until beatsArr.length()) {
                    add(beatsArr.getLong(b))
                }
            }
            MusicTrack(
                id = m.optString("id").ifBlank { UUID.randomUUID().toString() },
                title = m.optString("title", "Musiqa"),
                artist = m.optString("artist", "Lavelle"),
                bpm = m.optInt("bpm", 120),
                durationMs = m.optLong("durationMs", 20_000L),
                builtInPreset = m.optStringOrNull("builtInPreset"),
                audioFilePath = resolvedAudio,
                volume = m.optDouble("volume", 0.9).toFloat(),
                trimStartMs = m.optLong("trimStartMs", 0L),
                fadeInMs = m.optLong("fadeInMs", 350L),
                fadeOutMs = m.optLong("fadeOutMs", 700L),
                beatMarksMs = beats
            )
        }

        return MontageProject(
            id = projectId,
            name = name,
            templateId = templateId,
            aspectRatio = aspectRatio,
            clips = clips,
            texts = texts,
            stickers = stickers,
            voiceovers = voiceovers,
            watermark = watermark,
            music = music,
            updatedAt = updatedAt
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() && it != "null" }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T {
        return try {
            enumValueOf<T>(name)
        } catch (_: Exception) {
            default
        }
    }
}
