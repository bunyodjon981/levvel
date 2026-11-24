package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.model.AppStatistics
import com.example.model.CloudNotificationItem
import com.example.model.MontageProject
import com.example.model.TemplateSpec
import com.example.model.UserProfile
import com.example.ui.components.LvlLoadingOverlay
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TemplateFillScreen
import com.example.ui.screens.TemplateLivePreviewDialog
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.LavelleTheme
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LavelleViewModel
import kotlinx.coroutines.launch

private const val ROUTE_HOME = "home"
private const val ROUTE_TEMPLATE_FILL = "template_fill"
private const val ROUTE_EDITOR = "editor"
private const val ROUTE_ADMIN = "admin"

private enum class PickMode {
    BLANK_PROJECT,
    TEMPLATE_BATCH,
    ADD_TO_EDITOR,
    REPLACE_CLIP,
    TEMPLATE_SINGLE_SLOT
}

class MainActivity : ComponentActivity() {

    private val viewModel: LavelleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        handleIncomingIntent(intent)

        setContent {
            LavelleTheme {
                LavelleRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            }
            else -> null
        }
        if (uri != null) {
            viewModel.openLvlFromUri(uri)
        }
    }
}

@Composable
private fun LavelleRootApp(viewModel: LavelleViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val pendingTemplate by viewModel.pendingTemplate.collectAsStateWithLifecycle()
    val pendingSlotMedia by viewModel.pendingSlotMedia.collectAsStateWithLifecycle()
    val favoriteTemplateIds by viewModel.favoriteTemplateIds.collectAsStateWithLifecycle()
    val previewingTemplate by viewModel.previewingTemplate.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedClipIndex by viewModel.selectedClipIndex.collectAsStateWithLifecycle()
    val selectedTextId by viewModel.selectedTextId.collectAsStateWithLifecycle()
    val selectedStickerId by viewModel.selectedStickerId.collectAsStateWithLifecycle()
    val isRecordingVoiceover by viewModel.isRecordingVoiceover.collectAsStateWithLifecycle()
    val recordingElapsedMs by viewModel.recordingElapsedMs.collectAsStateWithLifecycle()
    val lvlLoadingState by viewModel.lvlLoadingState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val navEditorSignal by viewModel.navigateToEditorSignal.collectAsStateWithLifecycle()
    val navFillSignal by viewModel.navigateToTemplateFillSignal.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val cloudTemplates by viewModel.cloudTemplates.collectAsStateWithLifecycle()
    val firebaseConfig by viewModel.firebaseConfig.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val usersList by viewModel.usersList.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val usageCount by viewModel.usageCount.collectAsStateWithLifecycle()
    val showAuthPromptDialog by viewModel.showAuthPromptDialog.collectAsStateWithLifecycle()
    val adminUploadProgress by viewModel.adminUploadProgress.collectAsStateWithLifecycle()
    val adminMigrationProgress by viewModel.adminMigrationProgress.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()

    var selectedHomeTab by remember { mutableIntStateOf(0) }
    var pendingPickMode by remember { mutableStateOf(PickMode.BLANK_PROJECT) }
    var pendingTargetSlotIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceoverRecording()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Ovoz yozish uchun mikrofon ruxsati kerak")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        viewModel.syncCloud(silent = true)
        onPauseOrDispose { }
    }

    val multiPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(12)
    ) { uris ->
        if (uris.isNotEmpty()) {
            when (pendingPickMode) {
                PickMode.BLANK_PROJECT -> viewModel.createBlankProjectFromMedia(uris)
                PickMode.TEMPLATE_BATCH -> viewModel.fillTemplateBatch(uris)
                PickMode.ADD_TO_EDITOR -> viewModel.addMediaToProject(uris)
                else -> Unit
            }
        }
    }

    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            when (pendingPickMode) {
                PickMode.TEMPLATE_SINGLE_SLOT -> viewModel.fillTemplateSlot(pendingTargetSlotIndex, uri)
                PickMode.REPLACE_CLIP -> viewModel.replaceClipMedia(pendingTargetSlotIndex, uri)
                else -> Unit
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomMusic(uri)
        }
    }

    val lvlFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.openLvlFromUri(uri)
        }
    }

    LaunchedEffect(navFillSignal) {
        if (navFillSignal > 0 && pendingTemplate != null) {
            navController.navigate(ROUTE_TEMPLATE_FILL) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(navEditorSignal) {
        if (navEditorSignal > 0 && activeProject != null) {
            navController.navigate(ROUTE_EDITOR) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeToast()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        Scaffold(
            containerColor = StudioObsidian,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(ROUTE_HOME) {
                    HomeHubScreen(
                        selectedTab = selectedHomeTab,
                        onSelectTab = { selectedHomeTab = it },
                        projects = projects,
                        cloudTemplates = cloudTemplates,
                        favoriteTemplateIds = favoriteTemplateIds,
                        currentUser = currentUser,
                        usageCount = usageCount,
                        statistics = statistics,
                        notifications = notifications,
                        authLoading = authLoading,
                        authError = authError,
                        onClearAuthError = { viewModel.clearAuthError() },
                        onRegister = { u, e, p, c -> viewModel.registerUser(u, e, p, c) },
                        onLogin = { id, p -> viewModel.loginUser(id, p) },
                        onLogout = { viewModel.logoutUser() },
                        onToggleFavorite = { id -> viewModel.toggleFavoriteTemplate(id) },
                        onPreviewTemplate = { tpl -> viewModel.openTemplatePreview(tpl) },
                        onSelectTemplate = { tpl -> viewModel.startTemplateFlow(tpl) },
                        onNewBlankMontage = {
                            pendingPickMode = PickMode.BLANK_PROJECT
                            multiPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        onOpenLvlFile = {
                            lvlFilePickerLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenSavedProject = { proj -> viewModel.openSavedProject(proj) },
                        onDeleteSavedProject = { proj -> viewModel.deleteSavedProject(proj) },
                        onVerifyAdminPassword = { pass -> viewModel.verifyAdminPassword(pass) },
                        onNavigateAdmin = {
                            viewModel.syncCloud(silent = true)
                            navController.navigate(ROUTE_ADMIN) { launchSingleTop = true }
                        }
                    )
                }

                composable(ROUTE_TEMPLATE_FILL) {
                    val tpl = pendingTemplate
                    if (tpl != null) {
                        TemplateFillScreen(
                            template = tpl,
                            slotMedia = pendingSlotMedia,
                            onBack = { navController.popBackStack() },
                            onPreviewTemplate = { viewModel.openTemplatePreview(tpl) },
                            onPickMultiplePhotos = {
                                pendingPickMode = PickMode.TEMPLATE_BATCH
                                multiPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onPickSingleSlotPhoto = { idx ->
                                pendingPickMode = PickMode.TEMPLATE_SINGLE_SLOT
                                pendingTargetSlotIndex = idx
                                singlePhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onLaunchEditor = { viewModel.launchEditorFromPendingTemplate() }
                        )
                    }
                }

                composable(ROUTE_EDITOR) {
                    val proj = activeProject
                    if (proj != null) {
                        EditorScreen(
                            project = proj,
                            playheadMs = playheadMs,
                            isPlaying = isPlaying,
                            selectedClipIndex = selectedClipIndex,
                            selectedTextId = selectedTextId,
                            selectedStickerId = selectedStickerId,
                            isRecordingVoiceover = isRecordingVoiceover,
                            recordingElapsedMs = recordingElapsedMs,
                            exportState = exportState,
                            onBack = {
                                viewModel.pausePlayback()
                                navController.popBackStack(ROUTE_HOME, inclusive = false)
                            },
                            onTogglePlay = { viewModel.togglePlayback() },
                            onSeek = { ms -> viewModel.seekTo(ms) },
                            onSelectClip = { idx -> viewModel.selectClip(idx) },
                            onSelectText = { id -> viewModel.selectText(id) },
                            onSelectSticker = { id -> viewModel.selectSticker(id) },
                            onRenameProject = { name -> viewModel.renameProject(name) },
                            onSetAspectRatio = { opt -> viewModel.setAspectRatio(opt) },
                            onAddPhotos = {
                                pendingPickMode = PickMode.ADD_TO_EDITOR
                                multiPhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onReplaceClipPhoto = { idx ->
                                pendingPickMode = PickMode.REPLACE_CLIP
                                pendingTargetSlotIndex = idx
                                singlePhotoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onUpdateClip = { idx, transform -> viewModel.updateClip(idx, transform) },
                            onApplyFilterToAll = { filter -> viewModel.applyFilterToAll(filter) },
                            onApplyAnimationToAll = { spec -> viewModel.applyAnimationToAll(spec) },
                            onMoveClip = { idx, dir -> viewModel.moveClip(idx, dir) },
                            onDuplicateClip = { idx -> viewModel.duplicateClip(idx) },
                            onDeleteClip = { idx -> viewModel.deleteClip(idx) },
                            onAddText = { txt, anim, color, font -> viewModel.addTextOverlay(txt, anim, color, font) },
                            onTransformText = { id, x, y, sc, rot -> viewModel.updateTextTransform(id, x, y, sc, rot) },
                            onDeleteText = { id -> viewModel.deleteTextOverlay(id) },
                            onAddSticker = { kind -> viewModel.addStickerOverlay(kind) },
                            onTransformSticker = { id, x, y, sc, rot -> viewModel.updateStickerTransform(id, x, y, sc, rot) },
                            onDeleteSticker = { id -> viewModel.deleteStickerOverlay(id) },
                            onSelectBuiltInMusic = { track -> viewModel.selectBuiltInMusic(track) },
                            onPickMusicFromPhone = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                            onUpdateMusicSettings = { vol, fin, fout ->
                                viewModel.updateMusicSettings(vol, fin, fout, 0L)
                            },
                            onRemoveMusic = { viewModel.removeMusic() },
                            onAutoBeatSync = { viewModel.autoBeatSyncProject() },
                            onToggleVoiceoverRecord = {
                                if (isRecordingVoiceover) {
                                    viewModel.stopVoiceoverRecording()
                                } else {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        viewModel.startVoiceoverRecording()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            onUpdateVoiceoverVolume = { id, vol -> viewModel.updateVoiceoverVolume(id, vol) },
                            onDeleteVoiceover = { id -> viewModel.deleteVoiceover(id) },
                            onUpdateWatermark = { wm -> viewModel.updateWatermark(wm) },
                            onExportMp4 = { name, res, fps -> viewModel.exportMp4(name, res, fps) },
                            onExportLvl = { name -> viewModel.exportLvl(name) },
                            onResetExportState = { viewModel.resetExportState() },
                            onShareFile = { uriStr, mime ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = mime
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Faylni ulashish"))
                            }
                        )
                    }
                }

                composable(ROUTE_ADMIN) {
                    AdminScreen(
                        firebaseConfig = firebaseConfig,
                        cloudTemplates = cloudTemplates,
                        usersList = usersList,
                        notifications = notifications,
                        statistics = statistics,
                        uploadProgress = adminUploadProgress,
                        migrationProgress = adminMigrationProgress,
                        onBack = { navController.popBackStack() },
                        onRefreshSync = { viewModel.syncCloud(silent = false) },
                        onParseFirebaseJson = { raw -> viewModel.parseFirebaseJson(raw) },
                        onUpdateFirebase = { cfg -> viewModel.updateAndMigrateFirebase(cfg) },
                        onUploadLvlTemplate = { uri, title, cat ->
                            viewModel.uploadLvlTemplateToCloud(uri, title, cat)
                        },
                        onDeleteCloudTemplate = { id -> viewModel.deleteCloudTemplate(id) },
                        onSendNotification = { title, msg -> viewModel.sendAdminNotification(title, msg) }
                    )
                }
            }
        }

        LvlLoadingOverlay(
            state = lvlLoadingState,
            onDismissError = { viewModel.dismissLvlError() }
        )

        previewingTemplate?.let { previewTpl ->
            TemplateLivePreviewDialog(
                template = previewTpl,
                isFavorite = previewTpl.id in favoriteTemplateIds,
                onToggleFavorite = { viewModel.toggleFavoriteTemplate(previewTpl.id) },
                onDismiss = { viewModel.closeTemplatePreview() },
                onUseTemplate = { tpl -> viewModel.startTemplateFlow(tpl) }
            )
        }

        if (showAuthPromptDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissAuthPromptDialog() },
                containerColor = StudioSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = CyanElectric
                    )
                },
                title = { Text("Hisob ochishni tavsiya qilamiz", color = Color.White) },
                text = {
                    Text(
                        text = "Siz ilovadan $usageCount marta foydalandingiz. Loyihalar va yangi imkoniyatlarni saqlab qolish uchun Profil bo'limidan ro'yxatdan o'ting.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.dismissAuthPromptDialog()
                            selectedHomeTab = 2
                            navController.popBackStack(ROUTE_HOME, inclusive = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric)
                    ) {
                        Text("Ro'yxatdan o'tish", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissAuthPromptDialog() }) {
                        Text("Keyinroq")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeHubScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    projects: List<MontageProject>,
    cloudTemplates: List<TemplateSpec>,
    favoriteTemplateIds: Set<String>,
    currentUser: UserProfile?,
    usageCount: Int,
    statistics: AppStatistics,
    notifications: List<CloudNotificationItem>,
    authLoading: Boolean,
    authError: String?,
    onClearAuthError: () -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPreviewTemplate: (TemplateSpec) -> Unit,
    onSelectTemplate: (TemplateSpec) -> Unit,
    onNewBlankMontage: () -> Unit,
    onOpenLvlFile: () -> Unit,
    onOpenSavedProject: (MontageProject) -> Unit,
    onDeleteSavedProject: (MontageProject) -> Unit,
    onVerifyAdminPassword: (String) -> Boolean,
    onNavigateAdmin: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp
        val isMediumOrExpanded = maxWidth >= 600.dp

        if (isMediumOrExpanded) {
            Scaffold(
                containerColor = StudioObsidian,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    NavigationRail(
                        containerColor = StudioSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { onSelectTab(0) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Rounded.AutoAwesomeMosaic else Icons.Outlined.AutoAwesomeMosaic,
                                    contentDescription = "Shablonlar"
                                )
                            },
                            label = { Text("Shablonlar") },
                            colors = railItemColors()
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { onSelectTab(1) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary,
                                    contentDescription = "Loyihalar"
                                )
                            },
                            label = { Text("Loyihalar") },
                            colors = railItemColors()
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { onSelectTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Rounded.Person else Icons.Outlined.Person,
                                    contentDescription = "Profil"
                                )
                            },
                            label = { Text("Profil") },
                            colors = railItemColors(),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        HomeTabContent(
                            selectedTab = selectedTab,
                            isExpandedWidth = isExpanded,
                            projects = projects,
                            cloudTemplates = cloudTemplates,
                            favoriteTemplateIds = favoriteTemplateIds,
                            currentUser = currentUser,
                            usageCount = usageCount,
                            statistics = statistics,
                            notifications = notifications,
                            authLoading = authLoading,
                            authError = authError,
                            onClearAuthError = onClearAuthError,
                            onRegister = onRegister,
                            onLogin = onLogin,
                            onLogout = onLogout,
                            onToggleFavorite = onToggleFavorite,
                            onPreviewTemplate = onPreviewTemplate,
                            onSelectTemplate = onSelectTemplate,
                            onNewBlankMontage = onNewBlankMontage,
                            onOpenLvlFile = onOpenLvlFile,
                            onOpenSavedProject = onOpenSavedProject,
                            onDeleteSavedProject = onDeleteSavedProject,
                            onVerifyAdminPassword = onVerifyAdminPassword,
                            onNavigateAdmin = onNavigateAdmin
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = StudioObsidian,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(containerColor = StudioSurface) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { onSelectTab(0) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Rounded.AutoAwesomeMosaic else Icons.Outlined.AutoAwesomeMosaic,
                                    contentDescription = "Shablonlar"
                                )
                            },
                            label = { Text("Shablonlar") },
                            colors = barItemColors()
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { onSelectTab(1) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Rounded.VideoLibrary else Icons.Outlined.VideoLibrary,
                                    contentDescription = "Loyihalar"
                                )
                            },
                            label = { Text("Loyihalar") },
                            colors = barItemColors()
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { onSelectTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Rounded.Person else Icons.Outlined.Person,
                                    contentDescription = "Profil"
                                )
                            },
                            label = { Text("Profil") },
                            colors = barItemColors(),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    HomeTabContent(
                        selectedTab = selectedTab,
                        isExpandedWidth = false,
                        projects = projects,
                        cloudTemplates = cloudTemplates,
                        favoriteTemplateIds = favoriteTemplateIds,
                        currentUser = currentUser,
                        usageCount = usageCount,
                        statistics = statistics,
                        notifications = notifications,
                        authLoading = authLoading,
                        authError = authError,
                        onClearAuthError = onClearAuthError,
                        onRegister = onRegister,
                        onLogin = onLogin,
                        onLogout = onLogout,
                        onToggleFavorite = onToggleFavorite,
                        onPreviewTemplate = onPreviewTemplate,
                        onSelectTemplate = onSelectTemplate,
                        onNewBlankMontage = onNewBlankMontage,
                        onOpenLvlFile = onOpenLvlFile,
                        onOpenSavedProject = onOpenSavedProject,
                        onDeleteSavedProject = onDeleteSavedProject,
                        onVerifyAdminPassword = onVerifyAdminPassword,
                        onNavigateAdmin = onNavigateAdmin
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    selectedTab: Int,
    isExpandedWidth: Boolean,
    projects: List<MontageProject>,
    cloudTemplates: List<TemplateSpec>,
    favoriteTemplateIds: Set<String>,
    currentUser: UserProfile?,
    usageCount: Int,
    statistics: AppStatistics,
    notifications: List<CloudNotificationItem>,
    authLoading: Boolean,
    authError: String?,
    onClearAuthError: () -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPreviewTemplate: (TemplateSpec) -> Unit,
    onSelectTemplate: (TemplateSpec) -> Unit,
    onNewBlankMontage: () -> Unit,
    onOpenLvlFile: () -> Unit,
    onOpenSavedProject: (MontageProject) -> Unit,
    onDeleteSavedProject: (MontageProject) -> Unit,
    onVerifyAdminPassword: (String) -> Boolean,
    onNavigateAdmin: () -> Unit
) {
    when (selectedTab) {
        0 -> TemplatesScreen(
            isExpandedWidth = isExpandedWidth,
            cloudTemplates = cloudTemplates,
            favoriteTemplateIds = favoriteTemplateIds,
            onToggleFavorite = onToggleFavorite,
            onPreviewTemplate = onPreviewTemplate,
            onSelectTemplate = onSelectTemplate,
            onNewBlankMontage = onNewBlankMontage,
            onOpenLvlFile = onOpenLvlFile,
            onVerifyAdminPassword = onVerifyAdminPassword,
            onNavigateAdmin = onNavigateAdmin
        )
        1 -> ProjectsScreen(
            projects = projects,
            onOpenProject = onOpenSavedProject,
            onDeleteProject = onDeleteSavedProject,
            onNewBlankMontage = onNewBlankMontage,
            onOpenLvlFile = onOpenLvlFile
        )
        else -> ProfileScreen(
            currentUser = currentUser,
            usageCount = usageCount,
            firebaseConnected = statistics.firebaseConnected,
            notifications = notifications,
            authLoading = authLoading,
            authError = authError,
            onClearAuthError = onClearAuthError,
            onRegister = onRegister,
            onLogin = onLogin,
            onLogout = onLogout
        )
    }
}

@Composable
private fun barItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CyanElectric,
    selectedTextColor = CyanElectric,
    indicatorColor = StudioSurfaceElevated,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)

@Composable
private fun railItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = CyanElectric,
    selectedTextColor = CyanElectric,
    indicatorColor = StudioSurfaceElevated,
    unselectedIconColor = TextSecondary,
    unselectedTextColor = TextSecondary
)
