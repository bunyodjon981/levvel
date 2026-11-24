package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TemplateCatalog
import com.example.model.AppStatistics
import com.example.model.CloudNotificationItem
import com.example.model.FirebaseConfig
import com.example.model.TemplateSpec
import com.example.model.UserProfile
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AdminSection(val label: String, val icon: ImageVector) {
    FIREBASE("Firebase sozlamalari", Icons.Rounded.Settings),
    ADD_TEMPLATE("Shablon qo'shish", Icons.Rounded.AddCircle),
    DELETE_TEMPLATE("Shablonni o'chirish", Icons.Rounded.Delete),
    STATS("Statistika", Icons.Rounded.Analytics),
    NOTIFY("Xabar yuborish", Icons.Rounded.NotificationsActive),
    USERS("Foydalanuvchilar", Icons.Rounded.Group)
}

@Composable
fun AdminScreen(
    firebaseConfig: FirebaseConfig,
    cloudTemplates: List<TemplateSpec>,
    usersList: List<UserProfile>,
    notifications: List<CloudNotificationItem>,
    statistics: AppStatistics,
    uploadProgress: Float?,
    migrationProgress: Pair<String, Float>?,
    onBack: () -> Unit,
    onRefreshSync: () -> Unit,
    onParseFirebaseJson: (String) -> FirebaseConfig,
    onUpdateFirebase: (FirebaseConfig) -> Unit,
    onUploadLvlTemplate: (uri: Uri, title: String, category: String) -> Unit,
    onDeleteCloudTemplate: (templateId: String) -> Unit,
    onSendNotification: (title: String, message: String) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedSection by remember { mutableStateOf(AdminSection.FIREBASE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Lavelle Admin Paneli", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(
                    text = if (firebaseConfig.isConfigured) "Firebase ulangan" else "Firebase qo'shilmagan",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (firebaseConfig.isConfigured) EmeraldSuccess else GoldBeat
                )
            }
            Surface(
                color = if (statistics.firebaseConnected) EmeraldSuccess.copy(alpha = 0.16f) else StudioSurfaceVariant,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (statistics.firebaseConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (statistics.firebaseConnected) EmeraldSuccess else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (statistics.firebaseConnected) "${statistics.firebaseLatencyMs} ms" else "Oflayn",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statistics.firebaseConnected) EmeraldSuccess else TextSecondary
                    )
                }
            }
            IconButton(onClick = onRefreshSync) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Yangilash", tint = CyanElectric)
            }
        }

        // Admin Sections Tab Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminSection.entries) { section ->
                val active = selectedSection == section
                FilterChip(
                    selected = active,
                    onClick = { selectedSection = section },
                    leadingIcon = {
                        Icon(section.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(section.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanElectric.copy(alpha = 0.22f),
                        selectedLabelColor = CyanElectric,
                        selectedLeadingIconColor = CyanElectric,
                        containerColor = StudioSurface,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("admin_tab_${section.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSection) {
                AdminSection.FIREBASE -> FirebaseSettingsSection(
                    config = firebaseConfig,
                    migrationProgress = migrationProgress,
                    onParseJson = onParseFirebaseJson,
                    onUpdateFirebase = onUpdateFirebase
                )
                AdminSection.ADD_TEMPLATE -> AddTemplateSection(
                    isFirebaseConfigured = firebaseConfig.isConfigured,
                    uploadProgress = uploadProgress,
                    onUpload = onUploadLvlTemplate
                )
                AdminSection.DELETE_TEMPLATE -> DeleteTemplateSection(
                    cloudTemplates = cloudTemplates,
                    onDeleteTemplate = onDeleteCloudTemplate
                )
                AdminSection.STATS -> StatisticsSection(
                    statistics = statistics,
                    onRefresh = onRefreshSync
                )
                AdminSection.NOTIFY -> NotificationSenderSection(
                    notifications = notifications,
                    onSendNotification = onSendNotification
                )
                AdminSection.USERS -> UsersListSection(usersList = usersList)
            }
        }
    }
}

@Composable
private fun FirebaseSettingsSection(
    config: FirebaseConfig,
    migrationProgress: Pair<String, Float>?,
    onParseJson: (String) -> FirebaseConfig,
    onUpdateFirebase: (FirebaseConfig) -> Unit
) {
    var showEditForm by remember { mutableStateOf(false) }
    var dbUrlInput by remember(config.databaseUrl) { mutableStateOf(config.databaseUrl) }
    var projectIdInput by remember(config.projectId) { mutableStateOf(config.projectId) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var appIdInput by remember(config.appId) { mutableStateOf(config.appId) }
    var bucketInput by remember(config.storageBucket) { mutableStateOf(config.storageBucket) }
    var pastedJson by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Joriy Firebase ma'lumotlari",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                if (!config.isConfigured) {
                    Text(
                        text = "Firebase hali qo'shilmagan. Ilova hozircha oflayn rejimda ishlamoqda. Shablon ulashish va xabarlar uchun firebase.json faylini to'ldiring yoki quyidan yangi Firebase qo'shing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldBeat
                    )
                } else {
                    ConfigKeyValueRow("Project ID", config.projectId.ifBlank { "-" })
                    ConfigKeyValueRow("Database URL", config.databaseUrl.ifBlank { "-" })
                    ConfigKeyValueRow(
                        "API Key",
                        if (config.apiKey.length > 8) "${config.apiKey.take(5)}••••${config.apiKey.takeLast(4)}"
                        else config.apiKey.ifBlank { "-" }
                    )
                    ConfigKeyValueRow("App ID", config.appId.ifBlank { "-" })
                    ConfigKeyValueRow("Storage Bucket", config.storageBucket.ifBlank { "-" })
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showEditForm = !showEditForm },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_update_toggle_button")
                ) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = Color(0xFF001F26))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showEditForm) "Shaklni yopish" else "Firebase ma'lumotlarini yangilash",
                        color = Color(0xFF001F26),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (migrationProgress != null) {
            Surface(
                color = StudioSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(migrationProgress.first, style = MaterialTheme.typography.titleSmall, color = CyanElectric)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { migrationProgress.second },
                        color = CyanElectric,
                        trackColor = StudioSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        if (showEditForm) {
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanElectric.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Yangi Firebase ulanishi (Avtomatik ko'chirish)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanElectric
                    )
                    Text(
                        text = "Yuklashni bossangiz, barcha shablonlar, foydalanuvchilar va xabarlar yangi Firebase ga ko'chiriladi va boshqa telefonlarga ham avtomatik saqlanadi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = pastedJson,
                        onValueChange = { raw ->
                            pastedJson = raw
                            val parsed = onParseJson(raw)
                            if (parsed.isConfigured) {
                                dbUrlInput = parsed.databaseUrl
                                projectIdInput = parsed.projectId
                                apiKeyInput = parsed.apiKey
                                appIdInput = parsed.appId
                                bucketInput = parsed.storageBucket
                            }
                        },
                        label = { Text("Ixtiyoriy: firebase.json / google-services.json joylang") },
                        maxLines = 4,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dbUrlInput,
                        onValueChange = { dbUrlInput = it },
                        label = { Text("Realtime Database URL (masalan https://...-default-rtdb.firebaseio.com)") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = projectIdInput,
                        onValueChange = { projectIdInput = it },
                        label = { Text("Project ID") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appIdInput,
                        onValueChange = { appIdInput = it },
                        label = { Text("App ID (ixtiyoriy)") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bucketInput,
                        onValueChange = { bucketInput = it },
                        label = { Text("Storage Bucket (ixtiyoriy)") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onUpdateFirebase(
                                FirebaseConfig(
                                    projectId = projectIdInput.trim(),
                                    apiKey = apiKeyInput.trim(),
                                    databaseUrl = dbUrlInput.trim().trimEnd('/'),
                                    appId = appIdInput.trim(),
                                    storageBucket = bucketInput.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralMagenta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firebase_migrate_submit_button")
                    ) {
                        Text("Yuklash va Barcha Telefonlarga Ko'chirish", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AddTemplateSection(
    isFirebaseConfigured: Boolean,
    uploadProgress: Float?,
    onUpload: (Uri, String, String) -> Unit
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var templateTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Reels") }

    val lvlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            if (templateTitle.isBlank()) {
                templateTitle = uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.removeSuffix(".lvl")
                    .orEmpty()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isFirebaseConfigured) {
            Surface(
                color = CoralMagenta.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CoralMagenta.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Text(
                    text = "Diqqat: Firebase ulanmagan! Shablon qo'shib barcha foydalanuvchilarga yuborish uchun avval Firebase sozlamalarini kiriting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralMagenta,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Shablon yuklang (.lvl fayl)", style = MaterialTheme.typography.titleMedium, color = Color.White)

                OutlinedButton(
                    onClick = { lvlPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pick_lvl_button")
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = GoldBeat)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedUri == null) "Telefon xotirasidan .lvl fayl tanlash"
                        else "Tanlandi: ${selectedUri?.lastPathSegment?.substringAfterLast('/')}",
                        color = GoldBeat
                    )
                }

                OutlinedTextField(
                    value = templateTitle,
                    onValueChange = { templateTitle = it },
                    label = { Text("Shablon nomini kiriting") },
                    singleLine = true,
                    colors = adminInputColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_template_name_input")
                )

                Text("Kategoriya", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TemplateCatalog.categories.filter { it != "Barchasi" }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                if (uploadProgress != null) {
                    Surface(
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shablon yuklanmoqda…", style = MaterialTheme.typography.labelLarge, color = CyanElectric)
                                Text("${(uploadProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = CyanElectric)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress },
                                color = CyanElectric,
                                trackColor = StudioSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val uri = selectedUri
                        if (uri != null) {
                            onUpload(uri, templateTitle, selectedCategory)
                        }
                    },
                    enabled = selectedUri != null && templateTitle.isNotBlank() && uploadProgress == null,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_upload_lvl_submit")
                ) {
                    Text("Shablonni yuklash", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeleteTemplateSection(
    cloudTemplates: List<TemplateSpec>,
    onDeleteTemplate: (String) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<TemplateSpec?>(null) }

    if (cloudTemplates.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "O'chirish uchun qo'shilgan shablonlar yo'q",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(cloudTemplates, key = { it.id }) { tpl ->
                Surface(
                    onClick = { pendingDelete = tpl },
                    color = StudioSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tpl.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(tpl.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        IconButton(onClick = { pendingDelete = tpl }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { tpl ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = StudioSurface,
            title = { Text("Shablonni o'chirish?", color = Color.White) },
            text = { Text("\"${tpl.title}\" barcha foydalanuvchilardan o'chirilsinmi?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTemplate(tpl.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("O'chirish", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@Composable
private fun StatisticsSection(
    statistics: AppStatistics,
    onRefresh: () -> Unit
) {
    val lastSyncText = remember(statistics.lastSyncTimeMs) {
        if (statistics.lastSyncTimeMs <= 0L) "Hali ulanmagan"
        else SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(statistics.lastSyncTimeMs))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminStatCard("Foydalanuvchilar", "${statistics.totalUsers}", CyanElectric, Modifier.weight(1f))
            AdminStatCard("Cloud Shablonlar", "${statistics.totalCloudTemplates}", CoralMagenta, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminStatCard("Asosiy Shablonlar", "${statistics.totalBuiltInTemplates}", GoldBeat, Modifier.weight(1f))
            AdminStatCard("Jami Montajlar", "${statistics.totalMontagesCreated}", EmeraldSuccess, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminStatCard("Jami Eksportlar", "${statistics.totalExports}", CyanElectric, Modifier.weight(1f))
            AdminStatCard("Xabarlar", "${statistics.notificationCount}", GoldBeat, Modifier.weight(1f))
        }

        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Ilova kechikishi va tizim holati", style = MaterialTheme.typography.titleMedium, color = Color.White)
                ConfigKeyValueRow(
                    "Firebase server kechikishi",
                    if (statistics.firebaseLatencyMs >= 0L) "${statistics.firebaseLatencyMs} ms" else "Ulanmagan"
                )
                ConfigKeyValueRow("Ilova javob kechikishi", "${statistics.appFrameLatencyMs} ms")
                ConfigKeyValueRow("Oxirgi sinxronizatsiya", lastSyncText)
            }
        }

        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = CyanElectric)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Statistikani yangilash", color = CyanElectric)
        }
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, StudioOutline.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun NotificationSenderSection(
    notifications: List<CloudNotificationItem>,
    onSendNotification: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("Lavelle Yangilik") }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StudioOutline.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Barcha foydalanuvchilarga xabar yuborish",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Xabar sarlavhasi") },
                        singleLine = true,
                        colors = adminInputColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Xabar matnini kiriting") },
                        minLines = 3,
                        colors = adminInputColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_notification_input")
                    )
                    Button(
                        onClick = {
                            if (message.isNotBlank()) {
                                onSendNotification(title, message)
                                message = ""
                            }
                        },
                        enabled = message.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_send_notification_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, tint = Color(0xFF001F26))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bildirishnoma yuborish", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (notifications.isNotEmpty()) {
            item {
                Text("Yuborilgan xabarlar", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            }
            items(notifications, key = { it.id }) { item ->
                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = CyanElectric)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(item.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersListSection(usersList: List<UserProfile>) {
    if (usersList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hozircha ro'yxatdan o'tgan foydalanuvchilar yo'q",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(usersList, key = { _, u -> u.id }) { index, user ->
                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CyanElectric.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyanElectric,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Text(
                            text = "${user.montageCount} montaj",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldBeat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun adminInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioSurfaceElevated,
    unfocusedContainerColor = StudioSurfaceElevated,
    focusedBorderColor = CyanElectric,
    unfocusedBorderColor = StudioOutline.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
