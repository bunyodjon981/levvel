package com.example.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ClipMediaType
import com.example.model.MontageProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    data class ImportedMediaItem(
        val mediaType: ClipMediaType,
        val imagePath: String?,
        val videoPath: String?,
        val durationMs: Long
    )

    data class ImportedAudioInfo(
        val filePath: String,
        val title: String,
        val durationMs: Long
    )

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").apply { mkdirs() }

    fun getProjectDir(projectId: String): File {
        return File(projectsRoot, projectId).apply { mkdirs() }
    }

    fun createVoiceoverOutputFile(projectId: String): File {
        val audioDir = File(getProjectDir(projectId), "audio").apply { mkdirs() }
        return File(audioDir, "vo_${UUID.randomUUID().toString().take(8)}.m4a")
    }

    suspend fun saveProject(project: MontageProject): MontageProject = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val dir = getProjectDir(updated.id)
        val file = File(dir, "project.json")
        file.writeText(ProjectJsonCodec.toJson(updated))
        updated
    }

    suspend fun loadAllProjects(): List<MontageProject> = withContext(Dispatchers.IO) {
        val dirs = projectsRoot.listFiles()?.filter { it.isDirectory } ?: return@withContext emptyList()
        dirs.mapNotNull { dir ->
            val file = File(dir, "project.json")
            if (!file.exists()) return@mapNotNull null
            try {
                ProjectJsonCodec.fromJson(file.readText())
            } catch (_: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        File(projectsRoot, projectId).deleteRecursively()
    }

    suspend fun importMediaUriToProject(projectId: String, uri: Uri): ImportedMediaItem? = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        val displayName = queryDisplayName(uri).orEmpty()
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val isVideo = mime.startsWith("video/") || ext in setOf("mp4", "mov", "mkv", "webm", "3gp", "m4v")

        if (isVideo) {
            importVideoUriToProject(projectId, uri, ext.ifBlank { "mp4" })
        } else {
            val imgPath = importImageUriToProject(projectId, uri) ?: return@withContext null
            ImportedMediaItem(
                mediaType = ClipMediaType.IMAGE,
                imagePath = imgPath,
                videoPath = null,
                durationMs = 2000L
            )
        }
    }

    suspend fun importImageUriToProject(projectId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val mediaDir = File(getProjectDir(projectId), "media").apply { mkdirs() }
        val targetFile = File(mediaDir, "img_${UUID.randomUUID().toString().take(8)}.jpg")
        try {
            val bitmap = decodeDownsampledBitmap(uri, maxDimension = 1440) ?: return@withContext null
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            targetFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun importVideoUriToProject(projectId: String, uri: Uri, ext: String): ImportedMediaItem? {
        val mediaDir = File(getProjectDir(projectId), "media").apply { mkdirs() }
        val videoFile = File(mediaDir, "vid_${UUID.randomUUID().toString().take(8)}.${ext.take(5)}")
        val thumbFile = File(mediaDir, "thumb_${UUID.randomUUID().toString().take(8)}.jpg")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(videoFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val mmr = MediaMetadataRetriever()
            var durationMs = 3000L
            var poster: Bitmap? = null
            try {
                mmr.setDataSource(videoFile.absolutePath)
                durationMs = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 3000L)
                    .coerceAtLeast(800L)
                poster = mmr.getFrameAtTime(100_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: mmr.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (_: Exception) {
            } finally {
                try {
                    mmr.release()
                } catch (_: Exception) {
                }
            }

            if (poster != null) {
                val maxSide = maxOf(poster.width, poster.height)
                val finalPoster = if (maxSide > 720) {
                    val scale = 720f / maxSide
                    Bitmap.createScaledBitmap(
                        poster,
                        (poster.width * scale).toInt().coerceAtLeast(1),
                        (poster.height * scale).toInt().coerceAtLeast(1),
                        true
                    ).also { if (it !== poster) poster.recycle() }
                } else poster
                FileOutputStream(thumbFile).use { out ->
                    finalPoster.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                finalPoster.recycle()
            }

            ImportedMediaItem(
                mediaType = ClipMediaType.VIDEO,
                imagePath = if (thumbFile.exists()) thumbFile.absolutePath else null,
                videoPath = videoFile.absolutePath,
                durationMs = durationMs
            )
        } catch (_: Exception) {
            videoFile.delete()
            null
        }
    }

    suspend fun importAudioUriToProject(projectId: String, uri: Uri): ImportedAudioInfo? = withContext(Dispatchers.IO) {
        val audioDir = File(getProjectDir(projectId), "audio").apply { mkdirs() }
        val displayName = queryDisplayName(uri) ?: "Musiqa.mp3"
        val ext = displayName.substringAfterLast('.', "mp3").lowercase().take(5)
        val targetFile = File(audioDir, "track_${UUID.randomUUID().toString().take(8)}.$ext")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            val durationMs = readAudioDurationMs(targetFile).coerceAtLeast(5000L)
            val cleanTitle = displayName.substringBeforeLast('.')
            ImportedAudioInfo(
                filePath = targetFile.absolutePath,
                title = cleanTitle.ifBlank { "Musiqa" },
                durationMs = durationMs
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeDownsampledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val maxSide = maxOf(decoded.width, decoded.height)
        if (maxSide <= maxDimension) return decoded
        val scale = maxDimension.toFloat() / maxSide
        val scaledW = (decoded.width * scale).toInt().coerceAtLeast(1)
        val scaledH = (decoded.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readAudioDurationMs(file: File): Long {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(file.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 20_000L
        } catch (_: Exception) {
            20_000L
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {
            }
        }
    }
}
