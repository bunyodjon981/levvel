package com.example.io

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.audio.SynthMusicGenerator
import com.example.model.ClipMediaType
import com.example.model.MontageProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LvlPackageManager(
    private val context: Context,
    private val repository: ProjectRepository
) {

    data class LvlExportResult(
        val displayPath: String,
        val shareUriString: String,
        val fileName: String
    )

    suspend fun exportToDownloads(
        project: MontageProject,
        requestedFileName: String,
        onProgress: (String, Float) -> Unit
    ): LvlExportResult = withContext(Dispatchers.IO) {
        val safeBase = requestedFileName
            .trim()
            .removeSuffix(".lvl")
            .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .trim()
            .ifBlank { "lavelle_video" }
        val finalFileName = "$safeBase.lvl"

        onProgress("Loyiha fayllari tayyorlanmoqda…", 0.15f)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val tempLvlFile = File(exportsDir, finalFileName)

        val clipAssetMap = mutableMapOf<String, String>()
        val clipVideoMap = mutableMapOf<String, String>()
        val clipFilesToWrite = mutableListOf<Pair<String, ByteArray>>()
        val videoFilesToCopy = mutableListOf<Pair<String, File>>()

        project.clips.forEachIndexed { index, clip ->
            val progress = 0.2f + (0.35f * (index + 1) / project.clips.size.coerceAtLeast(1))
            onProgress("Kadr ${index + 1} qadoqlanmoqda…", progress)

            val relPath = "media/clip_$index.jpg"
            val bytes = readClipImageBytes(clip.imagePath, clip.sampleDrawableName)
            if (bytes != null) {
                clipAssetMap[clip.id] = relPath
                clipFilesToWrite.add(relPath to bytes)
            }

            if (clip.mediaType == ClipMediaType.VIDEO && !clip.videoPath.isNullOrBlank()) {
                val vidFile = File(clip.videoPath)
                if (vidFile.exists() && vidFile.length() > 0L) {
                    val ext = vidFile.extension.ifBlank { "mp4" }
                    val vidRel = "media/vid_$index.$ext"
                    clipVideoMap[clip.id] = vidRel
                    videoFilesToCopy.add(vidRel to vidFile)
                }
            }
        }

        var embeddedAudioRelPath: String? = null
        var audioFileToEmbed: File? = null
        if (project.music != null) {
            onProgress("Musiqa va ovozlar qo'shilmoqda…", 0.62f)
            val m = project.music
            val existingFile = m.audioFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
            audioFileToEmbed = existingFile ?: m.builtInPreset?.let { preset ->
                SynthMusicGenerator.ensureBuiltInTrackFile(context, preset, m.bpm, m.durationMs)
            }
            if (audioFileToEmbed != null && audioFileToEmbed.exists()) {
                val ext = audioFileToEmbed.extension.ifBlank { "wav" }
                embeddedAudioRelPath = "audio/soundtrack.$ext"
            }
        }

        val voiceoverMap = mutableMapOf<String, String>()
        val voiceoverFilesToCopy = mutableListOf<Pair<String, File>>()
        project.voiceovers.forEachIndexed { idx, vo ->
            val f = File(vo.filePath)
            if (f.exists() && f.length() > 0L) {
                val ext = f.extension.ifBlank { "m4a" }
                val rel = "audio/vo_$idx.$ext"
                voiceoverMap[vo.id] = rel
                voiceoverFilesToCopy.add(rel to f)
            }
        }

        onProgress(".lvl arxivi yaratilmoqda…", 0.80f)
        val manifestJson = ProjectJsonCodec.toJson(
            project = project,
            clipAssetPaths = clipAssetMap,
            clipVideoPaths = clipVideoMap,
            embeddedAudioRelPath = embeddedAudioRelPath,
            embeddedVoiceoverPaths = voiceoverMap
        )

        ZipOutputStream(BufferedOutputStream(FileOutputStream(tempLvlFile))).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            clipFilesToWrite.forEach { (relPath, data) ->
                zos.putNextEntry(ZipEntry(relPath))
                zos.write(data)
                zos.closeEntry()
            }

            videoFilesToCopy.forEach { (relPath, srcFile) ->
                zos.putNextEntry(ZipEntry(relPath))
                BufferedInputStream(FileInputStream(srcFile)).use { bis ->
                    bis.copyTo(zos)
                }
                zos.closeEntry()
            }

            if (embeddedAudioRelPath != null && audioFileToEmbed != null) {
                zos.putNextEntry(ZipEntry(embeddedAudioRelPath))
                BufferedInputStream(FileInputStream(audioFileToEmbed)).use { bis ->
                    bis.copyTo(zos)
                }
                zos.closeEntry()
            }

            voiceoverFilesToCopy.forEach { (relPath, srcFile) ->
                zos.putNextEntry(ZipEntry(relPath))
                BufferedInputStream(FileInputStream(srcFile)).use { bis ->
                    bis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        onProgress("Download papkasiga saqlanmoqda…", 0.92f)
        val displayPath = writeFileToPublicDownloads(tempLvlFile, finalFileName)
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempLvlFile
        ).toString()

        onProgress("Tayyor!", 1.0f)
        LvlExportResult(
            displayPath = displayPath,
            shareUriString = shareUri,
            fileName = finalFileName
        )
    }

    suspend fun importFromUri(
        uri: Uri,
        onProgress: (fileName: String, stepLabel: String, progress: Float) -> Unit
    ): MontageProject = withContext(Dispatchers.IO) {
        val fileName = queryFileName(uri) ?: "loyiha.lvl"
        onProgress(fileName, "Lavelle fayli ochilmoqda…", 0.15f)
        delay(240)

        val newProjectId = UUID.randomUUID().toString()
        val projectDir = repository.getProjectDir(newProjectId)
        val mediaDir = File(projectDir, "media").apply { mkdirs() }
        val audioDir = File(projectDir, "audio").apply { mkdirs() }

        var manifestJson: String? = null
        val extractedAssets = mutableMapOf<String, String>()
        var entryCount = 0

        onProgress(fileName, "Rasm, video va ovozlar ajratilmoqda…", 0.42f)
        context.contentResolver.openInputStream(uri)?.use { rawStream ->
            ZipInputStream(BufferedInputStream(rawStream)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val safeName = entry.name.replace("..", "")
                        entryCount++
                        when {
                            safeName == "manifest.json" -> {
                                manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            safeName.startsWith("media/") -> {
                                val outFile = File(mediaDir, File(safeName).name)
                                FileOutputStream(outFile).use { zis.copyTo(it) }
                                extractedAssets[safeName] = outFile.absolutePath
                                val pct = (0.45f + (entryCount * 0.05f)).coerceAtMost(0.78f)
                                onProgress(fileName, "Media tiklandi (${extractedAssets.size})…", pct)
                            }
                            safeName.startsWith("audio/") -> {
                                val outFile = File(audioDir, File(safeName).name)
                                FileOutputStream(outFile).use { zis.copyTo(it) }
                                extractedAssets[safeName] = outFile.absolutePath
                                onProgress(fileName, "Musiqa va ovoz yuklandi…", 0.84f)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: error("Faylni o'qib bo'lmadi")

        val json = manifestJson ?: error("Bu fayl yaroqli .lvl loyihasi emas")
        onProgress(fileName, "Montaj oynasi tayyorlanmoqda…", 0.94f)
        delay(260)

        val imported = ProjectJsonCodec.fromJson(
            jsonString = json,
            overrideProjectId = newProjectId,
            resolveEmbeddedAsset = { rel -> extractedAssets[rel] },
            resolveEmbeddedAudio = { rel -> extractedAssets[rel] },
            resolveEmbeddedVideo = { rel -> extractedAssets[rel] },
            resolveEmbeddedVoiceover = { rel -> extractedAssets[rel] }
        )

        val saved = repository.saveProject(imported)
        onProgress(fileName, "Tayyor!", 1.0f)
        delay(180)
        saved
    }

    private fun readClipImageBytes(imagePath: String?, sampleDrawableName: String?): ByteArray? {
        if (!imagePath.isNullOrBlank()) {
            val f = File(imagePath)
            if (f.exists() && f.length() > 0L) {
                return f.readBytes()
            }
        }
        if (!sampleDrawableName.isNullOrBlank()) {
            val resId = context.resources.getIdentifier(sampleDrawableName, "drawable", context.packageName)
            if (resId != 0) {
                val bmp = BitmapFactory.decodeResource(context.resources, resId)
                if (bmp != null) {
                    val out = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    bmp.recycle()
                    return out.toByteArray()
                }
            }
        }
        return null
    }

    private fun writeFileToPublicDownloads(sourceFile: File, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/x-lavelle")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return "Download/$fileName"
            }
        }
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        fallbackDir.mkdirs()
        val dest = File(fallbackDir, fileName)
        sourceFile.copyTo(dest, overwrite = true)
        return dest.absolutePath
    }

    private fun queryFileName(uri: Uri): String? {
        val fromResolver = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
        return fromResolver ?: uri.lastPathSegment?.substringAfterLast('/')
    }
}
