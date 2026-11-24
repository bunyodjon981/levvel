package com.example

import com.example.audio.BeatSyncAnalyzer
import com.example.cloud.AuthSecurity
import com.example.data.TemplateCatalog
import com.example.model.AnimationCatalog
import com.example.model.AspectRatioOption
import com.example.model.ClipItem
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.FirebaseConfig
import com.example.model.MontageProject
import com.example.model.MotionEffect
import com.example.model.StickerKind
import com.example.model.TextFontStyle
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun templateCatalog_hasValidTemplatesAndMusic() {
        assertTrue(TemplateCatalog.templates.isNotEmpty())
        TemplateCatalog.templates.forEach { tpl ->
            assertTrue(tpl.clips.isNotEmpty())
            assertTrue(tpl.totalDurationMs > 0L)
            assertTrue(tpl.music.beatMarksMs.isNotEmpty())
        }
    }

    @Test
    fun animationCatalog_has200DistinctAnimations_andMapsAllLegacyMotions() {
        assertEquals(200, AnimationCatalog.animations.size)
        assertEquals(200, AnimationCatalog.animations.map { it.id }.distinct().size)
        assertEquals(200, AnimationCatalog.animations.map { it.name }.distinct().size)
        assertEquals(50, MotionEffect.entries.size)
        MotionEffect.entries.forEach { effect ->
            val mapped = AnimationCatalog.fromLegacyMotion(effect)
            assertTrue(mapped.id.startsWith("anim_"))
        }
    }

    @Test
    fun filterPresets_hasSixteenProfessionalFilters() {
        assertEquals(16, FilterPreset.entries.size)
        assertEquals(16, FilterPreset.entries.map { it.label }.distinct().size)
    }

    @Test
    fun authSecurity_verifiesAdminAndHashesUserPasswords() {
        assertTrue(AuthSecurity.isAdminSearchTrigger("admin2009"))
        assertFalse(AuthSecurity.isAdminSearchTrigger("neon"))
        assertTrue(AuthSecurity.verifyAdminPassword("200922"))
        assertFalse(AuthSecurity.verifyAdminPassword("123456"))

        val salt = AuthSecurity.generateSaltHex()
        val hash = AuthSecurity.hashPassword("secret123", salt)
        assertTrue(AuthSecurity.verifyPassword("secret123", salt, hash))
        assertFalse(AuthSecurity.verifyPassword("wrong", salt, hash))

        assertNull(AuthSecurity.validateRegistration("alijon", "ali@mail.uz", "parol123", "parol123"))
        assertNotNull(AuthSecurity.validateRegistration("alijon", "ali@mail.uz", "parol123", "other123"))
    }

    @Test
    fun firebaseConfig_isConfiguredCheck() {
        assertFalse(FirebaseConfig().isConfigured)
        assertTrue(FirebaseConfig(databaseUrl = "https://demo-default-rtdb.firebaseio.com").isConfigured)
        assertTrue(FirebaseConfig(projectId = "demo-id", apiKey = "AIzaTest").isConfigured)
        assertFalse(FirebaseConfig(projectId = "demo-id", apiKey = "").isConfigured)
    }

    @Test
    fun exportDimensions_returnsEven1080pAnd720pSizes() {
        assertEquals(1080 to 1920, AspectRatioOption.PORTRAIT_9_16.dimensionsFor(ExportResolution.FHD_1080))
        assertEquals(1920 to 1080, AspectRatioOption.LANDSCAPE_16_9.dimensionsFor(ExportResolution.FHD_1080))
        assertEquals(720 to 1280, AspectRatioOption.PORTRAIT_9_16.dimensionsFor(ExportResolution.HD_720))
        AspectRatioOption.entries.forEach { ratio ->
            ExportResolution.entries.forEach { res ->
                val (w, h) = ratio.dimensionsFor(res)
                assertEquals(0, w % 2)
                assertEquals(0, h % 2)
            }
        }
    }

    @Test
    fun beatSyncAnalyzer_alignsClipsToBeatGrid() {
        val project = MontageProject(
            name = "Beat Test",
            clips = List(4) { ClipItem(slotLabel = "Kadr $it", durationMs = 2400L) },
            music = TemplateCatalog.builtInMusicLibrary.first()
        )
        val synced = BeatSyncAnalyzer.applyAutoBeatSync(project)
        assertEquals(4, synced.clips.size)
        assertTrue(synced.clips.all { it.durationMs in 500L..4500L })
        assertTrue(synced.clips.all { it.transitionDurationMs in 180L..420L })
        assertNotNull(synced.music)

        val noMusicSynced = BeatSyncAnalyzer.applyAutoBeatSync(project.copy(music = null))
        assertEquals(4, noMusicSynced.clips.size)
        assertTrue(noMusicSynced.clips.all { it.durationMs in 500L..4500L })
    }

    @Test
    fun proFeaturesEnums_haveExpectedStickersFontsAndWatermarkDefaults() {
        assertEquals(12, StickerKind.entries.size)
        assertEquals(4, StickerKind.entries.count { it.isFullFrame })
        assertEquals(5, TextFontStyle.entries.size)
        val wm = WatermarkConfig()
        assertFalse(wm.enabled)
        assertEquals("@Lavelle", wm.text)
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, wm.position)
    }
}
