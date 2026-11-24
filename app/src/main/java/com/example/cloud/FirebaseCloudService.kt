package com.example.cloud

import android.content.Context
import android.util.Base64
import com.example.data.TemplateCatalog
import com.example.io.LvlPackageManager
import com.example.io.ProjectJsonCodec
import com.example.model.AspectRatioOption
import com.example.model.CloudNotificationItem
import com.example.model.FirebaseConfig
import com.example.model.MontageProject
import com.example.model.MusicTrack
import com.example.model.TemplateSpec
import com.example.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FirebaseCloudService(
    private val context: Context,
    private val configManager: FirebaseConfigManager
) {
    private val localPrefs = context.getSharedPreferences("lavelle_cloud_cache", Context.MODE_PRIVATE)

    data class CloudSyncResult(
        val activeConfig: FirebaseConfig,
        val migratedToNewFirebase: Boolean,
        val templates: List<TemplateSpec>,
        val users: List<UserProfile>,
        val notifications: List<CloudNotificationItem>,
        val totalExports: Int,
        val totalMontages: Int,
        val latencyMs: Long,
        val connected: Boolean,
        val errorMessage: String? = null
    )

    suspend fun syncAll(): CloudSyncResult = withContext(Dispatchers.IO) {
        var config = configManager.loadActiveConfig()
        if (!config.isConfigured) {
            return@withContext CloudSyncResult(
                activeConfig = config,
                migratedToNewFirebase = false,
                templates = emptyList(),
                users = loadLocalCachedUsers(),
                notifications = emptyList(),
                totalExports = localPrefs.getInt(KEY_LOCAL_EXPORTS, 0),
                totalMontages = localPrefs.getInt(KEY_LOCAL_MONTAGES, 0),
                latencyMs = -1L,
                connected = false
            )
        }

        val startNs = System.nanoTime()
        var migrated = false

        try {
            // 1) Check if previous Firebase published a redirect to a new Firebase config
            val redirectObj = readNode(config, "firebase_redirect")
            if (redirectObj != null) {
                val targetJson = redirectObj.optString("targetConfigJson", "")
                if (targetJson.isNotBlank()) {
                    val redirected = configManager.parseFromJson(targetJson)
                    if (redirected.isConfigured && redirected != config) {
                        configManager.saveActiveConfig(redirected)
                        config = redirected
                        migrated = true
                    }
                }
            }

            // 2) Fetch templates, users, notifications, stats
            val templatesObj = readNode(config, "templates")
            val usersObj = readNode(config, "users")
            val notifObj = readNode(config, "notifications")
            val statsObj = readNode(config, "stats")

            val templates = parseCloudTemplates(templatesObj)
            val users = parseUsers(usersObj)
            val notifications = parseNotifications(notifObj)
            val exports = statsObj?.optInt("totalExports", 0) ?: localPrefs.getInt(KEY_LOCAL_EXPORTS, 0)
            val montages = statsObj?.optInt("totalMontages", 0) ?: localPrefs.getInt(KEY_LOCAL_MONTAGES, 0)

            val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

            CloudSyncResult(
                activeConfig = config,
                migratedToNewFirebase = migrated,
                templates = templates,
                users = users,
                notifications = notifications,
                totalExports = exports,
                totalMontages = montages,
                latencyMs = latencyMs,
                connected = true
            )
        } catch (e: Exception) {
            val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            CloudSyncResult(
                activeConfig = config,
                migratedToNewFirebase = migrated,
                templates = emptyList(),
                users = loadLocalCachedUsers(),
                notifications = emptyList(),
                totalExports = localPrefs.getInt(KEY_LOCAL_EXPORTS, 0),
                totalMontages = localPrefs.getInt(KEY_LOCAL_MONTAGES, 0),
                latencyMs = latencyMs,
                connected = false,
                errorMessage = e.message ?: "Firebase bilan ulanishda xatolik"
            )
        }
    }

    suspend fun uploadCloudTemplate(
        title: String,
        category: String,
        lvlBytes: ByteArray,
        onProgress: (Float) -> Unit
    ): Result<TemplateSpec> = withContext(Dispatchers.IO) {
        val config = configManager.loadActiveConfig()
        if (!config.isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Firebase sozlamalari kiritilmagan! Avval Admin panelda Firebase ma'lumotlarini kiriting.")
            )
        }

        try {
            onProgress(0.25f)
            val id = "cloud_" + UUID.randomUUID().toString().take(8)
            val parsedInfo = parseLvlMetadata(id, title.trim(), lvlBytes)
            onProgress(0.55f)
            val lvlBase64 = Base64.encodeToString(lvlBytes, Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("id", id)
                put("title", title.trim())
                put("subtitle", "${parsedInfo.slotCount} kadr • Admin shabloni (.lvl)")
                put("category", category.ifBlank { "Reels" })
                put("aspectRatio", parsedInfo.aspectRatio.name)
                put("bpm", parsedInfo.bpm)
                put("slotCount", parsedInfo.slotCount)
                put("durationMs", parsedInfo.totalDurationMs)
                put("lvlBase64", lvlBase64)
                put("createdAt", System.currentTimeMillis())
            }

            onProgress(0.80f)
            writeChild(config, "templates", id, payload)
            onProgress(1.0f)

            val spec = buildTemplateSpecFromCloud(
                id = id,
                title = title.trim(),
                subtitle = payload.optString("subtitle"),
                category = payload.optString("category"),
                aspectRatio = parsedInfo.aspectRatio,
                bpm = parsedInfo.bpm,
                lvlBase64 = lvlBase64
            )
            Result.success(spec)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudTemplate(templateId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val config = configManager.loadActiveConfig()
        if (!config.isConfigured) {
            return@withContext Result.failure(IllegalStateException("Firebase ulanmagan"))
        }
        try {
            deleteChild(config, "templates", templateId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun migrateFirebaseAndNotifyClients(
        oldConfig: FirebaseConfig,
        newConfig: FirebaseConfig,
        onStep: (String, Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!newConfig.isConfigured) {
            return@withContext Result.failure(IllegalArgumentException("Yangi Firebase ma'lumotlari to'liq emas"))
        }
        try {
            onStep("Yangi Firebase tekshirilmoqda…", 0.15f)
            // Verify write permission on new Firebase first
            val pingObj = JSONObject().apply {
                put("updatedAt", System.currentTimeMillis())
                put("app", "Lavelle")
            }
            writeNode(newConfig, "app_meta", pingObj)

            if (oldConfig.isConfigured && oldConfig != newConfig) {
                onStep("Eski ma'lumotlar o'qilmoqda…", 0.35f)
                val oldTemplates = readNode(oldConfig, "templates")
                val oldUsers = readNode(oldConfig, "users")
                val oldNotifications = readNode(oldConfig, "notifications")
                val oldStats = readNode(oldConfig, "stats")

                onStep("Yangi Firebase ga ko'chirilmoqda…", 0.60f)
                oldTemplates?.let { writeNode(newConfig, "templates", it) }
                oldUsers?.let { writeNode(newConfig, "users", it) }
                oldNotifications?.let { writeNode(newConfig, "notifications", it) }
                oldStats?.let { writeNode(newConfig, "stats", it) }

                onStep("Barcha qurilmalarga yo'naltirish yuborilmoqda…", 0.85f)
                val redirectPayload = JSONObject().apply {
                    put("targetConfigJson", configManager.toJson(newConfig))
                    put("migratedAt", System.currentTimeMillis())
                }
                writeNode(oldConfig, "firebase_redirect", redirectPayload)
            }

            onStep("Sozlamalar saqlanmoqda…", 0.95f)
            configManager.saveActiveConfig(newConfig)
            onStep("Tayyor!", 1.0f)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(
        username: String,
        email: String,
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanEmail = email.trim().lowercase()
        val salt = AuthSecurity.generateSaltHex()
        val hash = AuthSecurity.hashPassword(password, salt)
        val userId = "u_" + UUID.randomUUID().toString().take(10)

        val profile = UserProfile(
            id = userId,
            username = cleanUser,
            email = cleanEmail,
            passwordHash = hash,
            passwordSalt = salt,
            createdAt = System.currentTimeMillis(),
            montageCount = 0
        )

        val config = configManager.loadActiveConfig()
        if (config.isConfigured) {
            try {
                val existingUsers = parseUsers(readNode(config, "users"))
                if (existingUsers.any { it.username.equals(cleanUser, ignoreCase = true) }) {
                    return@withContext Result.failure(IllegalArgumentException("Bu username allaqachon band"))
                }
                if (existingUsers.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
                    return@withContext Result.failure(IllegalArgumentException("Bu email allaqachon ro'yxatdan o'tgan"))
                }

                writeChild(config, "users", userId, userToJson(profile))
                saveUserLocally(profile)
                return@withContext Result.success(profile)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Firebase ga saqlab bo'lmadi: ${e.message}"))
            }
        } else {
            val localUsers = loadLocalCachedUsers()
            if (localUsers.any { it.username.equals(cleanUser, ignoreCase = true) }) {
                return@withContext Result.failure(IllegalArgumentException("Bu username allaqachon band"))
            }
            if (localUsers.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
                return@withContext Result.failure(IllegalArgumentException("Bu email allaqachon ro'yxatdan o'tgan"))
            }
            saveUserLocally(profile)
            return@withContext Result.success(profile)
        }
    }

    suspend fun loginUser(identifier: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        val config = configManager.loadActiveConfig()
        val candidates = if (config.isConfigured) {
            try {
                parseUsers(readNode(config, "users")) + loadLocalCachedUsers()
            } catch (_: Exception) {
                loadLocalCachedUsers()
            }
        } else {
            loadLocalCachedUsers()
        }

        val match = candidates.firstOrNull {
            it.username.equals(cleanId, ignoreCase = true) || it.email.equals(cleanId, ignoreCase = true)
        } ?: return@withContext Result.failure(IllegalArgumentException("Foydalanuvchi topilmadi"))

        if (!AuthSecurity.verifyPassword(password, match.passwordSalt, match.passwordHash)) {
            return@withContext Result.failure(IllegalArgumentException("Parol noto'g'ri"))
        }

        saveUserLocally(match)
        Result.success(match)
    }

    suspend fun sendBroadcastNotification(title: String, message: String): Result<CloudNotificationItem> =
        withContext(Dispatchers.IO) {
            val item = CloudNotificationItem(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                title = title.trim().ifBlank { "Lavelle Xabar" },
                message = message.trim(),
                sentAt = System.currentTimeMillis()
            )
            val config = configManager.loadActiveConfig()
            if (!config.isConfigured) {
                return@withContext Result.failure(
                    IllegalStateException("Firebase sozlanmagan! Xabar boshqa qurilmalarga borishi uchun Firebase ulang.")
                )
            }
            try {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("sentAt", item.sentAt)
                }
                writeChild(config, "notifications", item.id, obj)
                Result.success(item)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun incrementStat(isExport: Boolean, currentUser: UserProfile?) = withContext(Dispatchers.IO) {
        val key = if (isExport) KEY_LOCAL_EXPORTS else KEY_LOCAL_MONTAGES
        val newLocal = localPrefs.getInt(key, 0) + 1
        localPrefs.edit().putInt(key, newLocal).apply()

        val config = configManager.loadActiveConfig()
        if (!config.isConfigured) return@withContext
        try {
            val stats = readNode(config, "stats") ?: JSONObject()
            val curExp = stats.optInt("totalExports", 0)
            val curMont = stats.optInt("totalMontages", 0)
            stats.put("totalExports", if (isExport) curExp + 1 else curExp)
            stats.put("totalMontages", if (!isExport) curMont + 1 else curMont)
            writeNode(config, "stats", stats)

            if (!isExport && currentUser != null) {
                val updated = currentUser.copy(montageCount = currentUser.montageCount + 1)
                writeChild(config, "users", updated.id, userToJson(updated))
                saveUserLocally(updated)
            }
        } catch (_: Exception) {
            // Silent best-effort telemetry update
        }
    }

    fun decodeCloudLvlToMontageProject(
        template: TemplateSpec,
        slotMedia: List<com.example.io.ProjectRepository.ImportedMediaItem?>
    ): MontageProject? {
        val b64 = template.cloudLvlBase64 ?: return null
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val newProjectId = UUID.randomUUID().toString()
            val projectDir = File(context.filesDir, "projects/$newProjectId")
            val mediaDir = File(projectDir, "media").apply { mkdirs() }
            val audioDir = File(projectDir, "audio").apply { mkdirs() }

            var manifestJson: String? = null
            val extractedAssets = mutableMapOf<String, String>()

            ZipInputStream(BufferedInputStream(ByteArrayInputStream(bytes))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val safeName = entry.name.replace("..", "")
                        when {
                            safeName == "manifest.json" -> manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                            safeName.startsWith("media/") -> {
                                val outFile = File(mediaDir, File(safeName).name)
                                FileOutputStream(outFile).use { zis.copyTo(it) }
                                extractedAssets[safeName] = outFile.absolutePath
                            }
                            safeName.startsWith("audio/") -> {
                                val outFile = File(audioDir, File(safeName).name)
                                FileOutputStream(outFile).use { zis.copyTo(it) }
                                extractedAssets[safeName] = outFile.absolutePath
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val json = manifestJson ?: return null
            val parsed = ProjectJsonCodec.fromJson(
                jsonString = json,
                overrideProjectId = newProjectId,
                resolveEmbeddedAsset = { extractedAssets[it] },
                resolveEmbeddedAudio = { extractedAssets[it] },
                resolveEmbeddedVideo = { extractedAssets[it] },
                resolveEmbeddedVoiceover = { extractedAssets[it] }
            )
            val updatedClips = parsed.clips.mapIndexed { idx, clip ->
                val userPicked = slotMedia.getOrNull(idx)
                if (userPicked != null) {
                    if (userPicked.mediaType == com.example.model.ClipMediaType.VIDEO && userPicked.videoPath != null) {
                        clip.copy(
                            id = UUID.randomUUID().toString(),
                            mediaType = com.example.model.ClipMediaType.VIDEO,
                            imagePath = userPicked.imagePath,
                            videoPath = userPicked.videoPath,
                            videoDurationMs = userPicked.durationMs,
                            trimStartMs = 0L,
                            trimEndMs = userPicked.durationMs
                        )
                    } else {
                        clip.copy(
                            id = UUID.randomUUID().toString(),
                            mediaType = com.example.model.ClipMediaType.IMAGE,
                            imagePath = userPicked.imagePath,
                            videoPath = null
                        )
                    }
                } else {
                    clip.copy(id = UUID.randomUUID().toString())
                }
            }
            parsed.copy(
                name = template.title,
                templateId = template.id,
                clips = updatedClips
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLvlMetadata(templateId: String, title: String, lvlBytes: ByteArray): TemplateSpec {
        var manifestJson: String? = null
        val previewDir = File(context.cacheDir, "cloud_tpl_previews/$templateId").apply { mkdirs() }
        var firstCoverPath: String? = null
        var firstVideoPath: String? = null

        ZipInputStream(BufferedInputStream(ByteArrayInputStream(lvlBytes))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val safeName = entry.name.replace("..", "")
                    if (safeName == "manifest.json") {
                        manifestJson = zis.readBytes().toString(Charsets.UTF_8)
                    } else if (safeName.startsWith("media/")) {
                        val lower = safeName.lowercase()
                        if ((lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv")) && firstVideoPath == null) {
                            val outFile = File(previewDir, "preview_video.mp4")
                            FileOutputStream(outFile).use { zis.copyTo(it) }
                            firstVideoPath = outFile.absolutePath
                        } else if (firstCoverPath == null) {
                            val outFile = File(previewDir, "cover.jpg")
                            FileOutputStream(outFile).use { zis.copyTo(it) }
                            firstCoverPath = outFile.absolutePath
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        val json = manifestJson ?: error("Tanlangan fayl yaroqli .lvl shabloni emas")
        val proj = ProjectJsonCodec.fromJson(json)
        return TemplateSpec(
            id = templateId,
            title = title,
            subtitle = "${proj.clips.size} kadr • Admin shabloni",
            category = "Reels",
            coverDrawableName = "img_tpl_neon_beat",
            coverImagePath = firstCoverPath,
            previewVideoPath = firstVideoPath,
            isCloudTemplate = true,
            accentColorHex = 0xFF00E5FFL,
            aspectRatio = proj.aspectRatio,
            bpm = proj.music?.bpm ?: 120,
            clips = proj.clips,
            texts = proj.texts,
            music = proj.music ?: TemplateCatalog.builtInMusicLibrary.first()
        )
    }

    private fun parseCloudTemplates(rootObj: JSONObject?): List<TemplateSpec> {
        if (rootObj == null) return emptyList()
        val list = mutableListOf<TemplateSpec>()
        val keys = rootObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = rootObj.optJSONObject(key) ?: continue
            val id = obj.optString("id", key)
            val title = obj.optString("title", "Yangi Shablon")
            val subtitle = obj.optString("subtitle", "Admin shabloni")
            val category = obj.optString("category", "Reels")
            val ratio = try {
                AspectRatioOption.valueOf(obj.optString("aspectRatio", "PORTRAIT_9_16"))
            } catch (_: Exception) {
                AspectRatioOption.PORTRAIT_9_16
            }
            val bpm = obj.optInt("bpm", 120)
            val b64 = obj.optString("lvlBase64", "").ifBlank { null }
            list.add(buildTemplateSpecFromCloud(id, title, subtitle, category, ratio, bpm, b64))
        }
        return list
    }

    private fun buildTemplateSpecFromCloud(
        id: String,
        title: String,
        subtitle: String,
        category: String,
        aspectRatio: AspectRatioOption,
        bpm: Int,
        lvlBase64: String?
    ): TemplateSpec {
        var coverPath: String? = null
        var previewVideoPath: String? = null
        var parsedProj: MontageProject? = null
        if (!lvlBase64.isNullOrBlank()) {
            val previewDir = File(context.cacheDir, "cloud_tpl_previews/$id").apply { mkdirs() }
            val coverFile = File(previewDir, "cover.jpg")
            val videoFile = File(previewDir, "preview_video.mp4")
            val extractedMedia = mutableMapOf<String, String>()
            try {
                val bytes = Base64.decode(lvlBase64, Base64.DEFAULT)
                var rawJson: String? = null
                ZipInputStream(BufferedInputStream(ByteArrayInputStream(bytes))).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val safe = entry.name.replace("..", "")
                            if (safe == "manifest.json") {
                                rawJson = zis.readBytes().toString(Charsets.UTF_8)
                            } else if (safe.startsWith("media/")) {
                                val lower = safe.lowercase()
                                if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".mkv")) {
                                    val target = if (!videoFile.exists()) videoFile else File(previewDir, File(safe).name)
                                    if (!target.exists()) {
                                        FileOutputStream(target).use { zis.copyTo(it) }
                                    }
                                    extractedMedia[safe] = target.absolutePath
                                } else {
                                    val target = if (!coverFile.exists()) coverFile else File(previewDir, File(safe).name)
                                    if (!target.exists()) {
                                        FileOutputStream(target).use { zis.copyTo(it) }
                                    }
                                    extractedMedia[safe] = target.absolutePath
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                if (rawJson != null) {
                    parsedProj = ProjectJsonCodec.fromJson(
                        jsonString = rawJson!!,
                        resolveEmbeddedAsset = { extractedMedia[it] },
                        resolveEmbeddedVideo = { extractedMedia[it] }
                    )
                }
                if (coverFile.exists()) coverPath = coverFile.absolutePath
                if (videoFile.exists()) previewVideoPath = videoFile.absolutePath
            } catch (_: Exception) {
                // ignore malformed base64 preview
            }
        }
        val clips = parsedProj?.clips?.takeIf { it.isNotEmpty() } ?: TemplateCatalog.templates.first().clips
        val music = parsedProj?.music ?: MusicTrack(
            title = title,
            artist = "Lavelle Admin",
            bpm = bpm,
            durationMs = clips.sumOf { it.durationMs }
        )
        return TemplateSpec(
            id = id,
            title = title,
            subtitle = subtitle,
            category = category,
            coverDrawableName = "img_tpl_neon_beat",
            coverImagePath = coverPath,
            previewVideoPath = previewVideoPath,
            isCloudTemplate = true,
            cloudLvlBase64 = lvlBase64,
            accentColorHex = 0xFFFF4D8DL,
            aspectRatio = aspectRatio,
            bpm = bpm,
            clips = clips,
            texts = parsedProj?.texts ?: emptyList(),
            music = music
        )
    }

    private fun parseUsers(obj: JSONObject?): List<UserProfile> {
        if (obj == null) return emptyList()
        val list = mutableListOf<UserProfile>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val u = obj.optJSONObject(key) ?: continue
            list.add(
                UserProfile(
                    id = u.optString("id", key),
                    username = u.optString("username", ""),
                    email = u.optString("email", ""),
                    passwordHash = u.optString("passwordHash", ""),
                    passwordSalt = u.optString("passwordSalt", ""),
                    createdAt = u.optLong("createdAt", 0L),
                    montageCount = u.optInt("montageCount", 0)
                )
            )
        }
        return list.sortedBy { it.createdAt }
    }

    private fun parseNotifications(obj: JSONObject?): List<CloudNotificationItem> {
        if (obj == null) return emptyList()
        val list = mutableListOf<CloudNotificationItem>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val n = obj.optJSONObject(key) ?: continue
            list.add(
                CloudNotificationItem(
                    id = n.optString("id", key),
                    title = n.optString("title", "Lavelle Xabar"),
                    message = n.optString("message", ""),
                    sentAt = n.optLong("sentAt", 0L)
                )
            )
        }
        return list.sortedByDescending { it.sentAt }
    }

    private fun userToJson(user: UserProfile): JSONObject {
        return JSONObject().apply {
            put("id", user.id)
            put("username", user.username)
            put("email", user.email)
            put("passwordHash", user.passwordHash)
            put("passwordSalt", user.passwordSalt)
            put("createdAt", user.createdAt)
            put("montageCount", user.montageCount)
        }
    }

    private fun saveUserLocally(user: UserProfile) {
        val existing = loadLocalCachedUsers().filterNot { it.id == user.id } + user
        val arr = JSONArray()
        existing.forEach { arr.put(userToJson(it)) }
        localPrefs.edit().putString(KEY_LOCAL_USERS, arr.toString()).apply()
    }

    fun loadLocalCachedUsers(): List<UserProfile> {
        val raw = localPrefs.getString(KEY_LOCAL_USERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val u = arr.getJSONObject(i)
                    add(
                        UserProfile(
                            id = u.optString("id"),
                            username = u.optString("username"),
                            email = u.optString("email"),
                            passwordHash = u.optString("passwordHash"),
                            passwordSalt = u.optString("passwordSalt"),
                            createdAt = u.optLong("createdAt"),
                            montageCount = u.optInt("montageCount")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // --- REST Transport Layer (RTDB + Firestore REST fallback) ---

    private fun isRtdb(config: FirebaseConfig): Boolean = config.databaseUrl.isNotBlank()

    private fun readNode(config: FirebaseConfig, path: String): JSONObject? {
        return if (isRtdb(config)) {
            val authSuffix = if (config.apiKey.isNotBlank()) "?auth=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "${config.databaseUrl.trimEnd('/')}/lavelle/$path.json$authSuffix"
            val body = httpRequest("GET", url, null)
            if (body.isBlank() || body == "null") null else JSONObject(body)
        } else {
            val keyParam = if (config.apiKey.isNotBlank()) "?key=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/lavelle_data/$path$keyParam"
            val body = try {
                httpRequest("GET", url, null)
            } catch (_: Exception) {
                return null
            }
            if (body.isBlank()) return null
            val doc = JSONObject(body)
            val fields = doc.optJSONObject("fields") ?: return null
            val rawJson = fields.optJSONObject("json")?.optString("stringValue", "") ?: return null
            if (rawJson.isBlank()) null else JSONObject(rawJson)
        }
    }

    private fun writeNode(config: FirebaseConfig, path: String, data: JSONObject) {
        if (isRtdb(config)) {
            val authSuffix = if (config.apiKey.isNotBlank()) "?auth=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "${config.databaseUrl.trimEnd('/')}/lavelle/$path.json$authSuffix"
            httpRequest("PUT", url, data.toString())
        } else {
            val keyParam = if (config.apiKey.isNotBlank()) "?key=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "https://firestore.googleapis.com/v1/projects/${config.projectId}/databases/(default)/documents/lavelle_data/$path$keyParam"
            val firestorePayload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("json", JSONObject().apply {
                        put("stringValue", data.toString())
                    })
                })
            }
            httpRequest("PATCH", url, firestorePayload.toString())
        }
    }

    private fun writeChild(config: FirebaseConfig, path: String, childId: String, childData: JSONObject) {
        if (isRtdb(config)) {
            val authSuffix = if (config.apiKey.isNotBlank()) "?auth=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "${config.databaseUrl.trimEnd('/')}/lavelle/$path/$childId.json$authSuffix"
            httpRequest("PUT", url, childData.toString())
        } else {
            val existing = readNode(config, path) ?: JSONObject()
            existing.put(childId, childData)
            writeNode(config, path, existing)
        }
    }

    private fun deleteChild(config: FirebaseConfig, path: String, childId: String) {
        if (isRtdb(config)) {
            val authSuffix = if (config.apiKey.isNotBlank()) "?auth=${URLEncoder.encode(config.apiKey, "UTF-8")}" else ""
            val url = "${config.databaseUrl.trimEnd('/')}/lavelle/$path/$childId.json$authSuffix"
            httpRequest("DELETE", url, null)
        } else {
            val existing = readNode(config, path) ?: return
            existing.remove(childId)
            writeNode(config, path, existing)
        }
    }

    private fun httpRequest(method: String, urlStr: String, jsonBody: String?): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = if (method == "PATCH") "POST" else method
            if (method == "PATCH") {
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
            }
            connectTimeout = 9000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
        }
        try {
            if (jsonBody != null) {
                conn.outputStream.use { os ->
                    os.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code: ${text.take(180)}")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val KEY_LOCAL_USERS = "local_users_json"
        private const val KEY_LOCAL_EXPORTS = "local_exports_count"
        private const val KEY_LOCAL_MONTAGES = "local_montages_count"
    }
}
