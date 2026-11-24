package com.example.model

import java.util.UUID
import kotlin.math.roundToInt

enum class AspectRatioOption(
    val label: String,
    val shortLabel: String,
    val ratio: Float,
    val exportWidth: Int,
    val exportHeight: Int
) {
    PORTRAIT_9_16("9:16 Reels", "9:16", 9f / 16f, 720, 1280),
    CLASSIC_4_5("4:5 Insta", "4:5", 4f / 5f, 720, 900),
    SQUARE_1_1("1:1 Kvadrat", "1:1", 1f, 720, 720),
    LANDSCAPE_16_9("16:9 Kino", "16:9", 16f / 9f, 1280, 720);

    fun dimensionsFor(resolution: ExportResolution): Pair<Int, Int> {
        val base = resolution.shortSidePx
        return when (this) {
            PORTRAIT_9_16 -> base to makeEven((base * 16f / 9f).roundToInt())
            CLASSIC_4_5 -> base to makeEven((base * 5f / 4f).roundToInt())
            SQUARE_1_1 -> base to base
            LANDSCAPE_16_9 -> makeEven((base * 16f / 9f).roundToInt()) to base
        }
    }

    private fun makeEven(v: Int): Int = if (v % 2 == 0) v else v + 1
}

enum class ExportResolution(val label: String, val shortSidePx: Int, val bitrate30: Int, val bitrate60: Int) {
    HD_720("720p HD", 720, 4_500_000, 6_500_000),
    FHD_1080("1080p Full HD", 1080, 8_500_000, 12_000_000)
}

enum class ExportFps(val label: String, val fps: Int) {
    FPS_30("30 FPS", 30),
    FPS_60("60 FPS", 60)
}

enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Yuqori chap"),
    TOP_RIGHT("Yuqori o'ng"),
    BOTTOM_LEFT("Pastki chap"),
    BOTTOM_RIGHT("Pastki o'ng")
}

data class WatermarkConfig(
    val enabled: Boolean = false,
    val text: String = "@Lavelle",
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT
)

enum class ClipMediaType {
    IMAGE,
    VIDEO
}

enum class ClipFitMode(val label: String) {
    COVER("To'ldirish"),
    CONTAIN("To'liq sig'dirish")
}

enum class ClipBackgroundStyle(val label: String) {
    BLUR("Blur fon"),
    GRADIENT("Gradient"),
    BLACK("Qora fon")
}

enum class TransitionType(val label: String, val badgeCode: String) {
    NONE("Kesish", "CUT"),
    CROSSFADE("Yumshoq", "MIX"),
    ZOOM_IN("Zoom +", "Z+"),
    ZOOM_OUT("Zoom -", "Z-"),
    SLIDE_LEFT("Chapga", "SL"),
    SLIDE_RIGHT("O'ngga", "SR"),
    FLASH_BEAT("Chaqnash", "FL"),
    GLITCH_SHAKE("Glitch", "GL")
}

enum class MotionCategory(val label: String) {
    ALL("Barchasi (200)"),
    ZOOM("Zoom"),
    SLIDE("Siljish"),
    ROTATE("Aylanish & 3D"),
    LIGHTNING("Chaqmoq & Nur"),
    GLITCH("Glitch & Titratish"),
    MASK("Parda & Shakl"),
    CINEMA("Kino & Atmosfera"),
    COMBO("Combo Pro")
}

enum class MotionEffect(
    val label: String,
    val category: MotionCategory
) {
    NONE("Statik (Asl)", MotionCategory.ZOOM),
    KEN_BURNS_IN("Sekin Zoom +", MotionCategory.ZOOM),
    KEN_BURNS_OUT("Sekin Zoom -", MotionCategory.ZOOM),
    FAST_ZOOM_IN("Tez Yaqinlashish", MotionCategory.ZOOM),
    FAST_ZOOM_OUT("Tez Uzoqlashish", MotionCategory.ZOOM),
    ZOOM_PUNCH("Zarbali Zoom", MotionCategory.ZOOM),
    ELASTIC_ZOOM("Elastik Zoom", MotionCategory.ZOOM),
    DRAMATIC_PUSH("Kino Yaqinlashish", MotionCategory.ZOOM),
    DRAMATIC_PULL("Kino Uzoqlashish", MotionCategory.ZOOM),
    BREATHE_ZOOM("Nafas Zoom", MotionCategory.ZOOM),
    PAN_LEFT("Chapga Surish", MotionCategory.SLIDE),
    PAN_RIGHT("O'ngga Surish", MotionCategory.SLIDE),
    PAN_UP("Yuqoriga Surish", MotionCategory.SLIDE),
    PAN_DOWN("Pastga Surish", MotionCategory.SLIDE),
    DIAGONAL_NW_SE("Burchakdan Pastga", MotionCategory.SLIDE),
    DIAGONAL_NE_SW("O'ngdan Chapga Qiya", MotionCategory.SLIDE),
    SLIDE_IN_LEFT("Chapdan Kirish", MotionCategory.SLIDE),
    SLIDE_IN_RIGHT("O'ngdan Kirish", MotionCategory.SLIDE),
    SLIDE_IN_BOTTOM("Pastdan Chiqish", MotionCategory.SLIDE),
    SLIDE_IN_TOP("Tepadan Tushish", MotionCategory.SLIDE),
    ROTATE_CW_SLOW("O'ngga Aylanish", MotionCategory.ROTATE),
    ROTATE_CCW_SLOW("Chapga Aylanish", MotionCategory.ROTATE),
    SPIN_IN_CW("Aylanib Kirish +", MotionCategory.ROTATE),
    SPIN_IN_CCW("Aylanib Kirish -", MotionCategory.ROTATE),
    SWING_PENDULUM("Mayatnik Tebranish", MotionCategory.ROTATE),
    TILT_3D_LEFT("3D Chapga Og'ish", MotionCategory.ROTATE),
    TILT_3D_RIGHT("3D O'ngga Og'ish", MotionCategory.ROTATE),
    VORTEX_TWIST("Girdob Burilish", MotionCategory.ROTATE),
    ROCK_N_ROLL("Yengil Silkinish", MotionCategory.ROTATE),
    FLOAT_ORBIT("Suzuvchi Orbita", MotionCategory.ROTATE),
    BEAT_PULSE("Ritmda Urish", MotionCategory.LIGHTNING),
    HEARTBEAT_DOUBLE("Qo'sh Yurak Zarbasi", MotionCategory.LIGHTNING),
    FLASH_IN("Chaqnash Kirish", MotionCategory.LIGHTNING),
    STROBE_PULSE("Stroboskop Zarba", MotionCategory.LIGHTNING),
    NEON_GLOW_SWEEP("Neon Nur O'tishi", MotionCategory.LIGHTNING),
    FADE_IN_SOFT("Yumshoq Paydo", MotionCategory.LIGHTNING),
    FADE_OUT_SOFT("Yumshoq So'nish", MotionCategory.LIGHTNING),
    FADE_IN_OUT("Kirish va Chiqish", MotionCategory.LIGHTNING),
    VIGNETTE_FOCUS("Kino Fokus", MotionCategory.CINEMA),
    SPOTLIGHT_REVEAL("Projektor Nuri", MotionCategory.LIGHTNING),
    GLITCH_SHAKE("Glitch Silkinish", MotionCategory.GLITCH),
    CAMERA_SHAKE_HEAVY("Kuchli Titratish", MotionCategory.GLITCH),
    MICRO_HANDHELD("Qo'l Kamerasi", MotionCategory.GLITCH),
    RGB_SPLIT_PULSE("RGB Rang Ajralishi", MotionCategory.GLITCH),
    CURTAIN_HORIZONTAL("Parda Gorizontal", MotionCategory.MASK),
    CURTAIN_VERTICAL("Parda Vertikal", MotionCategory.MASK),
    IRIS_CIRCLE_IN("Doira Ochilishi", MotionCategory.MASK),
    BOUNCE_DROP("Sakrab Tushish", MotionCategory.COMBO),
    WHIP_ZOOM_BLUR("Tezkor Qamchi Zoom", MotionCategory.COMBO),
    CINEMA_LETTERBOX("Kino Ramka Kirishi", MotionCategory.CINEMA)
}

enum class FilterPreset(val label: String) {
    ORIGINAL("Asl"),
    WARM_SUNSET("Quyoshli"),
    TEAL_ORANGE("Kino Teal"),
    VIVID_NEON("Neon"),
    NOIR_BW("Oq-Qora"),
    VINTAGE_FILM("Retro"),
    ARCTIC_ICE("Sovuq Muz"),
    GOLDEN_AUTUMN("Oltin Kuz"),
    CYBERPUNK("Cyberpunk"),
    EMERALD_FOREST("Zumrad"),
    SEPIA_CLASSIC("Sepia"),
    HIGH_CONTRAST("Keskin HDR"),
    SOFT_PASTEL("Mayin Pastel"),
    MIDNIGHT_BLUE("Tungi Moviy"),
    DRAMA_DARK("Drama Qora"),
    SUNRISE_GLOW("Tonggi Nur")
}

enum class TextAnimationStyle(val label: String) {
    FADE_UP("Pastdan"),
    POP_SCALE("Sakrab"),
    GLOW_PULSE("Yorqin"),
    STATIC("Oddiy")
}

enum class TextFontStyle(val label: String) {
    DISPLAY("Space Grotesk"),
    MODERN("Jakarta Sans"),
    MONO("Kino Mono"),
    SERIF("Klassik Serif"),
    HANDWRITING("Qo'lyozma")
}

enum class StickerKind(val label: String, val isFullFrame: Boolean) {
    POLAROID("Polaroid Ramka", true),
    VHS_RETRO("VHS Retro", true),
    CINEMA_REC("Kamera REC", true),
    NEON_FRAME("Neon Ramka", true),
    HEART("Yurakcha", false),
    FIRE("Olov", false),
    LIGHTNING("Chaqmoq", false),
    CROWN("Toj", false),
    STAR("Yulduz", false),
    MUSIC_VIBE("Musiqa", false),
    SPARKLES("Uchqun", false),
    SUNGLASSES("Ko'zoynak", false)
}

data class ClipItem(
    val id: String = UUID.randomUUID().toString(),
    val slotLabel: String = "Kadr",
    val mediaType: ClipMediaType = ClipMediaType.IMAGE,
    val imagePath: String? = null,
    val videoPath: String? = null,
    val videoDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val clipVolume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val sampleDrawableName: String? = null,
    val durationMs: Long = 1600L,
    val transition: TransitionType = TransitionType.CROSSFADE,
    val transitionDurationMs: Long = 450L,
    val motion: MotionEffect = MotionEffect.NONE,
    val animationId: String = "anim_001",
    val filter: FilterPreset = FilterPreset.ORIGINAL,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val zoomScale: Float = 1f,
    val rotationQuarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val fitMode: ClipFitMode = ClipFitMode.COVER,
    val backgroundStyle: ClipBackgroundStyle = ClipBackgroundStyle.BLUR
)

data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.78f,
    val fontSizeSp: Float = 24f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val fontStyle: TextFontStyle = TextFontStyle.DISPLAY,
    val colorArgb: Long = 0xFFFFFFFFL,
    val hasBackdrop: Boolean = true,
    val animation: TextAnimationStyle = TextAnimationStyle.FADE_UP
)

data class StickerOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: StickerKind,
    val startMs: Long,
    val endMs: Long,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f
)

data class VoiceoverItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Ovoz yozuv",
    val filePath: String,
    val startMs: Long = 0L,
    val durationMs: Long = 3000L,
    val volume: Float = 1f
)

data class MusicTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val bpm: Int,
    val durationMs: Long,
    val builtInPreset: String? = null,
    val audioFilePath: String? = null,
    val volume: Float = 0.9f,
    val trimStartMs: Long = 0L,
    val fadeInMs: Long = 350L,
    val fadeOutMs: Long = 700L,
    val beatMarksMs: List<Long> = emptyList()
)

data class TemplateSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val coverDrawableName: String,
    val coverImagePath: String? = null,
    val previewVideoPath: String? = null,
    val isCloudTemplate: Boolean = false,
    val cloudLvlBase64: String? = null,
    val accentColorHex: Long,
    val aspectRatio: AspectRatioOption,
    val bpm: Int,
    val clips: List<ClipItem>,
    val texts: List<TextOverlayItem>,
    val music: MusicTrack
) {
    val slotCount: Int get() = clips.size
    val totalDurationMs: Long get() = clips.sumOf { it.durationMs }
}

data class MontageProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Yangi Montaj",
    val templateId: String? = null,
    val aspectRatio: AspectRatioOption = AspectRatioOption.PORTRAIT_9_16,
    val clips: List<ClipItem> = emptyList(),
    val texts: List<TextOverlayItem> = emptyList(),
    val stickers: List<StickerOverlayItem> = emptyList(),
    val voiceovers: List<VoiceoverItem> = emptyList(),
    val watermark: WatermarkConfig = WatermarkConfig(),
    val music: MusicTrack? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.durationMs }.coerceAtLeast(1000L)
}

sealed interface LvlLoadingState {
    data object Idle : LvlLoadingState
    data class Loading(
        val fileName: String,
        val stepLabel: String,
        val progress: Float
    ) : LvlLoadingState
    data class Error(val message: String) : LvlLoadingState
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(
        val formatLabel: String,
        val stepLabel: String,
        val progress: Float
    ) : ExportState
    data class Success(
        val formatLabel: String,
        val displayPath: String,
        val shareUriString: String?,
        val mimeType: String
    ) : ExportState
    data class Failed(val message: String) : ExportState
}

// --- Cloud, Auth, Admin & Statistics Models ---

data class FirebaseConfig(
    val projectId: String = "",
    val apiKey: String = "",
    val databaseUrl: String = "",
    val appId: String = "",
    val storageBucket: String = ""
) {
    val isConfigured: Boolean
        get() = databaseUrl.isNotBlank() || (projectId.isNotBlank() && apiKey.isNotBlank())
}

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val montageCount: Int = 0
)

data class CloudNotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val sentAt: Long = System.currentTimeMillis()
)

data class AppStatistics(
    val totalUsers: Int = 0,
    val totalCloudTemplates: Int = 0,
    val totalBuiltInTemplates: Int = 6,
    val totalMontagesCreated: Int = 0,
    val totalExports: Int = 0,
    val firebaseConnected: Boolean = false,
    val firebaseLatencyMs: Long = -1L,
    val lastSyncTimeMs: Long = 0L,
    val appFrameLatencyMs: Long = 16L,
    val notificationCount: Int = 0
)
