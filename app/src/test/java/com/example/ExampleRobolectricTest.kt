package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.TemplateCatalog
import com.example.io.LvlPackageManager
import com.example.io.ProjectRepository
import com.example.model.MontageProject
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lavelle", appName)
    }

    @Test
    fun `lvl package export and import preserves clips text and audio`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProjectRepository(context)
        val lvlManager = LvlPackageManager(context, repo)

        val tpl = TemplateCatalog.templates.first()
        val project = MontageProject(
            name = "Test Neon Video",
            templateId = tpl.id,
            aspectRatio = tpl.aspectRatio,
            clips = tpl.clips,
            texts = tpl.texts,
            music = tpl.music
        )

        val exported = lvlManager.exportToDownloads(project, "test_video") { _, _ -> }
        assertTrue(exported.fileName == "test_video.lvl")

        val cachedExport = File(context.cacheDir, "exports/test_video.lvl")
        assertTrue(cachedExport.exists())
        assertTrue(cachedExport.length() > 1000L)

        val imported = lvlManager.importFromUri(Uri.fromFile(cachedExport)) { _, _, _ -> }
        assertEquals("Test Neon Video", imported.name)
        assertEquals(tpl.clips.size, imported.clips.size)
        assertEquals(tpl.texts.size, imported.texts.size)
        assertNotNull(imported.music)
        assertTrue(imported.clips.all { clip ->
            val path = clip.imagePath
            !path.isNullOrBlank() && File(path).exists()
        })
        val audioPath = imported.music?.audioFilePath
        assertTrue(!audioPath.isNullOrBlank() && File(audioPath).exists())
    }
}
