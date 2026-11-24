package com.example.model

enum class AnimRevealMode {
    NONE,
    CURTAIN_H,
    CURTAIN_V,
    IRIS_CIRCLE,
    DIAMOND_IN,
    BLINDS_H,
    BLINDS_V,
    SPLIT_DIAGONAL
}

enum class AnimFxOverlay {
    NONE,
    LIGHTNING_BOLT_CYAN,
    LIGHTNING_BOLT_GOLD,
    LIGHTNING_DUAL_STORM,
    ELECTRIC_ARC_BORDER,
    THUNDER_FLASH,
    STROBE_BEAT,
    NEON_SWEEP,
    LASER_SCAN_H,
    LASER_SCAN_V,
    RGB_GLITCH_BARS,
    CHROMATIC_PULSE,
    VIGNETTE_DARK,
    SPOTLIGHT_BEAM,
    RAIN_STREAKS,
    SNOW_SPARKLES,
    EMBER_FIRE,
    SPEED_LINES,
    BOKEH_LIGHTS,
    FILM_GRAIN_DUST,
    CINEMA_LETTERBOX,
    PRISM_AURORA
}

enum class AnimAlphaCurve {
    ALWAYS_ONE,
    FADE_IN_FAST,
    FADE_IN_SLOW,
    FADE_OUT,
    FADE_IN_OUT,
    FLICKER_ELECTRIC
}

data class AnimationSpec(
    val id: String,
    val number: Int,
    val name: String,
    val category: MotionCategory,
    val zoomFrom: Float = 1f,
    val zoomTo: Float = 1f,
    val zoomPulseAmp: Float = 0f,
    val zoomPulseFreq: Float = 0f,
    val panFromX: Float = 0f,
    val panToX: Float = 0f,
    val panFromY: Float = 0f,
    val panToY: Float = 0f,
    val orbitX: Float = 0f,
    val orbitY: Float = 0f,
    val rotateFromDeg: Float = 0f,
    val rotateToDeg: Float = 0f,
    val rotateSwingAmp: Float = 0f,
    val rotateSwingFreq: Float = 0f,
    val skewAmpX: Float = 0f,
    val skewAmpY: Float = 0f,
    val shakeAmp: Float = 0f,
    val shakeFreq: Float = 0f,
    val bounceDrop: Boolean = false,
    val elasticZoom: Boolean = false,
    val alphaCurve: AnimAlphaCurve = AnimAlphaCurve.ALWAYS_ONE,
    val revealMode: AnimRevealMode = AnimRevealMode.NONE,
    val fxOverlay: AnimFxOverlay = AnimFxOverlay.NONE
)

object AnimationCatalog {

    val animations: List<AnimationSpec> = buildCatalog()

    private val byId: Map<String, AnimationSpec> by lazy {
        animations.associateBy { it.id }
    }

    fun getById(id: String?): AnimationSpec {
        if (!id.isNullOrBlank()) {
            byId[id]?.let { return it }
        }
        return animations.first()
    }

    fun fromLegacyMotion(effect: MotionEffect): AnimationSpec {
        return when (effect) {
            MotionEffect.NONE -> animations[0]
            MotionEffect.KEN_BURNS_IN -> animations[1]
            MotionEffect.KEN_BURNS_OUT -> animations[2]
            MotionEffect.FAST_ZOOM_IN -> animations[3]
            MotionEffect.FAST_ZOOM_OUT -> animations[4]
            MotionEffect.ZOOM_PUNCH -> animations[5]
            MotionEffect.ELASTIC_ZOOM -> animations[6]
            MotionEffect.DRAMATIC_PUSH -> animations[7]
            MotionEffect.DRAMATIC_PULL -> animations[8]
            MotionEffect.BREATHE_ZOOM -> animations[9]
            MotionEffect.PAN_LEFT -> animations[25]
            MotionEffect.PAN_RIGHT -> animations[26]
            MotionEffect.PAN_UP -> animations[27]
            MotionEffect.PAN_DOWN -> animations[28]
            MotionEffect.DIAGONAL_NW_SE -> animations[29]
            MotionEffect.DIAGONAL_NE_SW -> animations[30]
            MotionEffect.SLIDE_IN_LEFT -> animations[31]
            MotionEffect.SLIDE_IN_RIGHT -> animations[32]
            MotionEffect.SLIDE_IN_BOTTOM -> animations[33]
            MotionEffect.SLIDE_IN_TOP -> animations[34]
            MotionEffect.ROTATE_CW_SLOW -> animations[50]
            MotionEffect.ROTATE_CCW_SLOW -> animations[51]
            MotionEffect.SPIN_IN_CW -> animations[52]
            MotionEffect.SPIN_IN_CCW -> animations[53]
            MotionEffect.SWING_PENDULUM -> animations[54]
            MotionEffect.TILT_3D_LEFT -> animations[55]
            MotionEffect.TILT_3D_RIGHT -> animations[56]
            MotionEffect.VORTEX_TWIST -> animations[57]
            MotionEffect.ROCK_N_ROLL -> animations[58]
            MotionEffect.FLOAT_ORBIT -> animations[59]
            MotionEffect.BEAT_PULSE -> animations[75]
            MotionEffect.HEARTBEAT_DOUBLE -> animations[76]
            MotionEffect.FLASH_IN -> animations[77]
            MotionEffect.STROBE_PULSE -> animations[78]
            MotionEffect.NEON_GLOW_SWEEP -> animations[79]
            MotionEffect.FADE_IN_SOFT -> animations[80]
            MotionEffect.FADE_OUT_SOFT -> animations[81]
            MotionEffect.FADE_IN_OUT -> animations[82]
            MotionEffect.VIGNETTE_FOCUS -> animations[150]
            MotionEffect.SPOTLIGHT_REVEAL -> animations[83]
            MotionEffect.GLITCH_SHAKE -> animations[100]
            MotionEffect.CAMERA_SHAKE_HEAVY -> animations[101]
            MotionEffect.MICRO_HANDHELD -> animations[102]
            MotionEffect.RGB_SPLIT_PULSE -> animations[103]
            MotionEffect.CURTAIN_HORIZONTAL -> animations[125]
            MotionEffect.CURTAIN_VERTICAL -> animations[126]
            MotionEffect.IRIS_CIRCLE_IN -> animations[127]
            MotionEffect.BOUNCE_DROP -> animations[175]
            MotionEffect.WHIP_ZOOM_BLUR -> animations[176]
            MotionEffect.CINEMA_LETTERBOX -> animations[151]
        }
    }

    private fun buildCatalog(): List<AnimationSpec> {
        val list = ArrayList<AnimationSpec>(200)
        var num = 1
        fun add(
            name: String,
            category: MotionCategory,
            zoomFrom: Float = 1f,
            zoomTo: Float = 1f,
            zoomPulseAmp: Float = 0f,
            zoomPulseFreq: Float = 0f,
            panFromX: Float = 0f,
            panToX: Float = 0f,
            panFromY: Float = 0f,
            panToY: Float = 0f,
            orbitX: Float = 0f,
            orbitY: Float = 0f,
            rotateFromDeg: Float = 0f,
            rotateToDeg: Float = 0f,
            rotateSwingAmp: Float = 0f,
            rotateSwingFreq: Float = 0f,
            skewAmpX: Float = 0f,
            skewAmpY: Float = 0f,
            shakeAmp: Float = 0f,
            shakeFreq: Float = 0f,
            bounceDrop: Boolean = false,
            elasticZoom: Boolean = false,
            alphaCurve: AnimAlphaCurve = AnimAlphaCurve.ALWAYS_ONE,
            revealMode: AnimRevealMode = AnimRevealMode.NONE,
            fxOverlay: AnimFxOverlay = AnimFxOverlay.NONE
        ) {
            val id = String.format(java.util.Locale.US, "anim_%03d", num)
            list.add(
                AnimationSpec(
                    id = id,
                    number = num++,
                    name = name,
                    category = category,
                    zoomFrom = zoomFrom,
                    zoomTo = zoomTo,
                    zoomPulseAmp = zoomPulseAmp,
                    zoomPulseFreq = zoomPulseFreq,
                    panFromX = panFromX,
                    panToX = panToX,
                    panFromY = panFromY,
                    panToY = panToY,
                    orbitX = orbitX,
                    orbitY = orbitY,
                    rotateFromDeg = rotateFromDeg,
                    rotateToDeg = rotateToDeg,
                    rotateSwingAmp = rotateSwingAmp,
                    rotateSwingFreq = rotateSwingFreq,
                    skewAmpX = skewAmpX,
                    skewAmpY = skewAmpY,
                    shakeAmp = shakeAmp,
                    shakeFreq = shakeFreq,
                    bounceDrop = bounceDrop,
                    elasticZoom = elasticZoom,
                    alphaCurve = alphaCurve,
                    revealMode = revealMode,
                    fxOverlay = fxOverlay
                )
            )
        }

        // --- 1..25: ZOOM ---
        add("Statik (Asl)", MotionCategory.ZOOM)
        add("Sekin Zoom +", MotionCategory.ZOOM, zoomFrom = 1.0f, zoomTo = 1.16f)
        add("Sekin Zoom -", MotionCategory.ZOOM, zoomFrom = 1.16f, zoomTo = 1.0f)
        add("Tez Yaqinlashish", MotionCategory.ZOOM, zoomFrom = 0.84f, zoomTo = 1.12f)
        add("Tez Uzoqlashish", MotionCategory.ZOOM, zoomFrom = 1.35f, zoomTo = 1.0f)
        add("Zarbali Zoom", MotionCategory.ZOOM, zoomFrom = 0.85f, zoomTo = 1.08f, zoomPulseAmp = 0.05f, zoomPulseFreq = 2f)
        add("Elastik Zoom", MotionCategory.ZOOM, zoomFrom = 1.04f, zoomTo = 1.08f, elasticZoom = true)
        add("Kino Yaqinlashish", MotionCategory.ZOOM, zoomFrom = 1.0f, zoomTo = 1.30f)
        add("Kino Uzoqlashish", MotionCategory.ZOOM, zoomFrom = 1.30f, zoomTo = 1.0f)
        add("Nafas Zoom", MotionCategory.ZOOM, zoomFrom = 1.05f, zoomTo = 1.05f, zoomPulseAmp = 0.07f, zoomPulseFreq = 2f)
        add("Chuqur Makro Zoom", MotionCategory.ZOOM, zoomFrom = 1.0f, zoomTo = 1.45f)
        add("Teskari Makro", MotionCategory.ZOOM, zoomFrom = 1.45f, zoomTo = 1.0f)
        add("Qadamli Zoom 2x", MotionCategory.ZOOM, zoomFrom = 1.0f, zoomTo = 1.24f, zoomPulseAmp = 0.06f, zoomPulseFreq = 4f)
        add("Kichrayish Kirish", MotionCategory.ZOOM, zoomFrom = 0.72f, zoomTo = 1.0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Kengayish Chiqish", MotionCategory.ZOOM, zoomFrom = 1.0f, zoomTo = 1.25f, alphaCurve = AnimAlphaCurve.FADE_OUT)
        add("Fokus Urish", MotionCategory.ZOOM, zoomFrom = 1.08f, zoomTo = 1.0f, zoomPulseAmp = 0.08f, zoomPulseFreq = 3f)
        add("Giper Zoom +", MotionCategory.ZOOM, zoomFrom = 0.65f, zoomTo = 1.15f)
        add("Giper Zoom -", MotionCategory.ZOOM, zoomFrom = 1.55f, zoomTo = 1.0f)
        add("Yengil Puls Zoom", MotionCategory.ZOOM, zoomFrom = 1.02f, zoomTo = 1.06f, zoomPulseAmp = 0.04f, zoomPulseFreq = 5f)
        add("Burchak Zoom Chap", MotionCategory.ZOOM, zoomFrom = 1.1f, zoomTo = 1.28f, panFromX = 0f, panToX = 0.06f, panFromY = 0f, panToY = 0.04f)
        add("Burchak Zoom O'ng", MotionCategory.ZOOM, zoomFrom = 1.1f, zoomTo = 1.28f, panFromX = 0f, panToX = -0.06f, panFromY = 0f, panToY = -0.04f)
        add("Shovqinli Zoom", MotionCategory.ZOOM, zoomFrom = 1.02f, zoomTo = 1.18f, shakeAmp = 0.012f, shakeFreq = 12f)
        add("Teleobyektiv", MotionCategory.ZOOM, zoomFrom = 1.15f, zoomTo = 1.38f, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Keng Burchak", MotionCategory.ZOOM, zoomFrom = 0.90f, zoomTo = 1.05f, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Yumshoq Kirish Zoom", MotionCategory.ZOOM, zoomFrom = 1.14f, zoomTo = 1.0f, alphaCurve = AnimAlphaCurve.FADE_IN_SLOW)

        // --- 26..50: SLIDE & PAN ---
        add("Chapga Surish", MotionCategory.SLIDE, zoomFrom = 1.15f, zoomTo = 1.15f, panFromX = 0.06f, panToX = -0.06f)
        add("O'ngga Surish", MotionCategory.SLIDE, zoomFrom = 1.15f, zoomTo = 1.15f, panFromX = -0.06f, panToX = 0.06f)
        add("Yuqoriga Surish", MotionCategory.SLIDE, zoomFrom = 1.15f, zoomTo = 1.15f, panFromY = 0.06f, panToY = -0.06f)
        add("Pastga Surish", MotionCategory.SLIDE, zoomFrom = 1.15f, zoomTo = 1.15f, panFromY = -0.06f, panToY = 0.06f)
        add("Burchakdan Pastga", MotionCategory.SLIDE, zoomFrom = 1.22f, zoomTo = 1.22f, panFromX = -0.05f, panToX = 0.05f, panFromY = -0.05f, panToY = 0.05f)
        add("O'ngdan Chapga Qiya", MotionCategory.SLIDE, zoomFrom = 1.22f, zoomTo = 1.22f, panFromX = 0.05f, panToX = -0.05f, panFromY = -0.05f, panToY = 0.05f)
        add("Chapdan Kirish", MotionCategory.SLIDE, panFromX = -0.85f, panToX = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("O'ngdan Kirish", MotionCategory.SLIDE, panFromX = 0.85f, panToX = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Pastdan Chiqish", MotionCategory.SLIDE, panFromY = 0.85f, panToY = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Tepadan Tushish", MotionCategory.SLIDE, panFromY = -0.85f, panToY = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Tez Chap Uchish", MotionCategory.SLIDE, zoomFrom = 1.18f, zoomTo = 1.18f, panFromX = 0.14f, panToX = -0.14f)
        add("Tez O'ng Uchish", MotionCategory.SLIDE, zoomFrom = 1.18f, zoomTo = 1.18f, panFromX = -0.14f, panToX = 0.14f)
        add("Kino Panorama Chap", MotionCategory.SLIDE, zoomFrom = 1.25f, zoomTo = 1.25f, panFromX = 0.10f, panToX = -0.10f, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Kino Panorama O'ng", MotionCategory.SLIDE, zoomFrom = 1.25f, zoomTo = 1.25f, panFromX = -0.10f, panToX = 0.10f, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Dron Ko'tarilishi", MotionCategory.SLIDE, zoomFrom = 1.20f, zoomTo = 1.05f, panFromY = 0.08f, panToY = -0.06f)
        add("Dron Sho'ng'ishi", MotionCategory.SLIDE, zoomFrom = 1.05f, zoomTo = 1.25f, panFromY = -0.06f, panToY = 0.06f)
        add("Gorizontal Tebranish", MotionCategory.SLIDE, zoomFrom = 1.14f, zoomTo = 1.14f, orbitX = 0.05f)
        add("Vertikal Tebranish", MotionCategory.SLIDE, zoomFrom = 1.14f, zoomTo = 1.14f, orbitY = 0.05f)
        add("Pastki Chapdan Kirish", MotionCategory.SLIDE, panFromX = -0.55f, panToX = 0f, panFromY = 0.55f, panToY = 0f)
        add("Yuqori O'ngdan Kirish", MotionCategory.SLIDE, panFromX = 0.55f, panToX = 0f, panFromY = -0.55f, panToY = 0f)
        add("Yengil Chapga Yurish", MotionCategory.SLIDE, zoomFrom = 1.10f, zoomTo = 1.16f, panFromX = 0.04f, panToX = -0.05f)
        add("Yengil O'ngga Yurish", MotionCategory.SLIDE, zoomFrom = 1.10f, zoomTo = 1.16f, panFromX = -0.04f, panToX = 0.05f)
        add("Zigzag Surish", MotionCategory.SLIDE, zoomFrom = 1.16f, zoomTo = 1.16f, orbitX = 0.045f, shakeAmp = 0.015f, shakeFreq = 4f)
        add("Parallel Surilish", MotionCategory.SLIDE, zoomFrom = 1.20f, zoomTo = 1.10f, panFromX = -0.08f, panToX = 0.04f)
        add("Chiqib Ketish Chapga", MotionCategory.SLIDE, panFromX = 0f, panToX = -0.75f, alphaCurve = AnimAlphaCurve.FADE_OUT)

        // --- 51..75: ROTATE & 3D ---
        add("O'ngga Aylanish", MotionCategory.ROTATE, zoomFrom = 1.18f, zoomTo = 1.18f, rotateFromDeg = 0f, rotateToDeg = 10f)
        add("Chapga Aylanish", MotionCategory.ROTATE, zoomFrom = 1.18f, zoomTo = 1.18f, rotateFromDeg = 0f, rotateToDeg = -10f)
        add("Aylanib Kirish +", MotionCategory.ROTATE, zoomFrom = 0.72f, zoomTo = 1.06f, rotateFromDeg = -95f, rotateToDeg = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Aylanib Kirish -", MotionCategory.ROTATE, zoomFrom = 0.72f, zoomTo = 1.06f, rotateFromDeg = 95f, rotateToDeg = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Mayatnik Tebranish", MotionCategory.ROTATE, zoomFrom = 1.18f, zoomTo = 1.18f, rotateSwingAmp = 6.5f, rotateSwingFreq = 2f)
        add("3D Chapga Og'ish", MotionCategory.ROTATE, zoomFrom = 1.16f, zoomTo = 1.16f, skewAmpY = -0.07f, panFromX = 0f, panToX = -0.03f)
        add("3D O'ngga Og'ish", MotionCategory.ROTATE, zoomFrom = 1.16f, zoomTo = 1.16f, skewAmpY = 0.07f, panFromX = 0f, panToX = 0.03f)
        add("Girdob Burilish", MotionCategory.ROTATE, zoomFrom = 1.06f, zoomTo = 1.24f, rotateFromDeg = -12f, rotateToDeg = 12f, skewAmpX = 0.05f)
        add("Yengil Silkinish", MotionCategory.ROTATE, zoomFrom = 1.14f, zoomTo = 1.14f, rotateSwingAmp = 4f, rotateSwingFreq = 4f)
        add("Suzuvchi Orbita", MotionCategory.ROTATE, zoomFrom = 1.16f, zoomTo = 1.16f, orbitX = 0.035f, orbitY = 0.028f)
        add("360° To'liq Aylanish", MotionCategory.ROTATE, zoomFrom = 0.85f, zoomTo = 1.05f, rotateFromDeg = 0f, rotateToDeg = 360f)
        add("Teskari 360°", MotionCategory.ROTATE, zoomFrom = 0.85f, zoomTo = 1.05f, rotateFromDeg = 360f, rotateToDeg = 0f)
        add("3D Perspektiva Tepaga", MotionCategory.ROTATE, zoomFrom = 1.20f, zoomTo = 1.20f, skewAmpX = 0.08f, panFromY = 0.03f, panToY = -0.03f)
        add("3D Perspektiva Pastga", MotionCategory.ROTATE, zoomFrom = 1.20f, zoomTo = 1.20f, skewAmpX = -0.08f, panFromY = -0.03f, panToY = 0.03f)
        add("Burchak Burilish 15°", MotionCategory.ROTATE, zoomFrom = 1.22f, zoomTo = 1.22f, rotateFromDeg = -15f, rotateToDeg = 0f)
        add("Burchak Burilish -15°", MotionCategory.ROTATE, zoomFrom = 1.22f, zoomTo = 1.22f, rotateFromDeg = 15f, rotateToDeg = 0f)
        add("Spiral Kirish", MotionCategory.ROTATE, zoomFrom = 0.45f, zoomTo = 1.05f, rotateFromDeg = -180f, rotateToDeg = 0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Spiral Chiqish", MotionCategory.ROTATE, zoomFrom = 1.05f, zoomTo = 1.45f, rotateFromDeg = 0f, rotateToDeg = 45f, alphaCurve = AnimAlphaCurve.FADE_OUT)
        add("To'lqinli Egilish", MotionCategory.ROTATE, zoomFrom = 1.18f, zoomTo = 1.18f, skewAmpX = 0.05f, skewAmpY = 0.05f, rotateSwingAmp = 3f, rotateSwingFreq = 3f)
        add("Kema Tebranishi", MotionCategory.ROTATE, zoomFrom = 1.16f, zoomTo = 1.16f, rotateSwingAmp = 5f, rotateSwingFreq = 2f, orbitY = 0.02f)
        add("Kino Tilt O'ng", MotionCategory.ROTATE, zoomFrom = 1.15f, zoomTo = 1.25f, rotateFromDeg = -4f, rotateToDeg = 6f)
        add("Kino Tilt Chap", MotionCategory.ROTATE, zoomFrom = 1.15f, zoomTo = 1.25f, rotateFromDeg = 4f, rotateToDeg = -6f)
        add("3D Kitob Ochilishi", MotionCategory.ROTATE, zoomFrom = 1.12f, zoomTo = 1.05f, skewAmpY = 0.14f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Dinamik Burilish Zoom", MotionCategory.ROTATE, zoomFrom = 1.0f, zoomTo = 1.28f, rotateFromDeg = 0f, rotateToDeg = 18f)
        add("Sokin Orbita +", MotionCategory.ROTATE, zoomFrom = 1.12f, zoomTo = 1.22f, orbitX = 0.025f, orbitY = 0.025f)

        // --- 76..100: LIGHTNING & LIGHT (CHAQMOQ & NUR) ---
        add("Ritmda Urish", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.02f, zoomPulseAmp = 0.09f, zoomPulseFreq = 4f)
        add("Qo'sh Yurak Zarbasi", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.02f, zoomPulseAmp = 0.10f, zoomPulseFreq = 6f)
        add("Chaqnash Kirish", MotionCategory.LIGHTNING, zoomFrom = 1.05f, zoomTo = 1.0f, fxOverlay = AnimFxOverlay.THUNDER_FLASH)
        add("Stroboskop Zarba", MotionCategory.LIGHTNING, zoomFrom = 1.04f, zoomTo = 1.04f, zoomPulseAmp = 0.05f, zoomPulseFreq = 6f, fxOverlay = AnimFxOverlay.STROBE_BEAT)
        add("Neon Nur O'tishi", MotionCategory.LIGHTNING, zoomFrom = 1.04f, zoomTo = 1.10f, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Yumshoq Paydo", MotionCategory.LIGHTNING, zoomFrom = 1.03f, zoomTo = 1.0f, alphaCurve = AnimAlphaCurve.FADE_IN_SLOW)
        add("Yumshoq So'nish", MotionCategory.LIGHTNING, zoomFrom = 1.0f, zoomTo = 1.03f, alphaCurve = AnimAlphaCurve.FADE_OUT)
        add("Kirish va Chiqish", MotionCategory.LIGHTNING, zoomFrom = 1.0f, zoomTo = 1.06f, alphaCurve = AnimAlphaCurve.FADE_IN_OUT)
        add("Projektor Nuri", MotionCategory.LIGHTNING, zoomFrom = 1.08f, zoomTo = 1.02f, fxOverlay = AnimFxOverlay.SPOTLIGHT_BEAM)
        add("Chaqmoq Zarbasi Moviy", MotionCategory.LIGHTNING, zoomFrom = 1.04f, zoomTo = 1.08f, shakeAmp = 0.018f, shakeFreq = 16f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_CYAN)
        add("Chaqmoq Zarbasi Oltin", MotionCategory.LIGHTNING, zoomFrom = 1.04f, zoomTo = 1.08f, shakeAmp = 0.018f, shakeFreq = 16f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_GOLD)
        add("Momaqaldiroq Bo'roni", MotionCategory.LIGHTNING, zoomFrom = 1.06f, zoomTo = 1.14f, shakeAmp = 0.025f, shakeFreq = 20f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Elektr Ramka Chaqmog'i", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.06f, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Elektr Miltillash", MotionCategory.LIGHTNING, zoomFrom = 1.05f, zoomTo = 1.05f, alphaCurve = AnimAlphaCurve.FLICKER_ELECTRIC, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_CYAN)
        add("Lazer Skaner Gorizontal", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.08f, fxOverlay = AnimFxOverlay.LASER_SCAN_H)
        add("Lazer Skaner Vertikal", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.08f, fxOverlay = AnimFxOverlay.LASER_SCAN_V)
        add("Shimol Yog'dusi (Aurora)", MotionCategory.LIGHTNING, zoomFrom = 1.0f, zoomTo = 1.10f, fxOverlay = AnimFxOverlay.PRISM_AURORA)
        add("Chaqmoq + Zoom Zarba", MotionCategory.LIGHTNING, zoomFrom = 0.88f, zoomTo = 1.12f, zoomPulseAmp = 0.06f, zoomPulseFreq = 4f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Tonggi Chaqnash", MotionCategory.LIGHTNING, zoomFrom = 1.0f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.THUNDER_FLASH)
        add("Neon Impuls", MotionCategory.LIGHTNING, zoomFrom = 1.03f, zoomTo = 1.03f, zoomPulseAmp = 0.06f, zoomPulseFreq = 5f, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Disko Stroboskop", MotionCategory.LIGHTNING, zoomFrom = 1.06f, zoomTo = 1.06f, zoomPulseAmp = 0.08f, zoomPulseFreq = 8f, fxOverlay = AnimFxOverlay.STROBE_BEAT)
        add("Qizg'in Yorug'lik", MotionCategory.LIGHTNING, zoomFrom = 1.04f, zoomTo = 1.14f, fxOverlay = AnimFxOverlay.SPOTLIGHT_BEAM)
        add("Oltin Nur Qamrovi", MotionCategory.LIGHTNING, zoomFrom = 1.02f, zoomTo = 1.09f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_GOLD)
        add("Elektr Tok Urishi", MotionCategory.LIGHTNING, zoomFrom = 1.08f, zoomTo = 1.08f, shakeAmp = 0.022f, shakeFreq = 24f, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Yaltirash Kirish", MotionCategory.LIGHTNING, zoomFrom = 1.12f, zoomTo = 1.0f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST, fxOverlay = AnimFxOverlay.THUNDER_FLASH)

        // --- 101..125: GLITCH & SHAKE ---
        add("Glitch Silkinish", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.10f, shakeAmp = 0.026f, shakeFreq = 22f, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Kuchli Titratish", MotionCategory.GLITCH, zoomFrom = 1.14f, zoomTo = 1.14f, shakeAmp = 0.032f, shakeFreq = 16f, rotateSwingAmp = 2.5f, rotateSwingFreq = 11f)
        add("Qo'l Kamerasi", MotionCategory.GLITCH, zoomFrom = 1.08f, zoomTo = 1.08f, shakeAmp = 0.011f, shakeFreq = 5f, rotateSwingAmp = 1.2f, rotateSwingFreq = 3f)
        add("RGB Rang Ajralishi", MotionCategory.GLITCH, zoomFrom = 1.06f, zoomTo = 1.06f, zoomPulseAmp = 0.04f, zoomPulseFreq = 4f, fxOverlay = AnimFxOverlay.CHROMATIC_PULSE)
        add("Kiber Glitch 2x", MotionCategory.GLITCH, zoomFrom = 1.12f, zoomTo = 1.12f, shakeAmp = 0.034f, shakeFreq = 28f, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Zilzila Zarbasi", MotionCategory.GLITCH, zoomFrom = 1.16f, zoomTo = 1.16f, shakeAmp = 0.040f, shakeFreq = 19f, rotateSwingAmp = 3.5f, rotateSwingFreq = 14f)
        add("Televizor Shovqini", MotionCategory.GLITCH, zoomFrom = 1.08f, zoomTo = 1.08f, alphaCurve = AnimAlphaCurve.FLICKER_ELECTRIC, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Gorizontal Silkish", MotionCategory.GLITCH, zoomFrom = 1.14f, zoomTo = 1.14f, shakeAmp = 0.035f, shakeFreq = 14f)
        add("Bass Titratishi", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.10f, zoomPulseAmp = 0.07f, zoomPulseFreq = 6f, shakeAmp = 0.018f, shakeFreq = 12f)
        add("Mikro Glitch", MotionCategory.GLITCH, zoomFrom = 1.06f, zoomTo = 1.10f, shakeAmp = 0.014f, shakeFreq = 24f, fxOverlay = AnimFxOverlay.CHROMATIC_PULSE)
        add("Xromatik Silkinish", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.10f, shakeAmp = 0.022f, shakeFreq = 18f, fxOverlay = AnimFxOverlay.CHROMATIC_PULSE)
        add("Elektro Glitch", MotionCategory.GLITCH, zoomFrom = 1.12f, zoomTo = 1.12f, shakeAmp = 0.024f, shakeFreq = 20f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_CYAN)
        add("Signal Uzilishi", MotionCategory.GLITCH, zoomFrom = 1.09f, zoomTo = 1.09f, alphaCurve = AnimAlphaCurve.FLICKER_ELECTRIC, fxOverlay = AnimFxOverlay.LASER_SCAN_H)
        add("Kuchli Zarba Kirish", MotionCategory.GLITCH, zoomFrom = 1.28f, zoomTo = 1.08f, shakeAmp = 0.038f, shakeFreq = 20f)
        add("Qaltirash Kino", MotionCategory.GLITCH, zoomFrom = 1.12f, zoomTo = 1.16f, shakeAmp = 0.016f, shakeFreq = 9f, fxOverlay = AnimFxOverlay.FILM_GRAIN_DUST)
        add("Neon Glitch", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.10f, shakeAmp = 0.020f, shakeFreq = 22f, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Skaner Glitch", MotionCategory.GLITCH, zoomFrom = 1.08f, zoomTo = 1.08f, shakeAmp = 0.018f, shakeFreq = 18f, fxOverlay = AnimFxOverlay.LASER_SCAN_V)
        add("O'q Tezligi Titratish", MotionCategory.GLITCH, zoomFrom = 1.14f, zoomTo = 1.24f, shakeAmp = 0.025f, shakeFreq = 25f, fxOverlay = AnimFxOverlay.SPEED_LINES)
        add("Tebranuvchi Zoom", MotionCategory.GLITCH, zoomFrom = 1.02f, zoomTo = 1.22f, rotateSwingAmp = 3f, rotateSwingFreq = 8f)
        add("Kamera Zarbasi", MotionCategory.GLITCH, zoomFrom = 1.18f, zoomTo = 1.10f, shakeAmp = 0.030f, shakeFreq = 14f)
        add("Chaqmoq Glitch", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.12f, shakeAmp = 0.026f, shakeFreq = 22f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Prizma Glitch", MotionCategory.GLITCH, zoomFrom = 1.08f, zoomTo = 1.08f, shakeAmp = 0.016f, shakeFreq = 16f, fxOverlay = AnimFxOverlay.PRISM_AURORA)
        add("Kiberpuls", MotionCategory.GLITCH, zoomFrom = 1.05f, zoomTo = 1.15f, zoomPulseAmp = 0.06f, zoomPulseFreq = 5f, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Yuqori Kuchlanish", MotionCategory.GLITCH, zoomFrom = 1.10f, zoomTo = 1.10f, shakeAmp = 0.028f, shakeFreq = 26f, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Reels Zarba", MotionCategory.GLITCH, zoomFrom = 0.90f, zoomTo = 1.14f, shakeAmp = 0.024f, shakeFreq = 18f)

        // --- 126..150: MASK & CURTAIN ---
        add("Parda Gorizontal", MotionCategory.MASK, zoomFrom = 1.06f, zoomTo = 1.06f, revealMode = AnimRevealMode.CURTAIN_H)
        add("Parda Vertikal", MotionCategory.MASK, zoomFrom = 1.06f, zoomTo = 1.06f, revealMode = AnimRevealMode.CURTAIN_V)
        add("Doira Ochilishi", MotionCategory.MASK, zoomFrom = 1.12f, zoomTo = 1.02f, revealMode = AnimRevealMode.IRIS_CIRCLE)
        add("Romb Ochilishi", MotionCategory.MASK, zoomFrom = 1.10f, zoomTo = 1.02f, revealMode = AnimRevealMode.DIAMOND_IN)
        add("Jalyuzi Gorizontal", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.05f, revealMode = AnimRevealMode.BLINDS_H)
        add("Jalyuzi Vertikal", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.05f, revealMode = AnimRevealMode.BLINDS_V)
        add("Qiya Parda", MotionCategory.MASK, zoomFrom = 1.08f, zoomTo = 1.02f, revealMode = AnimRevealMode.SPLIT_DIAGONAL)
        add("Doira + Zoom", MotionCategory.MASK, zoomFrom = 1.25f, zoomTo = 1.0f, revealMode = AnimRevealMode.IRIS_CIRCLE)
        add("Parda + Chaqnash", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.08f, revealMode = AnimRevealMode.CURTAIN_H, fxOverlay = AnimFxOverlay.THUNDER_FLASH)
        add("Romb + Neon", MotionCategory.MASK, zoomFrom = 1.04f, zoomTo = 1.12f, revealMode = AnimRevealMode.DIAMOND_IN, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Jalyuzi + Zoom", MotionCategory.MASK, zoomFrom = 1.0f, zoomTo = 1.16f, revealMode = AnimRevealMode.BLINDS_V)
        add("Kino Parda", MotionCategory.MASK, zoomFrom = 1.08f, zoomTo = 1.14f, revealMode = AnimRevealMode.CURTAIN_V, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Doira + Chaqmoq", MotionCategory.MASK, zoomFrom = 1.08f, zoomTo = 1.04f, revealMode = AnimRevealMode.IRIS_CIRCLE, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_CYAN)
        add("Qiya + Aylanish", MotionCategory.MASK, zoomFrom = 1.18f, zoomTo = 1.12f, rotateFromDeg = -6f, rotateToDeg = 0f, revealMode = AnimRevealMode.SPLIT_DIAGONAL)
        add("Jalyuzi + Lazer", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.08f, revealMode = AnimRevealMode.BLINDS_H, fxOverlay = AnimFxOverlay.LASER_SCAN_H)
        add("Romb + Oltin Chaqmoq", MotionCategory.MASK, zoomFrom = 1.06f, zoomTo = 1.10f, revealMode = AnimRevealMode.DIAMOND_IN, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_GOLD)
        add("Gorizontal Parda Zoom+", MotionCategory.MASK, zoomFrom = 1.0f, zoomTo = 1.20f, revealMode = AnimRevealMode.CURTAIN_H)
        add("Vertikal Parda Zoom-", MotionCategory.MASK, zoomFrom = 1.22f, zoomTo = 1.0f, revealMode = AnimRevealMode.CURTAIN_V)
        add("Doira Fokus", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.12f, revealMode = AnimRevealMode.IRIS_CIRCLE, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Jalyuzi Kino", MotionCategory.MASK, zoomFrom = 1.12f, zoomTo = 1.04f, revealMode = AnimRevealMode.BLINDS_H, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Parda + Elektr", MotionCategory.MASK, zoomFrom = 1.04f, zoomTo = 1.08f, revealMode = AnimRevealMode.CURTAIN_H, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Qiya Parda + Glitch", MotionCategory.MASK, zoomFrom = 1.12f, zoomTo = 1.12f, shakeAmp = 0.015f, shakeFreq = 18f, revealMode = AnimRevealMode.SPLIT_DIAGONAL, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Doira + Bokeh", MotionCategory.MASK, zoomFrom = 1.02f, zoomTo = 1.12f, revealMode = AnimRevealMode.IRIS_CIRCLE, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Romb + Aurora", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.10f, revealMode = AnimRevealMode.DIAMOND_IN, fxOverlay = AnimFxOverlay.PRISM_AURORA)
        add("Jalyuzi + Chaqnash", MotionCategory.MASK, zoomFrom = 1.05f, zoomTo = 1.05f, revealMode = AnimRevealMode.BLINDS_V, fxOverlay = AnimFxOverlay.THUNDER_FLASH)

        // --- 151..175: CINEMA & ATMOSPHERE ---
        add("Kino Fokus", MotionCategory.CINEMA, zoomFrom = 1.0f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Kino Ramka Kirishi", MotionCategory.CINEMA, zoomFrom = 1.04f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Yomg'ir Atmosferasi", MotionCategory.CINEMA, zoomFrom = 1.02f, zoomTo = 1.10f, fxOverlay = AnimFxOverlay.RAIN_STREAKS)
        add("Qor Uchqunlari", MotionCategory.CINEMA, zoomFrom = 1.02f, zoomTo = 1.08f, fxOverlay = AnimFxOverlay.SNOW_SPARKLES)
        add("Olovli Uchqunlar", MotionCategory.CINEMA, zoomFrom = 1.04f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.EMBER_FIRE)
        add("Tezlik Chiziqlari", MotionCategory.CINEMA, zoomFrom = 1.0f, zoomTo = 1.24f, fxOverlay = AnimFxOverlay.SPEED_LINES)
        add("Bokeh Chiroqlar", MotionCategory.CINEMA, zoomFrom = 1.02f, zoomTo = 1.10f, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Retro Plyonka Chang", MotionCategory.CINEMA, zoomFrom = 1.05f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.FILM_GRAIN_DUST)
        add("Yomg'ir + Chaqmoq", MotionCategory.CINEMA, zoomFrom = 1.04f, zoomTo = 1.10f, shakeAmp = 0.012f, shakeFreq = 16f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Kino Pan + Bokeh", MotionCategory.CINEMA, zoomFrom = 1.15f, zoomTo = 1.15f, panFromX = -0.05f, panToX = 0.05f, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Kino Pan + Yomg'ir", MotionCategory.CINEMA, zoomFrom = 1.15f, zoomTo = 1.15f, panFromX = 0.05f, panToX = -0.05f, fxOverlay = AnimFxOverlay.RAIN_STREAKS)
        add("Qishki Ertak", MotionCategory.CINEMA, zoomFrom = 1.08f, zoomTo = 1.0f, fxOverlay = AnimFxOverlay.SNOW_SPARKLES)
        add("Kino Drama + Ramka", MotionCategory.CINEMA, zoomFrom = 1.18f, zoomTo = 1.04f, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Olovli Jang", MotionCategory.CINEMA, zoomFrom = 1.08f, zoomTo = 1.18f, shakeAmp = 0.015f, shakeFreq = 14f, fxOverlay = AnimFxOverlay.EMBER_FIRE)
        add("Anime Tezlik", MotionCategory.CINEMA, zoomFrom = 0.90f, zoomTo = 1.25f, fxOverlay = AnimFxOverlay.SPEED_LINES)
        add("Analog 35mm", MotionCategory.CINEMA, zoomFrom = 1.08f, zoomTo = 1.08f, shakeAmp = 0.008f, shakeFreq = 7f, fxOverlay = AnimFxOverlay.FILM_GRAIN_DUST)
        add("Sokin Tungi Fokus", MotionCategory.CINEMA, zoomFrom = 1.12f, zoomTo = 1.02f, alphaCurve = AnimAlphaCurve.FADE_IN_SLOW, fxOverlay = AnimFxOverlay.VIGNETTE_DARK)
        add("Kino Projektor", MotionCategory.CINEMA, zoomFrom = 1.02f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.SPOTLIGHT_BEAM)
        add("Shimoliy Qish", MotionCategory.CINEMA, zoomFrom = 1.0f, zoomTo = 1.10f, fxOverlay = AnimFxOverlay.PRISM_AURORA)
        add("Issiq Uchqun Zoom", MotionCategory.CINEMA, zoomFrom = 1.15f, zoomTo = 1.02f, fxOverlay = AnimFxOverlay.EMBER_FIRE)
        add("Kino Xulosa", MotionCategory.CINEMA, zoomFrom = 1.10f, zoomTo = 1.0f, alphaCurve = AnimAlphaCurve.FADE_OUT, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Yorug' Bokeh Pan", MotionCategory.CINEMA, zoomFrom = 1.14f, zoomTo = 1.20f, panFromY = 0.04f, panToY = -0.04f, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Kuzgi Yomg'ir", MotionCategory.CINEMA, zoomFrom = 1.10f, zoomTo = 1.02f, fxOverlay = AnimFxOverlay.RAIN_STREAKS)
        add("Kino Tilt + Ramka", MotionCategory.CINEMA, zoomFrom = 1.16f, zoomTo = 1.16f, rotateFromDeg = -4f, rotateToDeg = 4f, fxOverlay = AnimFxOverlay.CINEMA_LETTERBOX)
        add("Retro Xotira", MotionCategory.CINEMA, zoomFrom = 1.0f, zoomTo = 1.10f, alphaCurve = AnimAlphaCurve.FADE_IN_OUT, fxOverlay = AnimFxOverlay.FILM_GRAIN_DUST)

        // --- 176..200: COMBO PRO ---
        add("Sakrab Tushish", MotionCategory.COMBO, zoomFrom = 1.05f, zoomTo = 1.05f, bounceDrop = true)
        add("Tezkor Qamchi Zoom", MotionCategory.COMBO, zoomFrom = 2.0f, zoomTo = 1.04f, alphaCurve = AnimAlphaCurve.FADE_IN_FAST)
        add("Chaqmoq + Aylanish", MotionCategory.COMBO, zoomFrom = 0.85f, zoomTo = 1.12f, rotateFromDeg = -22f, rotateToDeg = 0f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_CYAN)
        add("Chaqmoq + 3D Og'ish", MotionCategory.COMBO, zoomFrom = 1.14f, zoomTo = 1.14f, skewAmpY = 0.08f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_GOLD)
        add("Glitch + Tez Zoom", MotionCategory.COMBO, zoomFrom = 1.35f, zoomTo = 1.04f, shakeAmp = 0.024f, shakeFreq = 20f, fxOverlay = AnimFxOverlay.RGB_GLITCH_BARS)
        add("Neon + Elastik", MotionCategory.COMBO, zoomFrom = 1.05f, zoomTo = 1.10f, elasticZoom = true, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Bo'ron + Tezlik", MotionCategory.COMBO, zoomFrom = 0.92f, zoomTo = 1.24f, shakeAmp = 0.022f, shakeFreq = 20f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Sakrab + Chaqnash", MotionCategory.COMBO, zoomFrom = 1.04f, zoomTo = 1.08f, bounceDrop = true, fxOverlay = AnimFxOverlay.THUNDER_FLASH)
        add("Girdob + Lazer", MotionCategory.COMBO, zoomFrom = 1.08f, zoomTo = 1.22f, rotateFromDeg = -14f, rotateToDeg = 14f, fxOverlay = AnimFxOverlay.LASER_SCAN_H)
        add("Orbit + Aurora", MotionCategory.COMBO, zoomFrom = 1.16f, zoomTo = 1.16f, orbitX = 0.04f, orbitY = 0.03f, fxOverlay = AnimFxOverlay.PRISM_AURORA)
        add("Reels Combo #1", MotionCategory.COMBO, zoomFrom = 0.86f, zoomTo = 1.14f, rotateFromDeg = -8f, rotateToDeg = 4f, fxOverlay = AnimFxOverlay.STROBE_BEAT)
        add("Reels Combo #2", MotionCategory.COMBO, zoomFrom = 1.26f, zoomTo = 1.02f, panFromX = -0.08f, panToX = 0f, fxOverlay = AnimFxOverlay.NEON_SWEEP)
        add("Reels Combo #3", MotionCategory.COMBO, zoomFrom = 1.05f, zoomTo = 1.18f, zoomPulseAmp = 0.07f, zoomPulseFreq = 5f, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Kino Combo + Olov", MotionCategory.COMBO, zoomFrom = 1.04f, zoomTo = 1.16f, fxOverlay = AnimFxOverlay.EMBER_FIRE)
        add("Kino Combo + Qor", MotionCategory.COMBO, zoomFrom = 1.16f, zoomTo = 1.04f, rotateFromDeg = 3f, rotateToDeg = -3f, fxOverlay = AnimFxOverlay.SNOW_SPARKLES)
        add("TikTok Zarba Pro", MotionCategory.COMBO, zoomFrom = 0.82f, zoomTo = 1.10f, zoomPulseAmp = 0.08f, zoomPulseFreq = 6f, fxOverlay = AnimFxOverlay.SPEED_LINES)
        add("Musiqiy Parda Pro", MotionCategory.COMBO, zoomFrom = 1.04f, zoomTo = 1.14f, revealMode = AnimRevealMode.CURTAIN_H, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Elektr Girdob", MotionCategory.COMBO, zoomFrom = 1.10f, zoomTo = 1.25f, rotateFromDeg = -16f, rotateToDeg = 16f, fxOverlay = AnimFxOverlay.ELECTRIC_ARC_BORDER)
        add("Super Chaqmoq Pro", MotionCategory.COMBO, zoomFrom = 1.02f, zoomTo = 1.16f, zoomPulseAmp = 0.07f, zoomPulseFreq = 6f, shakeAmp = 0.02f, shakeFreq = 22f, fxOverlay = AnimFxOverlay.LIGHTNING_DUAL_STORM)
        add("Xromatik Orbita", MotionCategory.COMBO, zoomFrom = 1.15f, zoomTo = 1.15f, orbitX = 0.035f, orbitY = 0.035f, fxOverlay = AnimFxOverlay.CHROMATIC_PULSE)
        add("Qamchi + Aylanish", MotionCategory.COMBO, zoomFrom = 1.9f, zoomTo = 1.04f, rotateFromDeg = 35f, rotateToDeg = 0f)
        add("Lazer + Glitch", MotionCategory.COMBO, zoomFrom = 1.10f, zoomTo = 1.10f, shakeAmp = 0.020f, shakeFreq = 20f, fxOverlay = AnimFxOverlay.LASER_SCAN_V)
        add("Oltin Chaqmoq Kino", MotionCategory.COMBO, zoomFrom = 1.06f, zoomTo = 1.16f, fxOverlay = AnimFxOverlay.LIGHTNING_BOLT_GOLD)
        add("Bokeh + Tez Zoom", MotionCategory.COMBO, zoomFrom = 0.88f, zoomTo = 1.12f, fxOverlay = AnimFxOverlay.BOKEH_LIGHTS)
        add("Lavelle Signature FX", MotionCategory.COMBO, zoomFrom = 1.02f, zoomTo = 1.14f, zoomPulseAmp = 0.05f, zoomPulseFreq = 4f, fxOverlay = AnimFxOverlay.PRISM_AURORA)

        return list
    }
}
