package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackController
import com.example.audio.BeatSyncAnalyzer
import com.example.audio.VoiceoverRecorder
import com.example.cloud.AppNotificationHelper
import com.example.cloud.AuthSecurity
import com.example.cloud.FirebaseCloudService
import com.example.cloud.FirebaseConfigManager
import com.example.data.TemplateCatalog
import com.example.io.LvlPackageManager
import com.example.io.ProjectRepository
import com.example.model.AnimationSpec
import com.example.model.AppStatistics
import com.example.model.AspectRatioOption
import com.example.model.ClipItem
import com.example.model.ClipMediaType
import com.example.model.CloudNotificationItem
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.ExportState
import com.example.model.FilterPreset
import com.example.model.FirebaseConfig
import com.example.model.LvlLoadingState
import com.example.model.MontageProject
import com.example.model.MotionEffect
import com.example.model.MusicTrack
import com.example.model.StickerKind
import com.example.model.StickerOverlayItem
import com.example.model.TemplateSpec
import com.example.model.TextAnimationStyle
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.UserProfile
import com.example.model.VoiceoverItem
import com.example.model.WatermarkConfig
import com.example.video.VideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LavelleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val lvlManager = LvlPackageManager(application, repository)
    private val videoExporter = VideoExporter(application)
    private val audioController = AudioPlaybackController(application)
    private val voiceoverRecorder = VoiceoverRecorder(application)
    private val configManager = FirebaseConfigManager(application)
    private val cloudService = FirebaseCloudService(application, configManager)
    private val appPrefs = application.getSharedPreferences(PREFS_APP_STATE, Context.MODE_PRIVATE)

    private val _projects = MutableStateFlow<List<MontageProject>>(emptyList())
    val projects: StateFlow<List<MontageProject>> = _projects.asStateFlow()

    private val _activeProject = MutableStateFlow<MontageProject?>(null)
    val activeProject: StateFlow<MontageProject?> = _activeProject.asStateFlow()

    private val _pendingTemplate = MutableStateFlow<TemplateSpec?>(null)
    val pendingTemplate: StateFlow<TemplateSpec?> = _pendingTemplate.asStateFlow()

    private val _pendingSlotMedia = MutableStateFlow<List<ProjectRepository.ImportedMediaItem?>>(emptyList())
    val pendingSlotMedia: StateFlow<List<ProjectRepository.ImportedMediaItem?>> = _pendingSlotMedia.asStateFlow()

    private val _pendingSlotProjectId = MutableStateFlow(UUID.randomUUID().toString())

    private val _favoriteTemplateIds = MutableStateFlow<Set<String>>(loadFavoriteTemplateIds())
    val favoriteTemplateIds: StateFlow<Set<String>> = _favoriteTemplateIds.asStateFlow()

    private val _previewingTemplate = MutableStateFlow<TemplateSpec?>(null)
    val previewingTemplate: StateFlow<TemplateSpec?> = _previewingTemplate.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedClipIndex = MutableStateFlow<Int?>(null)
    val selectedClipIndex: StateFlow<Int?> = _selectedClipIndex.asStateFlow()

    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    private val _selectedStickerId = MutableStateFlow<String?>(null)
    val selectedStickerId: StateFlow<String?> = _selectedStickerId.asStateFlow()

    private val _isRecordingVoiceover = MutableStateFlow(false)
    val isRecordingVoiceover: StateFlow<Boolean> = _isRecordingVoiceover.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private val _lvlLoadingState = MutableStateFlow<LvlLoadingState>(LvlLoadingState.Idle)
    val lvlLoadingState: StateFlow<LvlLoadingState> = _lvlLoadingState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _navigateToEditorSignal = MutableStateFlow(0)
    val navigateToEditorSignal: StateFlow<Int> = _navigateToEditorSignal.asStateFlow()

    private val _navigateToTemplateFillSignal = MutableStateFlow(0)
    val navigateToTemplateFillSignal: StateFlow<Int> = _navigateToTemplateFillSignal.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Cloud, Auth, Admin & Telemetry State ---

    private val _cloudTemplates = MutableStateFlow<List<TemplateSpec>>(emptyList())
    val cloudTemplates: StateFlow<List<TemplateSpec>> = _cloudTemplates.asStateFlow()

    private val _firebaseConfig = MutableStateFlow(configManager.loadActiveConfig())
    val firebaseConfig: StateFlow<FirebaseConfig> = _firebaseConfig.asStateFlow()

    private val _currentUser = MutableStateFlow<UserProfile?>(loadSavedSession())
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _usersList = MutableStateFlow<List<UserProfile>>(emptyList())
    val usersList: StateFlow<List<UserProfile>> = _usersList.asStateFlow()

    private val _notifications = MutableStateFlow<List<CloudNotificationItem>>(emptyList())
    val notifications: StateFlow<List<CloudNotificationItem>> = _notifications.asStateFlow()

    private val _statistics = MutableStateFlow(
        AppStatistics(
            totalBuiltInTemplates = TemplateCatalog.templates.size
        )
    )
    val statistics: StateFlow<AppStatistics> = _statistics.asStateFlow()

    private val _usageCount = MutableStateFlow(appPrefs.getInt(KEY_USAGE_COUNT, 0))
    val usageCount: StateFlow<Int> = _usageCount.asStateFlow()

    private val _showAuthPromptDialog = MutableStateFlow(false)
    val showAuthPromptDialog: StateFlow<Boolean> = _showAuthPromptDialog.asStateFlow()

    private val _adminUploadProgress = MutableStateFlow<Float?>(null)
    val adminUploadProgress: StateFlow<Float?> = _adminUploadProgress.asStateFlow()

    private val _adminMigrationProgress = MutableStateFlow<Pair<String, Float>?>(null)
    val adminMigrationProgress: StateFlow<Pair<String, Float>?> = _adminMigrationProgress.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private var playbackJob: Job? = null
    private var saveJob: Job? = null
    private var recordingTickerJob: Job? = null
    private var activeVoiceoverStartTimelineMs: Long = 0L

    init {
        refreshProjects()
        syncCloud(silent = true)
    }

    fun refreshProjects() {
        viewModelScope.launch {
            _projects.value = repository.loadAllProjects()
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    // --- Favorites & Template Live Preview ---

    fun toggleFavoriteTemplate(templateId: String) {
        val current = _favoriteTemplateIds.value.toMutableSet()
        if (!current.add(templateId)) {
            current.remove(templateId)
        }
        _favoriteTemplateIds.value = current
        appPrefs.edit().putStringSet(KEY_FAVORITE_TEMPLATES, current).apply()
    }

    fun openTemplatePreview(template: TemplateSpec) {
        _previewingTemplate.value = template
    }

    fun closeTemplatePreview() {
        _previewingTemplate.value = null
    }

    private fun loadFavoriteTemplateIds(): Set<String> {
        return appPrefs.getStringSet(KEY_FAVORITE_TEMPLATES, emptySet())?.toSet() ?: emptySet()
    }

    // --- Cloud Sync, Admin & Firebase ---

    fun syncCloud(silent: Boolean = false) {
        viewModelScope.launch {
            val frameStart = System.nanoTime()
            val res = cloudService.syncAll()
            val elapsedFrame = ((System.nanoTime() - frameStart) / 1_000_000L).coerceAtLeast(4L)

            _firebaseConfig.value = res.activeConfig
            _cloudTemplates.value = res.templates
            _usersList.value = res.users
            _notifications.value = res.notifications

            _statistics.value = AppStatistics(
                totalUsers = res.users.size,
                totalCloudTemplates = res.templates.size,
                totalBuiltInTemplates = TemplateCatalog.templates.size,
                totalMontagesCreated = res.totalMontages,
                totalExports = res.totalExports,
                firebaseConnected = res.connected,
                firebaseLatencyMs = res.latencyMs,
                lastSyncTimeMs = if (res.connected) System.currentTimeMillis() else _statistics.value.lastSyncTimeMs,
                appFrameLatencyMs = elapsedFrame.coerceAtMost(999L),
                notificationCount = res.notifications.size
            )

            if (res.notifications.isNotEmpty()) {
                AppNotificationHelper.showUnseenNotifications(getApplication(), res.notifications)
            }

            if (res.migratedToNewFirebase) {
                _toastMessage.value = "Ilova yangi Firebase serveriga avtomatik ulandi!"
            } else if (!silent && res.errorMessage != null) {
                _toastMessage.value = res.errorMessage
            }
        }
    }

    fun verifyAdminPassword(password: String): Boolean {
        return AuthSecurity.verifyAdminPassword(password)
    }

    fun parseFirebaseJson(rawJson: String): FirebaseConfig {
        return configManager.parseFromJson(rawJson)
    }

    fun updateAndMigrateFirebase(newConfig: FirebaseConfig) {
        if (!newConfig.isConfigured) {
            _toastMessage.value = "Database URL yoki Project ID + API Key ni kiriting"
            return
        }
        val oldConfig = _firebaseConfig.value
        viewModelScope.launch {
            _adminMigrationProgress.value = "Tayyorlanmoqda…" to 0.05f
            val result = cloudService.migrateFirebaseAndNotifyClients(oldConfig, newConfig) { step, pct ->
                _adminMigrationProgress.value = step to pct
            }
            _adminMigrationProgress.value = null
            result.onSuccess {
                _firebaseConfig.value = newConfig
                _toastMessage.value = "Firebase yangilandi va barcha ma'lumotlar ko'chirildi!"
                syncCloud(silent = true)
            }.onFailure { err ->
                _toastMessage.value = "Firebase yangilashda xato: ${err.message}"
            }
        }
    }

    fun uploadLvlTemplateToCloud(uri: Uri, title: String, category: String) {
        val cfg = _firebaseConfig.value
        if (!cfg.isConfigured) {
            _toastMessage.value = "Firebase sozlanmagan! Avval Firebase ma'lumotlarini kiriting."
            return
        }
        if (title.isBlank()) {
            _toastMessage.value = "Shablon nomini kiriting"
            return
        }

        viewModelScope.launch {
            _adminUploadProgress.value = 0.08f
            val bytes = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (_: Exception) {
                    null
                }
            }
            if (bytes == null || bytes.isEmpty()) {
                _adminUploadProgress.value = null
                _toastMessage.value = ".lvl faylini o'qib bo'lmadi"
                return@launch
            }

            _adminUploadProgress.value = 0.20f
            delay(180)

            val result = cloudService.uploadCloudTemplate(title, category, bytes) { pct ->
                _adminUploadProgress.value = pct
            }

            result.onSuccess { spec ->
                delay(220)
                _adminUploadProgress.value = null
                _cloudTemplates.value = listOf(spec) + _cloudTemplates.value.filterNot { it.id == spec.id }
                _toastMessage.value = "\"${spec.title}\" shabloni barcha foydalanuvchilarga qo'shildi!"
                syncCloud(silent = true)
            }.onFailure { err ->
                _adminUploadProgress.value = null
                _toastMessage.value = err.message ?: "Shablon yuklashda xatolik"
            }
        }
    }

    fun deleteCloudTemplate(templateId: String) {
        viewModelScope.launch {
            val result = cloudService.deleteCloudTemplate(templateId)
            result.onSuccess {
                _cloudTemplates.value = _cloudTemplates.value.filterNot { it.id == templateId }
                _toastMessage.value = "Shablon o'chirildi"
                syncCloud(silent = true)
            }.onFailure { err ->
                _toastMessage.value = "O'chirishda xatolik: ${err.message}"
            }
        }
    }

    fun sendAdminNotification(title: String, message: String) {
        if (message.isBlank()) {
            _toastMessage.value = "Xabar matnini kiriting"
            return
        }
        viewModelScope.launch {
            val res = cloudService.sendBroadcastNotification(title, message)
            res.onSuccess { item ->
                _notifications.value = listOf(item) + _notifications.value
                AppNotificationHelper.showUnseenNotifications(getApplication(), listOf(item))
                _toastMessage.value = "Xabar barcha foydalanuvchilarga yuborildi!"
                syncCloud(silent = true)
            }.onFailure { err ->
                _toastMessage.value = err.message ?: "Xabar yuborib bo'lmadi"
            }
        }
    }

    // --- Authentication & Profile ---

    fun clearAuthError() {
        _authError.value = null
    }

    fun registerUser(username: String, email: String, password: String, confirmPassword: String) {
        val validationError = AuthSecurity.validateRegistration(username, email, password, confirmPassword)
        if (validationError != null) {
            _authError.value = validationError
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = cloudService.registerUser(username, email, password)
            _authLoading.value = false
            res.onSuccess { user ->
                _currentUser.value = user
                saveSession(user)
                _showAuthPromptDialog.value = false
                _toastMessage.value = "Xush kelibsiz, ${user.username}!"
                syncCloud(silent = true)
            }.onFailure { err ->
                _authError.value = err.message ?: "Ro'yxatdan o'tishda xatolik"
            }
        }
    }

    fun loginUser(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _authError.value = "Username/email va parolni kiriting"
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = cloudService.loginUser(identifier, password)
            _authLoading.value = false
            res.onSuccess { user ->
                _currentUser.value = user
                saveSession(user)
                _showAuthPromptDialog.value = false
                _toastMessage.value = "Tizimga kirdingiz: ${user.username}"
                syncCloud(silent = true)
            }.onFailure { err ->
                _authError.value = err.message ?: "Kirishda xatolik"
            }
        }
    }

    fun logoutUser() {
        _currentUser.value = null
        appPrefs.edit().remove(KEY_SESSION_JSON).apply()
        _toastMessage.value = "Hisobdan chiqildi"
    }

    fun dismissAuthPromptDialog() {
        _showAuthPromptDialog.value = false
    }

    private fun recordUsageAndCheckGate() {
        val next = _usageCount.value + 1
        _usageCount.value = next
        appPrefs.edit().putInt(KEY_USAGE_COUNT, next).apply()
        if (next >= 4 && _currentUser.value == null) {
            _showAuthPromptDialog.value = true
        }
    }

    private fun saveSession(user: UserProfile) {
        val obj = JSONObject().apply {
            put("id", user.id)
            put("username", user.username)
            put("email", user.email)
            put("passwordHash", user.passwordHash)
            put("passwordSalt", user.passwordSalt)
            put("createdAt", user.createdAt)
            put("montageCount", user.montageCount)
        }
        appPrefs.edit().putString(KEY_SESSION_JSON, obj.toString()).apply()
    }

    private fun loadSavedSession(): UserProfile? {
        val raw = appPrefs.getString(KEY_SESSION_JSON, null) ?: return null
        return try {
            val u = JSONObject(raw)
            UserProfile(
                id = u.optString("id"),
                username = u.optString("username"),
                email = u.optString("email"),
                passwordHash = u.optString("passwordHash"),
                passwordSalt = u.optString("passwordSalt"),
                createdAt = u.optLong("createdAt"),
                montageCount = u.optInt("montageCount")
            )
        } catch (_: Exception) {
            null
        }
    }

    // --- Template & Project Flows ---

    fun startTemplateFlow(template: TemplateSpec) {
        _previewingTemplate.value = null
        _pendingTemplate.value = template
        _pendingSlotProjectId.value = UUID.randomUUID().toString()
        _pendingSlotMedia.value = List(template.slotCount) { null }
        _navigateToTemplateFillSignal.value += 1
    }

    fun fillTemplateSlot(slotIndex: Int, uri: Uri) {
        viewModelScope.launch {
            val media = repository.importMediaUriToProject(_pendingSlotProjectId.value, uri)
            if (media == null) {
                _toastMessage.value = "Faylni o'qishda xatolik"
                return@launch
            }
            val current = _pendingSlotMedia.value.toMutableList()
            if (slotIndex in current.indices) {
                current[slotIndex] = media
                _pendingSlotMedia.value = current
            }
        }
    }

    fun fillTemplateBatch(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val current = _pendingSlotMedia.value.toMutableList()
            if (current.isEmpty()) return@launch
            var uriIdx = 0
            for (slotIdx in current.indices) {
                val chosenUri = uris[uriIdx % uris.size]
                val media = repository.importMediaUriToProject(_pendingSlotProjectId.value, chosenUri)
                if (media != null) {
                    current[slotIdx] = media
                }
                uriIdx++
            }
            _pendingSlotMedia.value = current
        }
    }

    fun launchEditorFromPendingTemplate() {
        val tpl = _pendingTemplate.value ?: return
        val slotMedia = _pendingSlotMedia.value
        val projectId = _pendingSlotProjectId.value
        val defaultWatermark = WatermarkConfig(
            enabled = false,
            text = _currentUser.value?.let { "@${it.username}" } ?: "@Lavelle"
        )

        viewModelScope.launch {
            val project = if (tpl.isCloudTemplate && !tpl.cloudLvlBase64.isNullOrBlank()) {
                withContext(Dispatchers.IO) {
                    cloudService.decodeCloudLvlToMontageProject(tpl, slotMedia)
                }?.copy(watermark = defaultWatermark)
            } else {
                null
            } ?: run {
                val populatedClips = tpl.clips.mapIndexed { index, clip ->
                    val picked = slotMedia.getOrNull(index)
                    if (picked != null) {
                        if (picked.mediaType == ClipMediaType.VIDEO && picked.videoPath != null) {
                            clip.copy(
                                id = UUID.randomUUID().toString(),
                                mediaType = ClipMediaType.VIDEO,
                                imagePath = picked.imagePath,
                                videoPath = picked.videoPath,
                                videoDurationMs = picked.durationMs,
                                trimStartMs = 0L,
                                trimEndMs = picked.durationMs
                            )
                        } else {
                            clip.copy(
                                id = UUID.randomUUID().toString(),
                                mediaType = ClipMediaType.IMAGE,
                                imagePath = picked.imagePath,
                                videoPath = null
                            )
                        }
                    } else {
                        clip.copy(id = UUID.randomUUID().toString())
                    }
                }
                MontageProject(
                    id = projectId,
                    name = tpl.title,
                    templateId = tpl.id,
                    aspectRatio = tpl.aspectRatio,
                    clips = populatedClips,
                    texts = tpl.texts.map { it.copy(id = UUID.randomUUID().toString()) },
                    watermark = defaultWatermark,
                    music = tpl.music.copy(id = UUID.randomUUID().toString())
                )
            }

            recordUsageAndCheckGate()
            cloudService.incrementStat(isExport = false, currentUser = _currentUser.value)
            openProjectInEditor(project)
        }
    }

    fun createBlankProjectFromMedia(uris: List<Uri>) {
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val clips = if (uris.isEmpty()) {
                listOf(
                    ClipItem(
                        slotLabel = "1 • Kadr",
                        sampleDrawableName = "img_tpl_golden_travel",
                        durationMs = 2000L,
                        transition = TransitionType.NONE,
                        motion = MotionEffect.NONE,
                        animationId = "anim_001"
                    )
                )
            } else {
                uris.mapIndexedNotNull { idx, uri ->
                    val imported = repository.importMediaUriToProject(projectId, uri) ?: return@mapIndexedNotNull null
                    if (imported.mediaType == ClipMediaType.VIDEO && imported.videoPath != null) {
                        val defaultClipDur = imported.durationMs.coerceIn(1000L, 12_000L)
                        ClipItem(
                            slotLabel = "${idx + 1} • Video",
                            mediaType = ClipMediaType.VIDEO,
                            imagePath = imported.imagePath,
                            videoPath = imported.videoPath,
                            videoDurationMs = imported.durationMs,
                            trimStartMs = 0L,
                            trimEndMs = imported.durationMs,
                            durationMs = defaultClipDur,
                            transition = TransitionType.NONE,
                            motion = MotionEffect.NONE,
                            animationId = "anim_001",
                            filter = FilterPreset.ORIGINAL
                        )
                    } else {
                        ClipItem(
                            slotLabel = "${idx + 1} • Kadr",
                            mediaType = ClipMediaType.IMAGE,
                            imagePath = imported.imagePath,
                            durationMs = 2000L,
                            transition = TransitionType.NONE,
                            motion = MotionEffect.NONE,
                            animationId = "anim_001",
                            filter = FilterPreset.ORIGINAL
                        )
                    }
                }
            }
            val defaultWatermark = WatermarkConfig(
                enabled = false,
                text = _currentUser.value?.let { "@${it.username}" } ?: "@Lavelle"
            )
            val project = MontageProject(
                id = projectId,
                name = "Yangi Montaj",
                aspectRatio = AspectRatioOption.PORTRAIT_9_16,
                clips = clips,
                texts = emptyList(),
                watermark = defaultWatermark,
                music = null
            )
            recordUsageAndCheckGate()
            cloudService.incrementStat(isExport = false, currentUser = _currentUser.value)
            openProjectInEditor(project)
        }
    }

    fun openSavedProject(project: MontageProject) {
        openProjectInEditor(project)
    }

    fun deleteSavedProject(project: MontageProject) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
            if (_activeProject.value?.id == project.id) {
                pausePlayback()
                _activeProject.value = null
            }
            refreshProjects()
        }
    }

    private fun openProjectInEditor(project: MontageProject) {
        pausePlayback()
        _activeProject.value = project
        _playheadMs.value = 0L
        _selectedClipIndex.value = null
        _selectedTextId.value = null
        _selectedStickerId.value = null
        viewModelScope.launch {
            repository.saveProject(project)
            audioController.prepareIfNeeded(project.music, project.voiceovers)
            refreshProjects()
        }
        _navigateToEditorSignal.value += 1
    }

    fun openLvlFromUri(uri: Uri) {
        pausePlayback()
        viewModelScope.launch {
            try {
                val imported = lvlManager.importFromUri(uri) { fileName, step, progress ->
                    _lvlLoadingState.value = LvlLoadingState.Loading(fileName, step, progress)
                }
                _lvlLoadingState.value = LvlLoadingState.Idle
                openProjectInEditor(imported)
                _toastMessage.value = "${imported.name} muvaffaqiyatli ochildi!"
            } catch (e: Exception) {
                _lvlLoadingState.value = LvlLoadingState.Error(
                    e.message ?: "Lavelle (.lvl) faylini ochishda xatolik yuz berdi"
                )
            }
        }
    }

    fun dismissLvlError() {
        _lvlLoadingState.value = LvlLoadingState.Idle
    }

    // --- Playback Controls ---

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val project = _activeProject.value ?: return
        if (project.clips.isEmpty()) return
        val total = project.totalDurationMs.coerceAtLeast(1000L)
        if (_playheadMs.value >= total - 50L) {
            _playheadMs.value = 0L
        }
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            audioController.prepareIfNeeded(project.music, project.voiceovers)
            audioController.startAt(_playheadMs.value, project.music, project.voiceovers)
            var lastTick = System.currentTimeMillis()
            while (_isPlaying.value) {
                delay(33L)
                val now = System.currentTimeMillis()
                val delta = (now - lastTick).coerceIn(0L, 120L)
                lastTick = now
                val currentProj = _activeProject.value ?: break
                val maxDur = currentProj.totalDurationMs.coerceAtLeast(1000L)
                val nextMs = _playheadMs.value + delta
                if (nextMs >= maxDur) {
                    _playheadMs.value = 0L
                    audioController.seekTo(0L, currentProj.music, currentProj.voiceovers)
                    audioController.startAt(0L, currentProj.music, currentProj.voiceovers)
                } else {
                    _playheadMs.value = nextMs
                    audioController.tickVoiceovers(nextMs, currentProj.voiceovers)
                }
            }
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        audioController.pause()
    }

    fun seekTo(ms: Long) {
        val project = _activeProject.value ?: return
        val clamped = ms.coerceIn(0L, project.totalDurationMs)
        _playheadMs.value = clamped
        audioController.seekTo(clamped, project.music, project.voiceovers)
    }

    // --- Editor Mutations ---

    fun selectClip(index: Int?) {
        _selectedClipIndex.value = index
    }

    fun selectText(id: String?) {
        _selectedTextId.value = id
        if (id != null) _selectedStickerId.value = null
    }

    fun selectSticker(id: String?) {
        _selectedStickerId.value = id
        if (id != null) _selectedTextId.value = null
    }

    fun renameProject(newName: String) {
        updateActiveProject { it.copy(name = newName.trim().ifBlank { "Lavelle Montaj" }) }
    }

    fun setAspectRatio(option: AspectRatioOption) {
        updateActiveProject { it.copy(aspectRatio = option) }
    }

    fun addMediaToProject(uris: List<Uri>) {
        val proj = _activeProject.value ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val newClips = uris.mapIndexedNotNull { idx, uri ->
                val imported = repository.importMediaUriToProject(proj.id, uri) ?: return@mapIndexedNotNull null
                if (imported.mediaType == ClipMediaType.VIDEO && imported.videoPath != null) {
                    ClipItem(
                        slotLabel = "${proj.clips.size + idx + 1} • Video",
                        mediaType = ClipMediaType.VIDEO,
                        imagePath = imported.imagePath,
                        videoPath = imported.videoPath,
                        videoDurationMs = imported.durationMs,
                        trimStartMs = 0L,
                        trimEndMs = imported.durationMs,
                        durationMs = imported.durationMs.coerceIn(1000L, 12_000L),
                        transition = TransitionType.NONE,
                        motion = MotionEffect.NONE,
                        animationId = "anim_001"
                    )
                } else {
                    ClipItem(
                        slotLabel = "${proj.clips.size + idx + 1} • Kadr",
                        mediaType = ClipMediaType.IMAGE,
                        imagePath = imported.imagePath,
                        durationMs = 2000L,
                        transition = TransitionType.NONE,
                        motion = MotionEffect.NONE,
                        animationId = "anim_001"
                    )
                }
            }
            if (newClips.isNotEmpty()) {
                updateActiveProject { it.copy(clips = it.clips + newClips) }
            }
        }
    }

    fun replaceClipMedia(clipIndex: Int, uri: Uri) {
        val proj = _activeProject.value ?: return
        if (clipIndex !in proj.clips.indices) return
        viewModelScope.launch {
            val imported = repository.importMediaUriToProject(proj.id, uri) ?: return@launch
            updateActiveProject { current ->
                val list = current.clips.toMutableList()
                if (clipIndex in list.indices) {
                    val prev = list[clipIndex]
                    list[clipIndex] = if (imported.mediaType == ClipMediaType.VIDEO && imported.videoPath != null) {
                        prev.copy(
                            mediaType = ClipMediaType.VIDEO,
                            imagePath = imported.imagePath,
                            videoPath = imported.videoPath,
                            videoDurationMs = imported.durationMs,
                            trimStartMs = 0L,
                            trimEndMs = imported.durationMs,
                            durationMs = imported.durationMs.coerceIn(1000L, 12_000L)
                        )
                    } else {
                        prev.copy(
                            mediaType = ClipMediaType.IMAGE,
                            imagePath = imported.imagePath,
                            videoPath = null,
                            videoDurationMs = 0L
                        )
                    }
                }
                current.copy(clips = list)
            }
        }
    }

    fun updateClip(clipIndex: Int, transform: (ClipItem) -> ClipItem) {
        updateActiveProject { current ->
            val list = current.clips.toMutableList()
            if (clipIndex in list.indices) {
                list[clipIndex] = transform(list[clipIndex])
            }
            current.copy(clips = list)
        }
    }

    fun applyFilterToAll(filter: FilterPreset) {
        updateActiveProject { current ->
            current.copy(clips = current.clips.map { it.copy(filter = filter) })
        }
    }

    fun applyAnimationToAll(animSpec: AnimationSpec) {
        updateActiveProject { current ->
            current.copy(
                clips = current.clips.map {
                    it.copy(motion = MotionEffect.NONE, animationId = animSpec.id)
                }
            )
        }
    }

    fun moveClip(fromIndex: Int, direction: Int) {
        val toIndex = fromIndex + direction
        updateActiveProject { current ->
            val list = current.clips.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
                _selectedClipIndex.value = toIndex
            }
            current.copy(clips = list)
        }
    }

    fun duplicateClip(index: Int) {
        updateActiveProject { current ->
            val list = current.clips.toMutableList()
            if (index in list.indices) {
                val copy = list[index].copy(id = UUID.randomUUID().toString())
                list.add(index + 1, copy)
                _selectedClipIndex.value = index + 1
            }
            current.copy(clips = list)
        }
    }

    fun deleteClip(index: Int) {
        updateActiveProject { current ->
            if (current.clips.size <= 1) {
                _toastMessage.value = "Kamida 1 ta kadr qolishi kerak"
                return@updateActiveProject current
            }
            val list = current.clips.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
                _selectedClipIndex.value = null
            }
            current.copy(clips = list)
        }
    }

    // --- Text & Interactive Gesture Transform ---

    fun addTextOverlay(
        text: String,
        animation: TextAnimationStyle,
        colorArgb: Long,
        fontStyle: TextFontStyle = TextFontStyle.DISPLAY
    ) {
        val proj = _activeProject.value ?: return
        val start = _playheadMs.value.coerceIn(0L, (proj.totalDurationMs - 500L).coerceAtLeast(0L))
        val end = (start + 2600L).coerceAtMost(proj.totalDurationMs).coerceAtLeast(start + 700L)
        val item = TextOverlayItem(
            text = text.trim(),
            startMs = start,
            endMs = end,
            fontStyle = fontStyle,
            colorArgb = colorArgb,
            animation = animation
        )
        updateActiveProject { it.copy(texts = it.texts + item) }
        _selectedTextId.value = item.id
        _selectedStickerId.value = null
    }

    fun updateTextTransform(
        id: String,
        xFraction: Float,
        yFraction: Float,
        scale: Float,
        rotationDeg: Float
    ) {
        updateActiveProject { current ->
            current.copy(
                texts = current.texts.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = xFraction.coerceIn(0.08f, 0.92f),
                            yFraction = yFraction.coerceIn(0.08f, 0.92f),
                            scale = scale.coerceIn(0.4f, 3.5f),
                            rotationDeg = rotationDeg
                        )
                    } else it
                }
            )
        }
    }

    fun deleteTextOverlay(id: String) {
        if (_selectedTextId.value == id) _selectedTextId.value = null
        updateActiveProject { current ->
            current.copy(texts = current.texts.filterNot { it.id == id })
        }
    }

    // --- Stickers & Frames ---

    fun addStickerOverlay(kind: StickerKind) {
        val proj = _activeProject.value ?: return
        val start = if (kind.isFullFrame) 0L else _playheadMs.value.coerceIn(0L, (proj.totalDurationMs - 500L).coerceAtLeast(0L))
        val end = if (kind.isFullFrame) proj.totalDurationMs else (start + 3000L).coerceAtMost(proj.totalDurationMs).coerceAtLeast(start + 800L)
        val item = StickerOverlayItem(
            kind = kind,
            startMs = start,
            endMs = end,
            xFraction = 0.5f,
            yFraction = 0.5f
        )
        updateActiveProject { it.copy(stickers = it.stickers + item) }
        if (!kind.isFullFrame) {
            _selectedStickerId.value = item.id
            _selectedTextId.value = null
        }
    }

    fun updateStickerTransform(
        id: String,
        xFraction: Float,
        yFraction: Float,
        scale: Float,
        rotationDeg: Float
    ) {
        updateActiveProject { current ->
            current.copy(
                stickers = current.stickers.map {
                    if (it.id == id) {
                        it.copy(
                            xFraction = xFraction.coerceIn(0.08f, 0.92f),
                            yFraction = yFraction.coerceIn(0.08f, 0.92f),
                            scale = scale.coerceIn(0.4f, 3.5f),
                            rotationDeg = rotationDeg
                        )
                    } else it
                }
            )
        }
    }

    fun deleteStickerOverlay(id: String) {
        if (_selectedStickerId.value == id) _selectedStickerId.value = null
        updateActiveProject { current ->
            current.copy(stickers = current.stickers.filterNot { it.id == id })
        }
    }

    // --- Music, Auto Beat Sync & Voiceover ---

    fun selectBuiltInMusic(track: MusicTrack) {
        val copy = track.copy(id = UUID.randomUUID().toString())
        updateActiveProject { it.copy(music = copy) }
        viewModelScope.launch {
            audioController.prepareIfNeeded(copy, _activeProject.value?.voiceovers.orEmpty())
            if (_isPlaying.value) {
                audioController.startAt(_playheadMs.value, copy, _activeProject.value?.voiceovers.orEmpty())
            }
        }
    }

    fun importCustomMusic(uri: Uri) {
        val proj = _activeProject.value ?: return
        viewModelScope.launch {
            val info = repository.importAudioUriToProject(proj.id, uri)
            if (info == null) {
                _toastMessage.value = "Musiqa faylini yuklab bo'lmadi"
                return@launch
            }
            val track = MusicTrack(
                title = info.title,
                artist = "Telefon xotirasidan",
                bpm = 116,
                durationMs = info.durationMs,
                audioFilePath = info.filePath,
                beatMarksMs = BeatSyncAnalyzer.generateBeatGrid(116, info.durationMs)
            )
            updateActiveProject { it.copy(music = track) }
            audioController.prepareIfNeeded(track, _activeProject.value?.voiceovers.orEmpty())
            if (_isPlaying.value) {
                audioController.startAt(_playheadMs.value, track, _activeProject.value?.voiceovers.orEmpty())
            }
            _toastMessage.value = "\"${info.title}\" musiqasi qo'shildi!"
        }
    }

    fun updateMusicSettings(volume: Float, fadeInMs: Long, fadeOutMs: Long, trimStartMs: Long) {
        updateActiveProject { current ->
            val m = current.music ?: return@updateActiveProject current
            val updated = m.copy(
                volume = volume.coerceIn(0f, 1f),
                fadeInMs = fadeInMs,
                fadeOutMs = fadeOutMs,
                trimStartMs = trimStartMs
            )
            audioController.applyVolume(updated.volume)
            current.copy(music = updated)
        }
    }

    fun removeMusic() {
        updateActiveProject { it.copy(music = null) }
        viewModelScope.launch {
            audioController.prepareIfNeeded(null, _activeProject.value?.voiceovers.orEmpty())
        }
    }

    fun autoBeatSyncProject() {
        val proj = _activeProject.value ?: return
        if (proj.clips.isEmpty()) return
        val synced = BeatSyncAnalyzer.applyAutoBeatSync(proj)
        _playheadMs.value = 0L
        _activeProject.value = synced
        viewModelScope.launch {
            repository.saveProject(synced)
            refreshProjects()
        }
        _toastMessage.value = "Kadrlar musiqa ritmiga avtomatik moslashtirildi!"
    }

    fun startVoiceoverRecording() {
        val proj = _activeProject.value ?: return
        if (_isRecordingVoiceover.value) return
        pausePlayback()
        val outFile = repository.createVoiceoverOutputFile(proj.id)
        val started = voiceoverRecorder.startRecording(outFile)
        if (!started) {
            _toastMessage.value = "Mikrofon yozishni boshlab bo'lmadi"
            return
        }
        activeVoiceoverStartTimelineMs = _playheadMs.value.coerceIn(0L, (proj.totalDurationMs - 300L).coerceAtLeast(0L))
        _recordingElapsedMs.value = 0L
        _isRecordingVoiceover.value = true
        recordingTickerJob?.cancel()
        recordingTickerJob = viewModelScope.launch {
            val startWall = System.currentTimeMillis()
            while (_isRecordingVoiceover.value) {
                _recordingElapsedMs.value = System.currentTimeMillis() - startWall
                delay(100L)
            }
        }
    }

    fun stopVoiceoverRecording() {
        if (!_isRecordingVoiceover.value) return
        recordingTickerJob?.cancel()
        _isRecordingVoiceover.value = false
        val result = voiceoverRecorder.stopRecording()
        if (result == null) {
            _toastMessage.value = "Ovoz yozuvi juda qisqa yoki saqlanmadi"
            return
        }
        val (file, durMs) = result
        val proj = _activeProject.value ?: return
        val vo = VoiceoverItem(
            title = "Ovoz #${proj.voiceovers.size + 1}",
            filePath = file.absolutePath,
            startMs = activeVoiceoverStartTimelineMs,
            durationMs = durMs,
            volume = 1.0f
        )
        updateActiveProject { it.copy(voiceovers = it.voiceovers + vo) }
        viewModelScope.launch {
            val updated = _activeProject.value ?: return@launch
            audioController.prepareIfNeeded(updated.music, updated.voiceovers)
        }
        _toastMessage.value = "Ovoz yozildi (${durMs / 1000.0} s)"
    }

    fun deleteVoiceover(id: String) {
        val removed = _activeProject.value?.voiceovers?.firstOrNull { it.id == id }
        updateActiveProject { current ->
            current.copy(voiceovers = current.voiceovers.filterNot { it.id == id })
        }
        removed?.let {
            try {
                File(it.filePath).delete()
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            val updated = _activeProject.value ?: return@launch
            audioController.prepareIfNeeded(updated.music, updated.voiceovers)
        }
    }

    fun updateVoiceoverVolume(id: String, volume: Float) {
        updateActiveProject { current ->
            current.copy(
                voiceovers = current.voiceovers.map {
                    if (it.id == id) it.copy(volume = volume.coerceIn(0f, 1.5f)) else it
                }
            )
        }
        viewModelScope.launch {
            val updated = _activeProject.value ?: return@launch
            audioController.prepareIfNeeded(updated.music, updated.voiceovers)
        }
    }

    fun updateWatermark(config: WatermarkConfig) {
        updateActiveProject { it.copy(watermark = config) }
    }

    // --- Export MP4 & .lvl ---

    fun exportMp4(
        fileName: String,
        resolution: ExportResolution = ExportResolution.HD_720,
        fpsOption: ExportFps = ExportFps.FPS_30
    ) {
        val project = _activeProject.value ?: return
        pausePlayback()
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting("${resolution.label} (${fpsOption.label})", "Tayyorlanmoqda…", 0.02f)
                val result = videoExporter.exportToMp4(project, fileName, resolution, fpsOption) { step, progress ->
                    _exportState.value = ExportState.Exporting("${resolution.label} (${fpsOption.label})", step, progress)
                }
                cloudService.incrementStat(isExport = true, currentUser = _currentUser.value)
                _exportState.value = ExportState.Success(
                    formatLabel = "${resolution.label} MP4 Video",
                    displayPath = result.displayPath,
                    shareUriString = result.shareUriString,
                    mimeType = "video/mp4"
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Failed(
                    e.message ?: "Video eksport qilishda xatolik yuz berdi"
                )
            }
        }
    }

    fun exportLvl(fileName: String) {
        val project = _activeProject.value ?: return
        pausePlayback()
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Exporting("Lavelle (.lvl)", "Tayyorlanmoqda…", 0.05f)
                val result = lvlManager.exportToDownloads(project, fileName) { step, progress ->
                    _exportState.value = ExportState.Exporting("Lavelle (.lvl)", step, progress)
                }
                cloudService.incrementStat(isExport = true, currentUser = _currentUser.value)
                _exportState.value = ExportState.Success(
                    formatLabel = ".lvl Loyiha Fayli",
                    displayPath = result.displayPath,
                    shareUriString = result.shareUriString,
                    mimeType = "application/x-lavelle"
                )
            } catch (e: Exception) {
                _exportState.value = ExportState.Failed(
                    e.message ?: ".lvl faylini eksport qilishda xatolik yuz berdi"
                )
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    private fun updateActiveProject(transform: (MontageProject) -> MontageProject) {
        val current = _activeProject.value ?: return
        val updated = transform(current)
        _activeProject.value = updated
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350L)
            repository.saveProject(updated)
            refreshProjects()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        recordingTickerJob?.cancel()
        voiceoverRecorder.cancelRecording()
        audioController.release()
    }

    companion object {
        private const val PREFS_APP_STATE = "lavelle_user_prefs"
        private const val KEY_USAGE_COUNT = "usage_count"
        private const val KEY_SESSION_JSON = "current_user_session"
        private const val KEY_FAVORITE_TEMPLATES = "favorite_template_ids"
    }
}
