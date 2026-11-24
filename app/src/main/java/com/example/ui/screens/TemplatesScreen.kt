package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.cloud.AuthSecurity
import com.example.data.TemplateCatalog
import com.example.model.ClipMediaType
import com.example.model.MontageProject
import com.example.model.TemplateSpec
import com.example.ui.components.MontagePreviewCanvas
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun TemplatesScreen(
    isExpandedWidth: Boolean,
    cloudTemplates: List<TemplateSpec>,
    favoriteTemplateIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onPreviewTemplate: (TemplateSpec) -> Unit,
    onSelectTemplate: (TemplateSpec) -> Unit,
    onNewBlankMontage: () -> Unit,
    onOpenLvlFile: () -> Unit,
    onVerifyAdminPassword: (String) -> Boolean,
    onNavigateAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Barchasi") }
    var searchQuery by remember { mutableStateOf("") }
    var showAdminPasswordDialog by remember { mutableStateOf(false) }
    var adminPassInput by remember { mutableStateOf("") }
    var adminPassError by remember { mutableStateOf<String?>(null) }

    val allTemplates = remember(cloudTemplates) {
        cloudTemplates + TemplateCatalog.templates
    }

    val filterCategories = remember {
        listOf("Barchasi", "Sevimlilar") + TemplateCatalog.categories.filterNot { it == "Barchasi" }
    }

    val filteredTemplates = remember(selectedCategory, searchQuery, allTemplates, favoriteTemplateIds) {
        val catFiltered = when (selectedCategory) {
            "Barchasi" -> allTemplates
            "Sevimlilar" -> allTemplates.filter { it.id in favoriteTemplateIds }
            else -> allTemplates.filter { it.category == selectedCategory }
        }
        val q = searchQuery.trim()
        if (q.isEmpty()) catFiltered
        else catFiltered.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.subtitle.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true)
        }
    }

    val featuredTemplate = allTemplates.firstOrNull() ?: TemplateCatalog.templates.first()
    val columns = if (isExpandedWidth) 3 else 2

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
    ) {
        // Studio Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(CyanElectric, CoralMagenta))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF0B0B13),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Lavelle",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "VN Shablon & Montaj Studiyasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenLvlFile,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("open_lvl_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = ".lvl ochish",
                        tint = GoldBeat,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(".lvl", color = GoldBeat, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNewBlankMontage,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("new_montage_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Yangi montaj",
                        tint = Color(0xFF001F26),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yangi", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Search Bar ("Shablon qidirish")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { next ->
                if (AuthSecurity.isAdminSearchTrigger(next)) {
                    searchQuery = ""
                    adminPassInput = ""
                    adminPassError = null
                    showAdminPasswordDialog = true
                } else {
                    searchQuery = next
                }
            },
            placeholder = { Text("Shablon qidirish…", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Shablon qidirish",
                    tint = CyanElectric
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tozalash",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = StudioSurface,
                unfocusedContainerColor = StudioSurface,
                focusedBorderColor = CyanElectric,
                unfocusedBorderColor = StudioOutline.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .testTag("template_search_input")
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (searchQuery.isBlank()) {
                item(span = { GridItemSpan(columns) }) {
                    FeaturedTemplateHero(
                        template = featuredTemplate,
                        isFavorite = featuredTemplate.id in favoriteTemplateIds,
                        onToggleFavorite = { onToggleFavorite(featuredTemplate.id) },
                        onPreview = { onPreviewTemplate(featuredTemplate) },
                        onClick = { onSelectTemplate(featuredTemplate) }
                    )
                }
            }

            // Category Filter Chips
            item(span = { GridItemSpan(columns) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                ) {
                    items(filterCategories) { category ->
                        val active = selectedCategory == category
                        val isFavChip = category == "Sevimlilar"
                        FilterChip(
                            selected = active,
                            onClick = { selectedCategory = category },
                            leadingIcon = if (isFavChip) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = if (active) CoralMagenta else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isFavChip) CoralMagenta.copy(alpha = 0.2f) else CyanElectric.copy(alpha = 0.2f),
                                selectedLabelColor = if (isFavChip) CoralMagenta else CyanElectric,
                                containerColor = StudioSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.testTag("category_chip_$category")
                        )
                    }
                }
            }

            if (filteredTemplates.isEmpty()) {
                item(span = { GridItemSpan(columns) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selectedCategory == "Sevimlilar") Icons.Rounded.FavoriteBorder else Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCategory == "Sevimlilar") "Hali sevimli shablonlar qo'shilmagan" else "Hech qanday shablon topilmadi",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // VN Template Cards Grid
                items(filteredTemplates, key = { it.id }) { tpl ->
                    TemplateGridCard(
                        template = tpl,
                        isFavorite = tpl.id in favoriteTemplateIds,
                        onToggleFavorite = { onToggleFavorite(tpl.id) },
                        onPreview = { onPreviewTemplate(tpl) },
                        onClick = { onSelectTemplate(tpl) }
                    )
                }
            }
        }
    }

    if (showAdminPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPasswordDialog = false },
            containerColor = StudioSurface,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = "Admin",
                    tint = CyanElectric,
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text("Admin paneli", color = Color.White)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Boshqaruv paneliga kirish uchun maxfiy admin parolini kiriting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = adminPassInput,
                        onValueChange = {
                            adminPassInput = it
                            adminPassError = null
                        },
                        label = { Text("Admin paroli") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = adminPassError != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )
                    if (adminPassError != null) {
                        Text(
                            text = adminPassError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onVerifyAdminPassword(adminPassInput)) {
                            showAdminPasswordDialog = false
                            adminPassInput = ""
                            onNavigateAdmin()
                        } else {
                            adminPassError = "Parol noto'g'ri"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    modifier = Modifier.testTag("admin_password_confirm")
                ) {
                    Text("Kirish", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasswordDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@Composable
private fun FeaturedTemplateHero(
    template: TemplateSpec,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPreview: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverId = remember(template.coverDrawableName) {
        context.resources.getIdentifier(template.coverDrawableName, "drawable", context.packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(StudioSurface)
            .border(
                1.dp,
                Brush.linearGradient(listOf(CyanElectric.copy(0.6f), CoralMagenta.copy(0.6f))),
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .testTag("featured_template_card")
    ) {
        if (!template.coverImagePath.isNullOrBlank()) {
            AsyncImage(
                model = File(template.coverImagePath),
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (coverId != 0) {
            Image(
                painter = painterResource(id = coverId),
                contentDescription = template.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xE80B0B13),
                            Color(0xB30B0B13),
                            Color(0x330B0B13)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CoralMagenta,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (template.isCloudTemplate) "YANGI SHABLON" else "TOP SHABLON",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color(0x990B0B13),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "${template.slotCount} kadr • ${template.bpm} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanElectric,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x990B0B13))
                        .testTag("hero_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Sevimli",
                        tint = if (isFavorite) CoralMagenta else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = template.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD5D9ED),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onPreview,
                        color = Color(0xCC0B0B13),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(1.dp, GoldBeat.copy(0.6f), RoundedCornerShape(14.dp))
                            .testTag("hero_preview_demo_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircleOutline,
                                contentDescription = "Demo ko'rish",
                                tint = GoldBeat,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Demo", style = MaterialTheme.typography.labelLarge, color = GoldBeat)
                        }
                    }

                    Surface(
                        onClick = onClick,
                        color = CyanElectric,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF001F26),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ishlatish",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF001F26),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateGridCard(
    template: TemplateSpec,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPreview: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val coverId = remember(template.coverDrawableName) {
        context.resources.getIdentifier(template.coverDrawableName, "drawable", context.packageName)
    }

    Surface(
        onClick = onClick,
        color = StudioSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioOutline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .testTag("template_card_${template.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .background(StudioSurfaceElevated)
            ) {
                if (!template.coverImagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(template.coverImagePath),
                        contentDescription = template.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (coverId != 0) {
                    Image(
                        painter = painterResource(id = coverId),
                        contentDescription = template.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC0B0B13))
                            )
                        )
                )

                // Top-left aspect ratio + cloud + video cover badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = Color(0xB30B0B13),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = template.aspectRatio.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (!template.previewVideoPath.isNullOrBlank()) {
                        Surface(
                            color = CyanElectric.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Videocam,
                                contentDescription = "Video muqova",
                                tint = Color(0xFF001F26),
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                    if (template.isCloudTemplate) {
                        Surface(
                            color = CoralMagenta.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = "Cloud",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }

                // Top-right favorite heart + slot count
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xB30B0B13),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(template.accentColorHex),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${template.slotCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        onClick = onToggleFavorite,
                        color = Color(0xB30B0B13),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("favorite_toggle_${template.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Sevimli",
                                tint = if (isFavorite) CoralMagenta else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Bottom music + Demo preview pill
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = GoldBeat,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = template.music.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        onClick = onPreview,
                        color = GoldBeat.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, GoldBeat.copy(0.6f), RoundedCornerShape(50))
                            .testTag("template_demo_button_${template.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.PlayCircleOutline, contentDescription = "Demo", tint = GoldBeat, modifier = Modifier.size(12.dp))
                            Text(
                                text = String.format(Locale.US, "%.1fs", template.totalDurationMs / 1000f),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBeat
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = template.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TemplateLivePreviewDialog(
    template: TemplateSpec,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit,
    onUseTemplate: (TemplateSpec) -> Unit
) {
    val demoProject = remember(template) {
        val clips = template.clips.mapIndexed { idx, clip ->
            if (idx == 0 && !template.previewVideoPath.isNullOrBlank()) {
                clip.copy(
                    mediaType = ClipMediaType.VIDEO,
                    videoPath = template.previewVideoPath
                )
            } else clip
        }
        MontageProject(
            name = template.title,
            templateId = template.id,
            aspectRatio = template.aspectRatio,
            clips = clips,
            texts = template.texts,
            music = template.music
        )
    }

    var playheadMs by remember(template.id) { mutableLongStateOf(0L) }
    var playing by remember(template.id) { mutableStateOf(true) }
    val totalMs = demoProject.totalDurationMs.coerceAtLeast(1000L)

    LaunchedEffect(playing, totalMs) {
        while (playing) {
            delay(33L)
            playheadMs = (playheadMs + 33L) % totalMs
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .border(1.dp, CyanElectric.copy(0.45f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jonli Demo: ${template.title}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${template.slotCount} kadr • ${template.bpm} BPM • ${template.aspectRatio.shortLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Sevimli",
                            tint = if (isFavorite) CoralMagenta else Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Yopish", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                MontagePreviewCanvas(
                    project = demoProject,
                    playheadMs = playheadMs,
                    isPlaying = playing,
                    interactive = false,
                    onTogglePlay = { playing = !playing },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yopish")
                    }
                    Button(
                        onClick = { onUseTemplate(template) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("demo_use_template_button")
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF001F26))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ishlatish", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
