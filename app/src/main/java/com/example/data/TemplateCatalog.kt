package com.example.data

import com.example.model.AspectRatioOption
import com.example.model.ClipItem
import com.example.model.FilterPreset
import com.example.model.MotionEffect
import com.example.model.MusicTrack
import com.example.model.TemplateSpec
import com.example.model.TextAnimationStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType

object TemplateCatalog {

    val categories = listOf(
        "Barchasi",
        "Beat Sync",
        "Reels",
        "Sayohat",
        "Kino",
        "Xotira"
    )

    val builtInMusicLibrary = listOf(
        MusicTrack(
            id = "track_neon_pulse",
            title = "Neon Pulse",
            artist = "Lavelle Studio",
            bpm = 124,
            durationMs = 20_000L,
            builtInPreset = "neon_pulse",
            beatMarksMs = generateBeats(124, 20_000L)
        ),
        MusicTrack(
            id = "track_golden_horizon",
            title = "Oltin Ufq",
            artist = "Lavelle Acoustic",
            bpm = 98,
            durationMs = 22_000L,
            builtInPreset = "golden_horizon",
            beatMarksMs = generateBeats(98, 22_000L)
        ),
        MusicTrack(
            id = "track_cinema_epic",
            title = "Kino Chaqmoq",
            artist = "Lavelle Score",
            bpm = 86,
            durationMs = 22_000L,
            builtInPreset = "cinema_epic",
            beatMarksMs = generateBeats(86, 22_000L)
        ),
        MusicTrack(
            id = "track_lofi_memory",
            title = "Xotira Yomg'iri",
            artist = "Lavelle Lo-Fi",
            bpm = 82,
            durationMs = 22_000L,
            builtInPreset = "lofi_memory",
            beatMarksMs = generateBeats(82, 22_000L)
        ),
        MusicTrack(
            id = "track_trap_reels",
            title = "Turbo Beat",
            artist = "Lavelle Bass",
            bpm = 135,
            durationMs = 18_000L,
            builtInPreset = "trap_reels",
            beatMarksMs = generateBeats(135, 18_000L)
        ),
        MusicTrack(
            id = "track_minimal_ambient",
            title = "Sokin Tong",
            artist = "Lavelle Minimal",
            bpm = 105,
            durationMs = 20_000L,
            builtInPreset = "minimal_ambient",
            beatMarksMs = generateBeats(105, 20_000L)
        )
    )

    val templates: List<TemplateSpec> = listOf(
        TemplateSpec(
            id = "tpl_neon_beat",
            title = "Neon Beat Sync",
            subtitle = "Tezkor beat almashinuvi, chaqnash va glitch effektlari",
            category = "Beat Sync",
            coverDrawableName = "img_tpl_neon_beat",
            accentColorHex = 0xFF00E5FFL,
            aspectRatio = AspectRatioOption.PORTRAIT_9_16,
            bpm = 124,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Kirish",
                    sampleDrawableName = "img_tpl_neon_beat",
                    durationMs = 1450L,
                    transition = TransitionType.FLASH_BEAT,
                    transitionDurationMs = 350L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.VIVID_NEON
                ),
                ClipItem(
                    slotLabel = "2 • Zarba",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 970L,
                    transition = TransitionType.ZOOM_IN,
                    transitionDurationMs = 300L,
                    motion = MotionEffect.BEAT_PULSE,
                    filter = FilterPreset.VIVID_NEON
                ),
                ClipItem(
                    slotLabel = "3 • Ritm",
                    sampleDrawableName = "img_tpl_neon_beat",
                    durationMs = 970L,
                    transition = TransitionType.GLITCH_SHAKE,
                    transitionDurationMs = 300L,
                    motion = MotionEffect.PAN_LEFT,
                    filter = FilterPreset.VIVID_NEON
                ),
                ClipItem(
                    slotLabel = "4 • Avj",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 1450L,
                    transition = TransitionType.SLIDE_LEFT,
                    transitionDurationMs = 380L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.TEAL_ORANGE
                ),
                ClipItem(
                    slotLabel = "5 • Final",
                    sampleDrawableName = "img_tpl_neon_beat",
                    durationMs = 1940L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 450L,
                    motion = MotionEffect.BEAT_PULSE,
                    filter = FilterPreset.VIVID_NEON
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "NEON NIGHTS",
                    startMs = 150L,
                    endMs = 1750L,
                    xFraction = 0.5f,
                    yFraction = 0.80f,
                    fontSizeSp = 26f,
                    colorArgb = 0xFF00E5FFL,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.POP_SCALE
                ),
                TextOverlayItem(
                    text = "TUNGI RITM",
                    startMs = 3400L,
                    endMs = 6400L,
                    xFraction = 0.5f,
                    yFraction = 0.18f,
                    fontSizeSp = 22f,
                    colorArgb = 0xFFFFFFFFL,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.GLOW_PULSE
                )
            ),
            music = builtInMusicLibrary[0]
        ),
        TemplateSpec(
            id = "tpl_golden_travel",
            title = "Oltin Sayohat",
            subtitle = "Sayohat va vloglar uchun iliq kino ranglari va mayin o'tishlar",
            category = "Sayohat",
            coverDrawableName = "img_tpl_golden_travel",
            accentColorHex = 0xFFFFB84DL,
            aspectRatio = AspectRatioOption.PORTRAIT_9_16,
            bpm = 98,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Manzil",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 2200L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 600L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.WARM_SUNSET
                ),
                ClipItem(
                    slotLabel = "2 • Yo'l",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 1800L,
                    transition = TransitionType.SLIDE_RIGHT,
                    transitionDurationMs = 500L,
                    motion = MotionEffect.PAN_RIGHT,
                    filter = FilterPreset.WARM_SUNSET
                ),
                ClipItem(
                    slotLabel = "3 • Quyosh",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 1800L,
                    transition = TransitionType.ZOOM_OUT,
                    transitionDurationMs = 500L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.WARM_SUNSET
                ),
                ClipItem(
                    slotLabel = "4 • Xotira",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 2400L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 650L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.VINTAGE_FILM
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "SAYOHAT KUNDALIGI",
                    startMs = 300L,
                    endMs = 2800L,
                    xFraction = 0.5f,
                    yFraction = 0.78f,
                    fontSizeSp = 22f,
                    colorArgb = 0xFFFFE0B2L,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.FADE_UP
                )
            ),
            music = builtInMusicLibrary[1]
        ),
        TemplateSpec(
            id = "tpl_turbo_reels",
            title = "Turbo Reels 6-Kadr",
            subtitle = "Instagram Reels & TikTok uchun 6 ta tezkor dinamik kadr",
            category = "Reels",
            coverDrawableName = "img_tpl_neon_beat",
            accentColorHex = 0xFFFF4D8DL,
            aspectRatio = AspectRatioOption.PORTRAIT_9_16,
            bpm = 135,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Start",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 900L,
                    transition = TransitionType.ZOOM_IN,
                    transitionDurationMs = 260L,
                    motion = MotionEffect.BEAT_PULSE,
                    filter = FilterPreset.TEAL_ORANGE
                ),
                ClipItem(
                    slotLabel = "2 • Tez",
                    sampleDrawableName = "img_tpl_neon_beat",
                    durationMs = 850L,
                    transition = TransitionType.FLASH_BEAT,
                    transitionDurationMs = 260L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.VIVID_NEON
                ),
                ClipItem(
                    slotLabel = "3 • Burilish",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 850L,
                    transition = TransitionType.SLIDE_LEFT,
                    transitionDurationMs = 280L,
                    motion = MotionEffect.PAN_LEFT,
                    filter = FilterPreset.TEAL_ORANGE
                ),
                ClipItem(
                    slotLabel = "4 • Zarba",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 850L,
                    transition = TransitionType.GLITCH_SHAKE,
                    transitionDurationMs = 260L,
                    motion = MotionEffect.BEAT_PULSE,
                    filter = FilterPreset.VIVID_NEON
                ),
                ClipItem(
                    slotLabel = "5 • Kadr",
                    sampleDrawableName = "img_tpl_neon_beat",
                    durationMs = 950L,
                    transition = TransitionType.ZOOM_OUT,
                    transitionDurationMs = 300L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.TEAL_ORANGE
                ),
                ClipItem(
                    slotLabel = "6 • Final",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 1500L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 400L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.TEAL_ORANGE
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "TREND #1",
                    startMs = 100L,
                    endMs = 1600L,
                    xFraction = 0.5f,
                    yFraction = 0.22f,
                    fontSizeSp = 28f,
                    colorArgb = 0xFFFF4D8DL,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.POP_SCALE
                )
            ),
            music = builtInMusicLibrary[4]
        ),
        TemplateSpec(
            id = "tpl_cinema_drama",
            title = "Kino Drama 16:9",
            subtitle = "Anamorfik kino rangi, sekin zoom va chuqur atmosfera",
            category = "Kino",
            coverDrawableName = "img_tpl_cinema_noir",
            accentColorHex = 0xFF4DD0E1L,
            aspectRatio = AspectRatioOption.LANDSCAPE_16_9,
            bpm = 86,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Prolog",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 2600L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 750L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.TEAL_ORANGE
                ),
                ClipItem(
                    slotLabel = "2 • Sahna",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 2200L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 700L,
                    motion = MotionEffect.PAN_LEFT,
                    filter = FilterPreset.NOIR_BW
                ),
                ClipItem(
                    slotLabel = "3 • Epilog",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 2600L,
                    transition = TransitionType.ZOOM_OUT,
                    transitionDurationMs = 700L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.TEAL_ORANGE
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "A LAVELLE FILM",
                    startMs = 400L,
                    endMs = 3000L,
                    xFraction = 0.5f,
                    yFraction = 0.82f,
                    fontSizeSp = 20f,
                    colorArgb = 0xFFFFFFFFL,
                    hasBackdrop = false,
                    animation = TextAnimationStyle.FADE_UP
                )
            ),
            music = builtInMusicLibrary[2]
        ),
        TemplateSpec(
            id = "tpl_memory_vibe",
            title = "Xotira Lenta",
            subtitle = "Issiq retro film, yumshoq o'tishlar va Lo-Fi musiqa",
            category = "Xotira",
            coverDrawableName = "img_tpl_memory_vibe",
            accentColorHex = 0xFFFFCA80L,
            aspectRatio = AspectRatioOption.CLASSIC_4_5,
            bpm = 82,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Bahor",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 2200L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 650L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.VINTAGE_FILM
                ),
                ClipItem(
                    slotLabel = "2 • Lahza",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 2000L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 600L,
                    motion = MotionEffect.PAN_RIGHT,
                    filter = FilterPreset.WARM_SUNSET
                ),
                ClipItem(
                    slotLabel = "3 • Tabassum",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 2000L,
                    transition = TransitionType.ZOOM_IN,
                    transitionDurationMs = 550L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.VINTAGE_FILM
                ),
                ClipItem(
                    slotLabel = "4 • Abadiy",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 2500L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 700L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.VINTAGE_FILM
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "ENG GO'ZAL LAHZALAR",
                    startMs = 250L,
                    endMs = 3100L,
                    xFraction = 0.5f,
                    yFraction = 0.82f,
                    fontSizeSp = 21f,
                    colorArgb = 0xFFFFF3E0L,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.FADE_UP
                )
            ),
            music = builtInMusicLibrary[3]
        ),
        TemplateSpec(
            id = "tpl_minimal_square",
            title = "Minimal 1:1 Studio",
            subtitle = "Instagram post va mahsulot taqdimotlari uchun toza uslub",
            category = "Reels",
            coverDrawableName = "img_tpl_golden_travel",
            accentColorHex = 0xFF2EE59DL,
            aspectRatio = AspectRatioOption.SQUARE_1_1,
            bpm = 105,
            clips = listOf(
                ClipItem(
                    slotLabel = "1 • Asosiy",
                    sampleDrawableName = "img_tpl_golden_travel",
                    durationMs = 1700L,
                    transition = TransitionType.SLIDE_LEFT,
                    transitionDurationMs = 420L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.ORIGINAL
                ),
                ClipItem(
                    slotLabel = "2 • Tafsilot",
                    sampleDrawableName = "img_tpl_memory_vibe",
                    durationMs = 1700L,
                    transition = TransitionType.SLIDE_LEFT,
                    transitionDurationMs = 420L,
                    motion = MotionEffect.KEN_BURNS_OUT,
                    filter = FilterPreset.ORIGINAL
                ),
                ClipItem(
                    slotLabel = "3 • Yakun",
                    sampleDrawableName = "img_tpl_cinema_noir",
                    durationMs = 2000L,
                    transition = TransitionType.CROSSFADE,
                    transitionDurationMs = 500L,
                    motion = MotionEffect.KEN_BURNS_IN,
                    filter = FilterPreset.TEAL_ORANGE
                )
            ),
            texts = listOf(
                TextOverlayItem(
                    text = "MINIMAL STUDIO",
                    startMs = 200L,
                    endMs = 2200L,
                    xFraction = 0.5f,
                    yFraction = 0.84f,
                    fontSizeSp = 20f,
                    colorArgb = 0xFFFFFFFFL,
                    hasBackdrop = true,
                    animation = TextAnimationStyle.FADE_UP
                )
            ),
            music = builtInMusicLibrary[5]
        )
    )

    private fun generateBeats(bpm: Int, durationMs: Long): List<Long> {
        val intervalMs = (60_000.0 / bpm).toLong().coerceAtLeast(250L)
        val beats = mutableListOf<Long>()
        var t = 0L
        while (t < durationMs) {
            beats.add(t)
            t += intervalMs
        }
        return beats
    }
}
