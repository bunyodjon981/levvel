package com.example.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.AnimAlphaCurve
import com.example.model.AnimFxOverlay
import com.example.model.AnimRevealMode
import com.example.model.AnimationCatalog
import com.example.model.AnimationSpec
import com.example.model.ClipBackgroundStyle
import com.example.model.ClipFitMode
import com.example.model.ClipItem
import com.example.model.ClipMediaType
import com.example.model.FilterPreset
import com.example.model.MontageProject
import com.example.model.MotionEffect
import com.example.model.StickerKind
import com.example.model.StickerOverlayItem
import com.example.model.TextAnimationStyle
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MontageBitmapCache(private val context: Context) {
    private val cache = LinkedHashMap<String, Bitmap>(24, 0.75f, true)
    private val blurCache = LinkedHashMap<String, Bitmap>(12, 0.75f, true)
    private val retrievers = HashMap<String, MediaMetadataRetriever>()

    fun getBitmap(
        clip: ClipItem,
        maxDim: Int = 1280,
        localTimeMs: Long = 0L,
        exactVideoFrame: Boolean = false
    ): Bitmap? {
        if (clip.mediaType == ClipMediaType.VIDEO && !clip.videoPath.isNullOrBlank()) {
            val videoFile = File(clip.videoPath)
            if (videoFile.exists()) {
                val maxDur = (clip.trimEndMs.takeIf { it > clip.trimStartMs } ?: clip.videoDurationMs).coerceAtLeast(100L)
                val videoTimeMs = (clip.trimStartMs + (localTimeMs * clip.playbackSpeed).toLong())
                    .coerceIn(0L, maxDur)
                val stepMs = if (exactVideoFrame) 16L else 85L
                val bucket = videoTimeMs / stepMs
                val vidKey = "vid:${clip.videoPath}:$bucket:$maxDim"
                cache[vidKey]?.let { if (!it.isRecycled) return it }

                val frameBmp = extractVideoFrame(
                    videoPath = clip.videoPath,
                    timeUs = videoTimeMs * 1000L,
                    maxDim = maxDim,
                    exact = exactVideoFrame
                )
                if (frameBmp != null) {
                    putBitmap(vidKey, frameBmp, maxEntries = 18)
                    return frameBmp
                }
            }
        }

        val key = clip.imagePath ?: ("res:" + (clip.sampleDrawableName ?: ""))
        if (key.isBlank() || key == "res:") return null

        cache[key]?.let { if (!it.isRecycled) return it }

        val bmp = if (!clip.imagePath.isNullOrBlank()) {
            decodeFileDownsampled(File(clip.imagePath), maxDim)
        } else {
            val resId = clip.sampleDrawableName?.let {
                context.resources.getIdentifier(it, "drawable", context.packageName)
            } ?: 0
            if (resId != 0) decodeResourceDownsampled(resId, maxDim) else null
        }

        if (bmp != null) {
            putBitmap(key, bmp, maxEntries = 18)
        }
        return bmp
    }

    fun getBlurredBackground(source: Bitmap, seedKey: String): Bitmap {
        val cacheKey = "blur:$seedKey"
        blurCache[cacheKey]?.let { if (!it.isRecycled) return it }

        val targetW = 48
        val targetH = ((source.height.toFloat() / source.width.coerceAtLeast(1)) * targetW)
            .toInt()
            .coerceIn(24, 96)
        val tiny = Bitmap.createScaledBitmap(source, targetW, targetH, true)
        val blurred = fastBoxBlur(tiny, radius = 4, passes = 2)
        if (tiny !== blurred) tiny.recycle()

        if (blurCache.size >= 10) {
            val eldest = blurCache.keys.firstOrNull()
            if (eldest != null) blurCache.remove(eldest)?.recycle()
        }
        blurCache[cacheKey] = blurred
        return blurred
    }

    private fun extractVideoFrame(
        videoPath: String,
        timeUs: Long,
        maxDim: Int,
        exact: Boolean
    ): Bitmap? {
        return try {
            val retriever = retrievers.getOrPut(videoPath) {
                MediaMetadataRetriever().apply { setDataSource(videoPath) }
            }
            val option = if (exact) MediaMetadataRetriever.OPTION_CLOSEST else MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            val raw = retriever.getFrameAtTime(timeUs.coerceAtLeast(0L), option) ?: return null
            val maxSide = maxOf(raw.width, raw.height)
            if (maxSide <= maxDim) {
                raw
            } else {
                val scale = maxDim.toFloat() / maxSide
                val w = (raw.width * scale).toInt().coerceAtLeast(1)
                val h = (raw.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(raw, w, h, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun putBitmap(key: String, bmp: Bitmap, maxEntries: Int) {
        if (cache.size >= maxEntries) {
            val eldestKey = cache.keys.firstOrNull()
            if (eldestKey != null) {
                cache.remove(eldestKey)?.recycle()
            }
        }
        cache[key] = bmp
    }

    private fun fastBoxBlur(src: Bitmap, radius: Int, passes: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        val tmp = IntArray(w * h)

        repeat(passes) {
            for (y in 0 until h) {
                val rowOff = y * w
                for (x in 0 until w) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    var count = 0
                    for (dx in -radius..radius) {
                        val sx = (x + dx).coerceIn(0, w - 1)
                        val c = pix[rowOff + sx]
                        rSum += (c shr 16) and 0xFF
                        gSum += (c shr 8) and 0xFF
                        bSum += c and 0xFF
                        count++
                    }
                    tmp[rowOff + x] = (0xFF shl 24) or
                        ((rSum / count) shl 16) or
                        ((gSum / count) shl 8) or
                        (bSum / count)
                }
            }
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var rSum = 0
                    var gSum = 0
                    var bSum = 0
                    var count = 0
                    for (dy in -radius..radius) {
                        val sy = (y + dy).coerceIn(0, h - 1)
                        val c = tmp[sy * w + x]
                        rSum += (c shr 16) and 0xFF
                        gSum += (c shr 8) and 0xFF
                        bSum += c and 0xFF
                        count++
                    }
                    pix[y * w + x] = (0xFF shl 24) or
                        ((rSum / count) shl 16) or
                        ((gSum / count) shl 8) or
                        (bSum / count)
                }
            }
        }
        return Bitmap.createBitmap(pix, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun decodeFileDownsampled(file: File, maxDim: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun decodeResourceDownsampled(resId: Int, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    private fun computeInSampleSize(w: Int, h: Int, maxDim: Int): Int {
        var sample = 1
        while (w / sample > maxDim * 2 || h / sample > maxDim * 2) {
            sample *= 2
        }
        return sample
    }

    fun clear() {
        cache.values.forEach { if (!it.isRecycled) it.recycle() }
        cache.clear()
        blurCache.values.forEach { if (!it.isRecycled) it.recycle() }
        blurCache.clear()
        retrievers.values.forEach {
            try {
                it.release()
            } catch (_: Exception) {
            }
        }
        retrievers.clear()
    }
}

class MontageFrameRenderer(
    private val context: Context,
    private val bitmapCache: MontageBitmapCache
) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B0B13") }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(155, 11, 11, 19)
    }
    private val spaceGrotesk: Typeface? = try {
        ResourcesCompat.getFont(context, R.font.space_grotesk)
    } catch (_: Exception) {
        Typeface.DEFAULT_BOLD
    }
    private val jakartaSans: Typeface? = try {
        ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
    } catch (_: Exception) {
        Typeface.SANS_SERIF
    }
    private val jetbrainsMono: Typeface? = try {
        ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    } catch (_: Exception) {
        Typeface.MONOSPACE
    }
    private val handwritingTypeface: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)

    fun drawFrame(
        canvas: Canvas,
        width: Float,
        height: Float,
        project: MontageProject,
        timeMs: Long,
        maxBitmapDim: Int = 1080,
        exactVideoFrames: Boolean = false
    ) {
        canvas.drawRect(0f, 0f, width, height, bgPaint)
        val clips = project.clips
        if (clips.isEmpty()) {
            drawEmptyPlaceholder(canvas, width, height)
            return
        }

        val totalMs = project.totalDurationMs.coerceAtLeast(1L)
        val clampedTime = timeMs.coerceIn(0L, totalMs)

        var cursor = 0L
        var activeIndex = clips.lastIndex
        var clipStartMs = 0L
        for (i in clips.indices) {
            val dur = clips[i].durationMs.coerceAtLeast(200L)
            if (clampedTime < cursor + dur || i == clips.lastIndex) {
                activeIndex = i
                clipStartMs = cursor
                break
            }
            cursor += dur
        }

        val activeClip = clips[activeIndex]
        val activeLocalMs = (clampedTime - clipStartMs).coerceAtLeast(0L)
        val activeProgress = (activeLocalMs.toFloat() / activeClip.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)

        val nextClip = clips.getOrNull(activeIndex + 1)
        val transDur = activeClip.transitionDurationMs.coerceIn(150L, (activeClip.durationMs * 0.7f).toLong().coerceAtLeast(150L))
        val transStartLocal = (activeClip.durationMs - transDur).coerceAtLeast(0L)
        val inTransition = nextClip != null && activeClip.transition != TransitionType.NONE && activeLocalMs >= transStartLocal

        if (!inTransition || nextClip == null) {
            drawClipTransformed(
                canvas = canvas,
                width = width,
                height = height,
                clip = activeClip,
                localTimeMs = activeLocalMs,
                progress = activeProgress,
                alpha = 1f,
                extraScale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                maxBitmapDim = maxBitmapDim,
                exactVideoFrames = exactVideoFrames
            )
        } else {
            val t = ((activeLocalMs - transStartLocal).toFloat() / transDur.toFloat()).coerceIn(0f, 1f)
            val eased = EffectPreviewRenderer.easeInOutCubic(t)
            val transPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            when (activeClip.transition) {
                TransitionType.NONE -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f, 1f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.CROSSFADE -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f - eased, 1f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                    drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, eased, 1f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.ZOOM_IN -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f - eased, 1f + eased * 0.45f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                    drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, eased, 0.85f + eased * 0.15f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.ZOOM_OUT -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f - eased, 1f - eased * 0.25f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                    drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, eased, 1.35f - eased * 0.35f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.SLIDE_LEFT -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f, 1f, -width * eased, 0f, maxBitmapDim, exactVideoFrames)
                    drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, 1f, 1f, width * (1f - eased), 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.SLIDE_RIGHT -> {
                    drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f, 1f, width * eased, 0f, maxBitmapDim, exactVideoFrames)
                    drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, 1f, 1f, -width * (1f - eased), 0f, maxBitmapDim, exactVideoFrames)
                }
                TransitionType.FLASH_BEAT -> {
                    if (eased < 0.5f) {
                        drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f, 1f + eased * 0.12f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                    } else {
                        drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, 1f, 1.08f - (eased - 0.5f) * 0.16f, 0f, 0f, maxBitmapDim, exactVideoFrames)
                    }
                    val flashAlpha = (1f - abs(eased - 0.5f) * 2f).coerceIn(0f, 1f)
                    transPaint.color = Color.argb((flashAlpha * 215).toInt(), 255, 255, 255)
                    canvas.drawRect(0f, 0f, width, height, transPaint)
                }
                TransitionType.GLITCH_SHAKE -> {
                    val shake = sin(eased * PI * 8.0).toFloat() * width * 0.03f * (1f - eased)
                    val shakeY = sin(eased * PI * 11.0).toFloat() * height * 0.015f * (1f - eased)
                    if (eased < 0.55f) {
                        drawClipTransformed(canvas, width, height, activeClip, activeLocalMs, activeProgress, 1f, 1.04f, shake, shakeY, maxBitmapDim, exactVideoFrames)
                    } else {
                        drawClipTransformed(canvas, width, height, nextClip, 0L, 0f, 1f, 1.04f, -shake, -shakeY, maxBitmapDim, exactVideoFrames)
                    }
                    if (eased in 0.25f..0.75f) {
                        transPaint.color = Color.argb(65, 0, 229, 255)
                        canvas.drawRect(0f, height * 0.28f, width, height * 0.34f, transPaint)
                        transPaint.color = Color.argb(65, 255, 77, 141)
                        canvas.drawRect(0f, height * 0.62f, width, height * 0.67f, transPaint)
                    }
                }
            }
        }

        project.stickers.forEach { sticker ->
            if (clampedTime in sticker.startMs..sticker.endMs) {
                drawStickerOverlay(canvas, width, height, sticker, clampedTime)
            }
        }

        project.texts.forEach { item ->
            if (clampedTime in item.startMs..item.endMs && item.text.isNotBlank()) {
                val span = (item.endMs - item.startMs).coerceAtLeast(1L)
                val localProgress = ((clampedTime - item.startMs).toFloat() / span).coerceIn(0f, 1f)
                drawTextOverlay(canvas, width, height, item, localProgress)
            }
        }

        if (project.watermark.enabled && project.watermark.text.isNotBlank()) {
            drawWatermark(canvas, width, height, project.watermark)
        }
    }

    private fun drawClipTransformed(
        canvas: Canvas,
        width: Float,
        height: Float,
        clip: ClipItem,
        localTimeMs: Long,
        progress: Float,
        alpha: Float,
        extraScale: Float,
        offsetX: Float,
        offsetY: Float,
        maxBitmapDim: Int,
        exactVideoFrames: Boolean
    ) {
        val bmp = bitmapCache.getBitmap(clip, maxBitmapDim, localTimeMs, exactVideoFrames)
        if (bmp == null) {
            val grad = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width, height,
                    Color.parseColor("#1D2240"),
                    Color.parseColor("#0D1528"),
                    Shader.TileMode.CLAMP
                )
                this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, grad)
            return
        }

        // Draw backdrop if Contain fit mode or rotated/non-covering
        if (clip.fitMode == ClipFitMode.CONTAIN || clip.zoomScale < 1f) {
            drawClipBackground(canvas, width, height, clip, bmp, alpha, offsetX, offsetY)
        }

        val animSpec = resolveAnimationSpec(clip)
        EffectPreviewRenderer.drawBitmapWithSpec(
            canvas = canvas,
            width = width,
            height = height,
            bmp = bmp,
            animSpec = animSpec,
            filter = clip.filter,
            brightness = clip.brightness,
            contrast = clip.contrast,
            saturation = clip.saturation,
            clipZoomScale = clip.zoomScale,
            progress = progress,
            alpha = alpha,
            extraScale = extraScale,
            offsetX = offsetX,
            offsetY = offsetY,
            rotationQuarterTurns = clip.rotationQuarterTurns,
            flipHorizontal = clip.flipHorizontal,
            flipVertical = clip.flipVertical,
            fitMode = clip.fitMode
        )
    }

    private fun drawClipBackground(
        canvas: Canvas,
        width: Float,
        height: Float,
        clip: ClipItem,
        bmp: Bitmap,
        alpha: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        canvas.save()
        canvas.clipRect(offsetX, offsetY, offsetX + width, offsetY + height)
        val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
        when (clip.backgroundStyle) {
            ClipBackgroundStyle.BLUR -> {
                val seed = clip.imagePath ?: clip.videoPath ?: clip.sampleDrawableName ?: clip.id
                val blurred = bitmapCache.getBlurredBackground(bmp, seed)
                val coverScale = maxOf(width / blurred.width.toFloat(), height / blurred.height.toFloat())
                val m = Matrix().apply {
                    postTranslate(-blurred.width / 2f, -blurred.height / 2f)
                    postScale(coverScale, coverScale)
                    postTranslate(width / 2f + offsetX, height / 2f + offsetY)
                }
                val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                    this.alpha = alphaInt
                    colorFilter = EffectPreviewRenderer.buildColorFilter(clip.filter, clip.brightness - 0.12f, clip.contrast, clip.saturation)
                }
                canvas.drawBitmap(blurred, m, p)
                val scrim = Paint().apply { color = Color.argb((alpha * 95).toInt().coerceIn(0, 255), 8, 8, 16) }
                canvas.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, scrim)
            }
            ClipBackgroundStyle.GRADIENT -> {
                val grad = Paint().apply {
                    shader = LinearGradient(
                        0f, 0f, width, height,
                        intArrayOf(
                            Color.parseColor("#112842"),
                            Color.parseColor("#1D1233"),
                            Color.parseColor("#2E142B")
                        ),
                        null,
                        Shader.TileMode.CLAMP
                    )
                    this.alpha = alphaInt
                }
                canvas.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, grad)
            }
            ClipBackgroundStyle.BLACK -> {
                val black = Paint().apply {
                    color = Color.BLACK
                    this.alpha = alphaInt
                }
                canvas.drawRect(offsetX, offsetY, offsetX + width, offsetY + height, black)
            }
        }
        canvas.restore()
    }

    private fun resolveAnimationSpec(clip: ClipItem): AnimationSpec {
        return if (clip.animationId != "anim_001") {
            AnimationCatalog.getById(clip.animationId)
        } else if (clip.motion != MotionEffect.NONE) {
            AnimationCatalog.fromLegacyMotion(clip.motion)
        } else {
            AnimationCatalog.animations.first()
        }
    }

    private fun resolveTextTypeface(style: TextFontStyle): Typeface? = when (style) {
        TextFontStyle.DISPLAY -> spaceGrotesk
        TextFontStyle.MODERN -> jakartaSans
        TextFontStyle.MONO -> jetbrainsMono
        TextFontStyle.SERIF -> Typeface.SERIF
        TextFontStyle.HANDWRITING -> handwritingTypeface
    }

    private fun drawTextOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        item: TextOverlayItem,
        localProgress: Float
    ) {
        val enterFrac = (localProgress / 0.18f).coerceIn(0f, 1f)
        val exitFrac = ((1f - localProgress) / 0.15f).coerceIn(0f, 1f)
        val alphaFrac = minOf(enterFrac, exitFrac)

        var yShift = 0f
        var animScale = 1f
        when (item.animation) {
            TextAnimationStyle.FADE_UP -> {
                yShift = (1f - EffectPreviewRenderer.easeOutCubic(enterFrac)) * height * 0.04f
            }
            TextAnimationStyle.POP_SCALE -> {
                animScale = 0.72f + 0.28f * EffectPreviewRenderer.easeOutBack(enterFrac)
            }
            TextAnimationStyle.GLOW_PULSE -> {
                animScale = 1f + 0.04f * sin(localProgress * PI * 4.0).toFloat()
            }
            TextAnimationStyle.STATIC -> Unit
        }

        val scaleFactor = width / 360f
        textPaint.typeface = resolveTextTypeface(item.fontStyle)
        textPaint.textSize = item.fontSizeSp * scaleFactor
        textPaint.color = item.colorArgb.toInt()
        textPaint.alpha = (alphaFrac * 255).toInt().coerceIn(0, 255)
        textPaint.setShadowLayer(8f * scaleFactor, 0f, 2f * scaleFactor, Color.argb(180, 0, 0, 0))

        val cx = width * item.xFraction
        val cy = height * item.yFraction + yShift
        val totalScale = animScale * item.scale.coerceIn(0.4f, 3.5f)

        canvas.save()
        canvas.rotate(item.rotationDeg, cx, cy)
        canvas.scale(totalScale, totalScale, cx, cy)
        if (item.hasBackdrop) {
            val textWidth = textPaint.measureText(item.text)
            val fm = textPaint.fontMetrics
            val padH = 18f * scaleFactor
            val padV = 10f * scaleFactor
            val rect = RectF(
                cx - textWidth / 2f - padH,
                cy + fm.ascent - padV,
                cx + textWidth / 2f + padH,
                cy + fm.descent + padV
            )
            textBgPaint.alpha = (alphaFrac * 165).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(rect, 14f * scaleFactor, 14f * scaleFactor, textBgPaint)
        }
        canvas.drawText(item.text, cx, cy, textPaint)
        canvas.restore()
    }

    private fun drawStickerOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        sticker: StickerOverlayItem,
        timeMs: Long
    ) {
        val unit = minOf(width, height).coerceAtLeast(100f)
        val scaleFactor = width / 360f

        when (sticker.kind) {
            StickerKind.POLAROID -> {
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F7F5EE")
                    style = Paint.Style.STROKE
                    strokeWidth = unit * 0.055f
                }
                val bottomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F7F5EE")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width, height, framePaint)
                canvas.drawRect(0f, height - unit * 0.14f, width, height, bottomPaint)
                val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = handwritingTypeface
                    textSize = 15f * scaleFactor
                    color = Color.parseColor("#2B2B33")
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("LAVELLE MEMORY", width * 0.5f, height - unit * 0.045f, labelPaint)
            }
            StickerKind.VHS_RETRO -> {
                val scanPaint = Paint().apply {
                    color = Color.argb(38, 255, 255, 255)
                    strokeWidth = 1.5f
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width, y, scanPaint)
                    y += 8f * scaleFactor
                }
                val borderPaint = Paint().apply {
                    color = Color.argb(120, 0, 229, 255)
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f * scaleFactor
                }
                canvas.drawRect(10f * scaleFactor, 10f * scaleFactor, width - 10f * scaleFactor, height - 10f * scaleFactor, borderPaint)
                val vhsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = jetbrainsMono
                    textSize = 14f * scaleFactor
                    color = Color.parseColor("#00FF9C")
                    textAlign = Paint.Align.LEFT
                }
                val secs = (timeMs / 1000L).toInt()
                val dateLabel = String.format(java.util.Locale.US, "VHS • 00:%02d", secs)
                canvas.drawText("▶ PLAY", 22f * scaleFactor, 32f * scaleFactor, vhsPaint)
                canvas.drawText(dateLabel, 22f * scaleFactor, height - 22f * scaleFactor, vhsPaint)
            }
            StickerKind.CINEMA_REC -> {
                val barH = height * 0.085f
                val blackPaint = Paint().apply { color = Color.BLACK }
                canvas.drawRect(0f, 0f, width, barH, blackPaint)
                canvas.drawRect(0f, height - barH, width, height, blackPaint)
                val recDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if ((timeMs / 450L) % 2L == 0L) Color.RED else Color.argb(90, 255, 0, 0)
                }
                canvas.drawCircle(24f * scaleFactor, barH * 0.52f, 5.5f * scaleFactor, recDot)
                val recText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = jetbrainsMono
                    textSize = 12f * scaleFactor
                    color = Color.WHITE
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("REC  24FPS  RAW", 36f * scaleFactor, barH * 0.58f, recText)
            }
            StickerKind.NEON_FRAME -> {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 10f * scaleFactor
                    color = Color.argb(115, 255, 77, 141)
                }
                val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 3f * scaleFactor
                    color = Color.parseColor("#00E5FF")
                }
                val pad = 16f * scaleFactor
                val rect = RectF(pad, pad, width - pad, height - pad)
                canvas.drawRoundRect(rect, 20f * scaleFactor, 20f * scaleFactor, glow)
                canvas.drawRoundRect(rect, 20f * scaleFactor, 20f * scaleFactor, inner)
            }
            else -> {
                val cx = width * sticker.xFraction
                val cy = height * sticker.yFraction
                val size = unit * 0.16f * sticker.scale.coerceIn(0.4f, 3.5f)
                canvas.save()
                canvas.rotate(sticker.rotationDeg, cx, cy)
                val emoji = when (sticker.kind) {
                    StickerKind.HEART -> "❤️"
                    StickerKind.FIRE -> "🔥"
                    StickerKind.LIGHTNING -> "⚡"
                    StickerKind.CROWN -> "👑"
                    StickerKind.STAR -> "⭐"
                    StickerKind.MUSIC_VIBE -> "🎵"
                    StickerKind.SPARKLES -> "✨"
                    StickerKind.SUNGLASSES -> "😎"
                    else -> "✨"
                }
                val p = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = size
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(8f * scaleFactor, 0f, 3f * scaleFactor, Color.argb(140, 0, 0, 0))
                }
                val fm = p.fontMetrics
                val baseline = cy - (fm.ascent + fm.descent) / 2f
                canvas.drawText(emoji, cx, baseline, p)
                canvas.restore()
            }
        }
    }

    private fun drawWatermark(
        canvas: Canvas,
        width: Float,
        height: Float,
        watermark: WatermarkConfig
    ) {
        val scaleFactor = width / 360f
        val wmPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = spaceGrotesk ?: Typeface.DEFAULT_BOLD
            textSize = 13.5f * scaleFactor
            color = Color.argb(225, 255, 255, 255)
            setShadowLayer(4f * scaleFactor, 0f, 1.5f * scaleFactor, Color.argb(160, 0, 0, 0))
        }
        val padX = 16f * scaleFactor
        val padY = 24f * scaleFactor
        val textW = wmPaint.measureText(watermark.text)
        val fm = wmPaint.fontMetrics
        val boxPadH = 10f * scaleFactor
        val boxPadV = 5f * scaleFactor

        val (cx, baseY) = when (watermark.position) {
            WatermarkPosition.TOP_LEFT -> (padX + textW / 2f + boxPadH) to (padY + boxPadV - fm.ascent)
            WatermarkPosition.TOP_RIGHT -> (width - padX - textW / 2f - boxPadH) to (padY + boxPadV - fm.ascent)
            WatermarkPosition.BOTTOM_LEFT -> (padX + textW / 2f + boxPadH) to (height - padY - boxPadV - fm.descent)
            WatermarkPosition.BOTTOM_RIGHT -> (width - padX - textW / 2f - boxPadH) to (height - padY - boxPadV - fm.descent)
        }
        wmPaint.textAlign = Paint.Align.CENTER
        val bgRect = RectF(
            cx - textW / 2f - boxPadH,
            baseY + fm.ascent - boxPadV,
            cx + textW / 2f + boxPadH,
            baseY + fm.descent + boxPadV
        )
        val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 10, 12, 22)
        }
        canvas.drawRoundRect(bgRect, 12f * scaleFactor, 12f * scaleFactor, pill)
        canvas.drawText(watermark.text, cx, baseY, wmPaint)
    }

    private fun drawEmptyPlaceholder(canvas: Canvas, width: Float, height: Float) {
        textPaint.typeface = spaceGrotesk
        textPaint.textSize = (width / 360f) * 18f
        textPaint.color = Color.parseColor("#A0A5C0")
        canvas.drawText("Rasm yoki video qo'shing", width / 2f, height / 2f, textPaint)
    }
}

object EffectPreviewRenderer {

    fun drawSingleEffectPreview(
        canvas: Canvas,
        width: Float,
        height: Float,
        bitmap: Bitmap,
        animSpec: AnimationSpec,
        filter: FilterPreset = FilterPreset.ORIGINAL,
        progress: Float
    ) {
        val bg = Paint().apply { color = Color.parseColor("#0B0B13") }
        canvas.drawRect(0f, 0f, width, height, bg)
        drawBitmapWithSpec(
            canvas = canvas,
            width = width,
            height = height,
            bmp = bitmap,
            animSpec = animSpec,
            filter = filter,
            brightness = 0f,
            contrast = 1f,
            saturation = 1f,
            clipZoomScale = 1f,
            progress = progress,
            alpha = 1f,
            extraScale = 1f,
            offsetX = 0f,
            offsetY = 0f
        )
    }

    fun drawBitmapWithSpec(
        canvas: Canvas,
        width: Float,
        height: Float,
        bmp: Bitmap,
        animSpec: AnimationSpec,
        filter: FilterPreset,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        clipZoomScale: Float,
        progress: Float,
        alpha: Float,
        extraScale: Float,
        offsetX: Float,
        offsetY: Float,
        rotationQuarterTurns: Int = 0,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        fitMode: ClipFitMode = ClipFitMode.COVER
    ) {
        val p = progress.coerceIn(0f, 1f)
        val eased = easeInOutCubic(p)

        var motionScale = lerp(animSpec.zoomFrom, animSpec.zoomTo, eased)
        if (animSpec.zoomPulseAmp != 0f) {
            motionScale += abs(sin(p * PI * animSpec.zoomPulseFreq)).toFloat() * animSpec.zoomPulseAmp
        }
        if (animSpec.elasticZoom) {
            motionScale *= 1f + sin(p * PI * 4.0).toFloat() * 0.08f * (1f - p)
        }

        var motionDx = lerp(animSpec.panFromX, animSpec.panToX, eased) * width
        var motionDy = lerp(animSpec.panFromY, animSpec.panToY, eased) * height

        if (animSpec.orbitX != 0f) {
            motionDx += cos(p * PI * 2.0).toFloat() * width * animSpec.orbitX
        }
        if (animSpec.orbitY != 0f) {
            motionDy += sin(p * PI * 2.0).toFloat() * height * animSpec.orbitY
        }

        if (animSpec.bounceDrop) {
            val b = easeOutBounce((p / 0.55f).coerceIn(0f, 1f))
            motionDy -= (1f - b) * height * 0.55f
        }

        var rotationDeg = lerp(animSpec.rotateFromDeg, animSpec.rotateToDeg, eased)
        if (animSpec.rotateSwingAmp != 0f) {
            rotationDeg += sin(p * PI * animSpec.rotateSwingFreq).toFloat() * animSpec.rotateSwingAmp
        }

        if (animSpec.shakeAmp != 0f) {
            motionDx += sin(p * PI * animSpec.shakeFreq).toFloat() * width * animSpec.shakeAmp
            motionDy += cos(p * PI * (animSpec.shakeFreq * 0.83f)).toFloat() * height * animSpec.shakeAmp * 0.7f
        }

        val skewWave = sin(p * PI).toFloat()
        val skewX = animSpec.skewAmpX * skewWave
        val skewY = animSpec.skewAmpY * skewWave

        val motionAlpha = when (animSpec.alphaCurve) {
            AnimAlphaCurve.ALWAYS_ONE -> 1f
            AnimAlphaCurve.FADE_IN_FAST -> easeOutCubic((p / 0.30f).coerceIn(0f, 1f))
            AnimAlphaCurve.FADE_IN_SLOW -> easeOutCubic((p / 0.55f).coerceIn(0f, 1f))
            AnimAlphaCurve.FADE_OUT -> easeOutCubic(((1f - p) / 0.45f).coerceIn(0f, 1f))
            AnimAlphaCurve.FADE_IN_OUT -> {
                val fi = (p / 0.25f).coerceIn(0f, 1f)
                val fo = ((1f - p) / 0.25f).coerceIn(0f, 1f)
                minOf(fi, fo)
            }
            AnimAlphaCurve.FLICKER_ELECTRIC -> {
                val step = (p * 18f).toInt()
                if (step % 5 == 2) 0.42f else if (step % 7 == 4) 0.72f else 1f
            }
        }

        val turns = ((rotationQuarterTurns % 4) + 4) % 4
        val effectiveW = if (turns % 2 == 1) bmp.height.toFloat() else bmp.width.toFloat()
        val effectiveH = if (turns % 2 == 1) bmp.width.toFloat() else bmp.height.toFloat()

        val totalScale = clipZoomScale.coerceIn(0.45f, 2.5f) * motionScale * extraScale
        val baseFitScale = if (fitMode == ClipFitMode.CONTAIN) {
            minOf(width / effectiveW.coerceAtLeast(1f), height / effectiveH.coerceAtLeast(1f))
        } else {
            maxOf(width / effectiveW.coerceAtLeast(1f), height / effectiveH.coerceAtLeast(1f))
        } * totalScale

        val sx = if (flipHorizontal) -baseFitScale else baseFitScale
        val sy = if (flipVertical) -baseFitScale else baseFitScale

        val matrix = Matrix().apply {
            postTranslate(-bmp.width / 2f, -bmp.height / 2f)
            postRotate(turns * 90f)
            postScale(sx, sy)
            if (skewX != 0f || skewY != 0f) {
                postSkew(skewX, skewY)
            }
            if (rotationDeg != 0f) {
                postRotate(rotationDeg)
            }
            postTranslate(width / 2f + offsetX + motionDx, height / 2f + offsetY + motionDy)
        }

        val finalAlpha = (alpha * motionAlpha).coerceIn(0f, 1f)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (finalAlpha * 255).toInt()
            colorFilter = buildColorFilter(filter, brightness, contrast, saturation)
        }

        canvas.save()
        canvas.clipRect(0f, 0f, width, height)

        applyRevealClip(canvas, width, height, animSpec.revealMode, p)

        canvas.drawBitmap(bmp, matrix, imagePaint)

        drawFxOverlay(canvas, width, height, animSpec.fxOverlay, p, finalAlpha)

        canvas.restore()
    }

    private fun applyRevealClip(
        canvas: Canvas,
        width: Float,
        height: Float,
        mode: AnimRevealMode,
        p: Float
    ) {
        if (mode == AnimRevealMode.NONE) return
        val open = easeOutCubic((p / 0.55f).coerceIn(0f, 1f))
        val path = Path()
        when (mode) {
            AnimRevealMode.NONE -> Unit
            AnimRevealMode.CURTAIN_H -> {
                val halfW = (width * 0.5f) * open
                canvas.clipRect(width * 0.5f - halfW, 0f, width * 0.5f + halfW, height)
            }
            AnimRevealMode.CURTAIN_V -> {
                val halfH = (height * 0.5f) * open
                canvas.clipRect(0f, height * 0.5f - halfH, width, height * 0.5f + halfH)
            }
            AnimRevealMode.IRIS_CIRCLE -> {
                val maxR = sqrt(width * width + height * height) * 0.55f
                path.addCircle(width / 2f, height / 2f, (maxR * open).coerceAtLeast(4f), Path.Direction.CW)
                canvas.clipPath(path)
            }
            AnimRevealMode.DIAMOND_IN -> {
                val rx = width * 0.85f * open
                val ry = height * 0.85f * open
                val cx = width * 0.5f
                val cy = height * 0.5f
                path.moveTo(cx, cy - ry)
                path.lineTo(cx + rx, cy)
                path.lineTo(cx, cy + ry)
                path.lineTo(cx - rx, cy)
                path.close()
                canvas.clipPath(path)
            }
            AnimRevealMode.BLINDS_H -> {
                val strips = 6
                val stripH = height / strips
                for (i in 0 until strips) {
                    val top = i * stripH
                    path.addRect(0f, top, width, top + stripH * open, Path.Direction.CW)
                }
                canvas.clipPath(path)
            }
            AnimRevealMode.BLINDS_V -> {
                val strips = 6
                val stripW = width / strips
                for (i in 0 until strips) {
                    val left = i * stripW
                    path.addRect(left, 0f, left + stripW * open, height, Path.Direction.CW)
                }
                canvas.clipPath(path)
            }
            AnimRevealMode.SPLIT_DIAGONAL -> {
                val offset = (1f - open) * (width + height) * 0.5f
                path.moveTo(0f, 0f)
                path.lineTo(width - offset, 0f)
                path.lineTo(width, height)
                path.lineTo(offset, height)
                path.close()
                canvas.clipPath(path)
            }
        }
    }

    private fun drawFxOverlay(
        canvas: Canvas,
        width: Float,
        height: Float,
        overlay: AnimFxOverlay,
        p: Float,
        finalAlpha: Float
    ) {
        if (overlay == AnimFxOverlay.NONE) return
        val fxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val unit = minOf(width, height).coerceAtLeast(64f)

        when (overlay) {
            AnimFxOverlay.NONE -> Unit

            AnimFxOverlay.LIGHTNING_BOLT_CYAN -> {
                drawLightningBolt(canvas, width, height, p, Color.parseColor("#00E5FF"), Color.WHITE, leftOrigin = true)
            }
            AnimFxOverlay.LIGHTNING_BOLT_GOLD -> {
                drawLightningBolt(canvas, width, height, p, Color.parseColor("#FFB84D"), Color.parseColor("#FFF8E1"), leftOrigin = false)
            }
            AnimFxOverlay.LIGHTNING_DUAL_STORM -> {
                val stormFlash = (abs(sin(p * PI * 6.0)).toFloat()).coerceIn(0f, 1f)
                if (stormFlash > 0.65f) {
                    fxPaint.color = Color.argb(((stormFlash - 0.65f) * 2.5f * 130).toInt().coerceIn(0, 140), 210, 245, 255)
                    canvas.drawRect(0f, 0f, width, height, fxPaint)
                }
                drawLightningBolt(canvas, width, height, p, Color.parseColor("#00E5FF"), Color.WHITE, leftOrigin = true)
                drawLightningBolt(canvas, width, height, (p + 0.35f) % 1f, Color.parseColor("#B388FF"), Color.WHITE, leftOrigin = false)
            }
            AnimFxOverlay.ELECTRIC_ARC_BORDER -> {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (unit * 0.025f).coerceAtLeast(2f)
                    color = Color.parseColor("#00E5FF")
                }
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (unit * 0.06f).coerceAtLeast(4f)
                    color = Color.argb(110, 0, 229, 255)
                }
                val path = Path()
                val segs = 12
                val margin = unit * 0.03f
                path.moveTo(margin, margin)
                for (i in 1..segs) {
                    val jitter = sin((p * 24f + i * 2.1f) * PI).toFloat() * margin
                    path.lineTo(lerp(margin, width - margin, i.toFloat() / segs), margin + jitter)
                }
                for (i in 1..segs) {
                    val jitter = cos((p * 24f + i * 1.9f) * PI).toFloat() * margin
                    path.lineTo(width - margin + jitter, lerp(margin, height - margin, i.toFloat() / segs))
                }
                for (i in 1..segs) {
                    val jitter = sin((p * 24f + i * 2.7f) * PI).toFloat() * margin
                    path.lineTo(lerp(width - margin, margin, i.toFloat() / segs), height - margin + jitter)
                }
                for (i in 1..segs) {
                    val jitter = cos((p * 24f + i * 1.5f) * PI).toFloat() * margin
                    path.lineTo(margin + jitter, lerp(height - margin, margin, i.toFloat() / segs))
                }
                canvas.drawPath(path, glowPaint)
                canvas.drawPath(path, borderPaint)
            }
            AnimFxOverlay.THUNDER_FLASH -> {
                val pulse = if (p < 0.35f) 1f - p / 0.35f else abs(sin(p * PI * 3.0)).toFloat() * 0.25f
                val a = (pulse * 210 * finalAlpha).toInt().coerceIn(0, 255)
                fxPaint.color = Color.argb(a, 255, 255, 255)
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.STROBE_BEAT -> {
                val flash = (sin(p * PI * 8.0).toFloat()).coerceAtLeast(0f)
                fxPaint.color = Color.argb((flash * 105 * finalAlpha).toInt(), 255, 255, 255)
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.NEON_SWEEP -> {
                val sweepX = (p * 1.6f - 0.3f) * width
                fxPaint.shader = LinearGradient(
                    sweepX - width * 0.25f, 0f, sweepX + width * 0.25f, height * 0.4f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(125, 0, 229, 255), Color.argb(105, 255, 77, 141), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.LASER_SCAN_H -> {
                val y = ((p * 2f) % 1f) * height
                fxPaint.shader = LinearGradient(
                    0f, y - height * 0.08f, 0f, y + height * 0.08f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(210, 0, 229, 255), Color.WHITE, Color.argb(210, 0, 229, 255), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, y - height * 0.08f, width, y + height * 0.08f, fxPaint)
            }
            AnimFxOverlay.LASER_SCAN_V -> {
                val x = ((p * 2f) % 1f) * width
                fxPaint.shader = LinearGradient(
                    x - width * 0.08f, 0f, x + width * 0.08f, 0f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(210, 255, 77, 141), Color.WHITE, Color.argb(210, 255, 77, 141), Color.TRANSPARENT),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(x - width * 0.08f, 0f, x + width * 0.08f, height, fxPaint)
            }
            AnimFxOverlay.RGB_GLITCH_BARS -> {
                val step = (p * 14f).toInt()
                val band1 = ((step * 37) % 70) / 100f * height
                val band2 = ((step * 53) % 75) / 100f * height
                fxPaint.color = Color.argb(95, 0, 229, 255)
                canvas.drawRect(0f, band1, width, band1 + height * 0.06f, fxPaint)
                fxPaint.color = Color.argb(95, 255, 77, 141)
                canvas.drawRect(0f, band2, width, band2 + height * 0.05f, fxPaint)
                if (step % 3 == 0) {
                    fxPaint.color = Color.argb(55, 255, 255, 255)
                    canvas.drawRect(width * 0.1f, (band1 + band2) * 0.5f, width * 0.9f, (band1 + band2) * 0.5f + 3f, fxPaint)
                }
            }
            AnimFxOverlay.CHROMATIC_PULSE -> {
                val shift = (abs(sin(p * PI * 4.0)).toFloat() * width * 0.055f).coerceAtLeast(3f)
                fxPaint.color = Color.argb(80, 0, 229, 255)
                canvas.drawRect(0f, 0f, shift, height, fxPaint)
                fxPaint.color = Color.argb(80, 255, 77, 141)
                canvas.drawRect(width - shift, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.VIGNETTE_DARK -> {
                val radius = maxOf(width, height) * 0.72f
                fxPaint.shader = RadialGradient(
                    width / 2f, height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb(185, 5, 5, 10)),
                    floatArrayOf(0.42f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.SPOTLIGHT_BEAM -> {
                val open = easeOutCubic((p / 0.55f).coerceIn(0f, 1f))
                val radius = (maxOf(width, height) * (0.28f + 0.52f * open)).coerceAtLeast(10f)
                fxPaint.shader = RadialGradient(
                    width / 2f, height / 2f, radius,
                    intArrayOf(Color.TRANSPARENT, Color.argb(215, 0, 0, 0)),
                    floatArrayOf(0.38f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
            AnimFxOverlay.RAIN_STREAKS -> {
                fxPaint.color = Color.argb(135, 190, 235, 255)
                fxPaint.strokeWidth = (unit * 0.01f).coerceAtLeast(1.5f)
                fxPaint.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 22) {
                    val x = ((i * 47 % 100) / 100f) * width
                    val yBase = (((i * 29) / 100f + p * 2.2f) % 1.2f) * height - height * 0.1f
                    canvas.drawLine(x, yBase, x - width * 0.03f, yBase + height * 0.10f, fxPaint)
                }
            }
            AnimFxOverlay.SNOW_SPARKLES -> {
                fxPaint.color = Color.argb(210, 255, 255, 255)
                for (i in 0 until 24) {
                    val x = ((i * 37 % 100) / 100f) * width + sin(p * PI * 2.0 + i).toFloat() * width * 0.02f
                    val y = (((i * 19) / 100f + p * 1.1f) % 1f) * height
                    val r = (unit * (0.010f + (i % 3) * 0.005f)).coerceAtLeast(1.5f)
                    canvas.drawCircle(x, y, r, fxPaint)
                }
            }
            AnimFxOverlay.EMBER_FIRE -> {
                for (i in 0 until 22) {
                    val x = ((i * 43 % 100) / 100f) * width + sin(p * PI * 3.0 + i).toFloat() * width * 0.03f
                    val y = height - (((i * 23) / 100f + p * 1.4f) % 1f) * height
                    val r = (unit * (0.012f + (i % 3) * 0.006f)).coerceAtLeast(2f)
                    fxPaint.color = if (i % 2 == 0) Color.argb(220, 255, 145, 0) else Color.argb(210, 255, 77, 77)
                    canvas.drawCircle(x, y, r, fxPaint)
                }
            }
            AnimFxOverlay.SPEED_LINES -> {
                fxPaint.color = Color.argb(120, 255, 255, 255)
                fxPaint.strokeWidth = (unit * 0.012f).coerceAtLeast(1.5f)
                val cx = width * 0.5f
                val cy = height * 0.5f
                val maxR = maxOf(width, height) * 0.7f
                for (i in 0 until 20) {
                    val angle = (i * 18.0 + p * 120.0) * PI / 180.0
                    val inner = maxR * (0.45f + (i % 3) * 0.08f)
                    val outer = maxR * 0.98f
                    canvas.drawLine(
                        cx + cos(angle).toFloat() * inner,
                        cy + sin(angle).toFloat() * inner,
                        cx + cos(angle).toFloat() * outer,
                        cy + sin(angle).toFloat() * outer,
                        fxPaint
                    )
                }
            }
            AnimFxOverlay.BOKEH_LIGHTS -> {
                for (i in 0 until 12) {
                    val x = ((i * 59 % 100) / 100f) * width + cos(p * PI * 2.0 + i).toFloat() * width * 0.03f
                    val y = ((i * 31 % 100) / 100f) * height + sin(p * PI * 2.0 + i).toFloat() * height * 0.03f
                    val r = unit * (0.05f + (i % 4) * 0.025f)
                    fxPaint.color = when (i % 3) {
                        0 -> Color.argb(65, 255, 193, 7)
                        1 -> Color.argb(60, 0, 229, 255)
                        else -> Color.argb(60, 255, 77, 141)
                    }
                    canvas.drawCircle(x, y, r, fxPaint)
                }
            }
            AnimFxOverlay.FILM_GRAIN_DUST -> {
                val frame = (p * 16f).toInt()
                fxPaint.color = Color.argb(85, 255, 240, 210)
                for (i in 0 until 18) {
                    val x = (((i * 37 + frame * 17) % 100) / 100f) * width
                    val y = (((i * 61 + frame * 23) % 100) / 100f) * height
                    canvas.drawCircle(x, y, (unit * 0.008f).coerceAtLeast(1f), fxPaint)
                }
                val scratchX = (((frame * 29) % 100) / 100f) * width
                fxPaint.color = Color.argb(65, 255, 245, 220)
                fxPaint.strokeWidth = 1.5f
                canvas.drawLine(scratchX, 0f, scratchX, height, fxPaint)
            }
            AnimFxOverlay.CINEMA_LETTERBOX -> {
                val e = easeOutCubic((p / 0.35f).coerceIn(0f, 1f))
                val barH = height * 0.11f * e
                val boxPaint = Paint().apply { color = Color.BLACK }
                canvas.drawRect(0f, 0f, width, barH, boxPaint)
                canvas.drawRect(0f, height - barH, width, height, boxPaint)
            }
            AnimFxOverlay.PRISM_AURORA -> {
                val shift = sin(p * PI * 2.0).toFloat() * width * 0.2f
                fxPaint.shader = LinearGradient(
                    shift, 0f, width + shift, height,
                    intArrayOf(
                        Color.argb(95, 0, 229, 255),
                        Color.argb(75, 124, 77, 255),
                        Color.argb(85, 255, 77, 141),
                        Color.argb(75, 46, 229, 157)
                    ),
                    null,
                    Shader.TileMode.MIRROR
                )
                canvas.drawRect(0f, 0f, width, height, fxPaint)
            }
        }
    }

    private fun drawLightningBolt(
        canvas: Canvas,
        width: Float,
        height: Float,
        p: Float,
        glowColor: Int,
        coreColor: Int,
        leftOrigin: Boolean
    ) {
        val cycle = (p * 3f) % 1f
        val intensity = (1f - cycle * 1.4f).coerceIn(0.25f, 1f)
        val unit = minOf(width, height).coerceAtLeast(64f)

        val boltPath = Path()
        val startX = if (leftOrigin) width * 0.28f else width * 0.72f
        val dir = if (leftOrigin) 1f else -1f
        val jitter = sin(p * PI * 18.0).toFloat() * width * 0.04f

        boltPath.moveTo(startX, 0f)
        boltPath.lineTo(startX + dir * width * 0.10f + jitter, height * 0.24f)
        boltPath.lineTo(startX - dir * width * 0.04f, height * 0.28f)
        boltPath.lineTo(startX + dir * width * 0.18f + jitter, height * 0.54f)
        boltPath.lineTo(startX + dir * width * 0.04f, height * 0.58f)
        boltPath.lineTo(startX + dir * width * 0.24f + jitter, height * 0.88f)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (unit * 0.06f).coerceAtLeast(4f)
            color = glowColor
            alpha = (intensity * 180).toInt().coerceIn(0, 255)
        }
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (unit * 0.022f).coerceAtLeast(1.8f)
            color = coreColor
            alpha = (intensity * 255).toInt().coerceIn(0, 255)
        }

        canvas.drawPath(boltPath, glowPaint)
        canvas.drawPath(boltPath, corePaint)
    }

    fun buildColorFilter(
        preset: FilterPreset,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f
    ): ColorMatrixColorFilter {
        val cm = ColorMatrix()

        when (preset) {
            FilterPreset.ORIGINAL -> Unit
            FilterPreset.WARM_SUNSET -> {
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.14f, 0f, 0f, 0f, 16f,
                    0f, 1.02f, 0f, 0f, 6f,
                    0f, 0f, 0.84f, 0f, -8f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.TEAL_ORANGE -> {
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.10f, 0.04f, -0.05f, 0f, 6f,
                    0f, 1.02f, 0.06f, 0f, 2f,
                    -0.06f, 0.08f, 1.12f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.VIVID_NEON -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(1.42f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.10f, 0f, 0f, 0f, 4f,
                    0f, 1.04f, 0f, 0f, 0f,
                    0f, 0f, 1.16f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.NOIR_BW -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(0f) })
                val c = 1.24f
                val t = (-0.5f * c + 0.5f) * 255f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.VINTAGE_FILM -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(0.75f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.05f, 0.06f, 0f, 0f, 12f,
                    0f, 0.98f, 0.03f, 0f, 8f,
                    0f, 0f, 0.84f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.ARCTIC_ICE -> {
                cm.postConcat(ColorMatrix(floatArrayOf(
                    0.86f, 0f, 0.06f, 0f, -6f,
                    0f, 1.02f, 0.08f, 0f, 6f,
                    0.06f, 0.08f, 1.22f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.GOLDEN_AUTUMN -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(1.18f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.18f, 0.08f, 0f, 0f, 12f,
                    0.04f, 1.06f, 0f, 0f, 4f,
                    0f, 0f, 0.74f, 0f, -12f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.CYBERPUNK -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(1.45f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.18f, 0f, 0.14f, 0f, 6f,
                    0f, 0.88f, 0.10f, 0f, -6f,
                    0.12f, 0.05f, 1.24f, 0f, 16f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.EMERALD_FOREST -> {
                cm.postConcat(ColorMatrix(floatArrayOf(
                    0.88f, 0.04f, 0f, 0f, -6f,
                    0.04f, 1.16f, 0.04f, 0f, 10f,
                    0f, 0.06f, 0.96f, 0f, 2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.SEPIA_CLASSIC -> {
                cm.postConcat(ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.HIGH_CONTRAST -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(1.22f) })
                val c = 1.32f
                val t = (-0.5f * c + 0.5f) * 255f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.SOFT_PASTEL -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(0.82f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.02f, 0f, 0.04f, 0f, 20f,
                    0.02f, 1.02f, 0.02f, 0f, 18f,
                    0.04f, 0f, 1.05f, 0f, 24f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.MIDNIGHT_BLUE -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(0.9f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    0.78f, 0f, 0.06f, 0f, -14f,
                    0f, 0.88f, 0.10f, 0f, -6f,
                    0.05f, 0.12f, 1.24f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.DRAMA_DARK -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(0.68f) })
                val c = 1.28f
                val t = (-0.5f * c + 0.5f) * 255f - 18f
                cm.postConcat(ColorMatrix(floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            FilterPreset.SUNRISE_GLOW -> {
                cm.postConcat(ColorMatrix().apply { setSaturation(1.22f) })
                cm.postConcat(ColorMatrix(floatArrayOf(
                    1.20f, 0.06f, 0f, 0f, 18f,
                    0.04f, 1.04f, 0f, 0f, 10f,
                    0f, 0.02f, 0.90f, 0f, -2f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
        }

        if (saturation != 1f) {
            cm.postConcat(ColorMatrix().apply { setSaturation(saturation) })
        }
        if (contrast != 1f || brightness != 0f) {
            val c = contrast
            val b = brightness * 255f
            val translate = (-0.5f * c + 0.5f) * 255f + b
            cm.postConcat(ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }

        return ColorMatrixColorFilter(cm)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun easeInOutCubic(t: Float): Float =
        if (t < 0.5f) 4f * t * t * t else 1f - ((-2f * t + 2f).let { it * it * it }) / 2f

    fun easeOutCubic(t: Float): Float =
        1f - (1f - t).let { it * it * it }

    fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val tm1 = t - 1f
        return 1f + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1
    }

    private fun easeOutBounce(x: Float): Float {
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> {
                val t = x - 1.5f / d1
                n1 * t * t + 0.75f
            }
            x < 2.5f / d1 -> {
                val t = x - 2.25f / d1
                n1 * t * t + 0.9375f
            }
            else -> {
                val t = x - 2.625f / d1
                n1 * t * t + 0.984375f
            }
        }
    }
}
