package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TemplateCatalog
import com.example.model.AnimationCatalog
import com.example.model.AnimationSpec
import com.example.model.ClipItem
import com.example.model.ExportFps
import com.example.model.ExportResolution
import com.example.model.ExportState
import com.example.model.FilterPreset
import com.example.model.MotionCategory
import com.example.model.MotionEffect
import com.example.model.MusicTrack
import com.example.model.TextAnimationStyle
import com.example.model.TextFontStyle
import com.example.model.TextOverlayItem
import com.example.model.TransitionType
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.video.EffectPreviewRenderer
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransitionPickerSheet(
    clip: ClipItem,
    onDismiss: () -> Unit,
    onApply: (TransitionType, Long) -> Unit
) {
    var selectedType by remember(clip.id) { mutableStateOf(clip.transition) }
    var durationMs by remember(clip.id) { mutableLongStateOf(clip.transitionDurationMs) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "O'tish effekti",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransitionType.entries.forEach { type ->
                    val selected = selectedType == type
                    FilterChip(
                        selected = selected,
                        onClick = { selectedType = type },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldBeat.copy(alpha = 0.22f),
                            selectedLabelColor = GoldBeat
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Davomiyligi: ${String.format(Locale.US, "%.2f", durationMs / 1000f)} s",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            Slider(
                value = durationMs.toFloat(),
                onValueChange = { durationMs = it.toLong() },
                valueRange = 200f..1200f,
                colors = SliderDefaults.colors(thumbColor = GoldBeat, activeTrackColor = GoldBeat)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    onApply(selectedType, durationMs)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Saqlash", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStudioSheet(
    currentMusic: MusicTrack?,
    onDismiss: () -> Unit,
    onSelectBuiltIn: (MusicTrack) -> Unit,
    onPickFromPhone: () -> Unit,
    onUpdateSettings: (volume: Float, fadeInMs: Long, fadeOutMs: Long) -> Unit,
    onAutoBeatSync: () -> Unit = {},
    onRemoveMusic: () -> Unit
) {
    var volume by remember(currentMusic?.id) { mutableFloatStateOf(currentMusic?.volume ?: 0.9f) }
    var fadeInMs by remember(currentMusic?.id) { mutableLongStateOf(currentMusic?.fadeInMs ?: 350L) }
    var fadeOutMs by remember(currentMusic?.id) { mutableLongStateOf(currentMusic?.fadeOutMs ?: 700L) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Musiqa Studiyasi",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (currentMusic != null) {
                    TextButton(onClick = {
                        onRemoveMusic()
                        onDismiss()
                    }) {
                        Icon(Icons.Rounded.MusicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("O'chirish", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    onPickFromPhone()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoralMagenta),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_phone_audio_button")
            ) {
                Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Telefon xotirasidan musiqa tanlash (MP3/M4A)", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    onAutoBeatSync()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("music_sheet_beat_sync_button")
            ) {
                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = GoldBeat)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kadrlarni musiqa ritmiga moslash (Auto Beat Sync)", color = GoldBeat, fontWeight = FontWeight.Bold)
            }

            if (currentMusic != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Tanlangan: ${currentMusic.title} (${currentMusic.bpm} BPM)",
                            style = MaterialTheme.typography.titleSmall,
                            color = CyanElectric
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Balandlik: ${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                onUpdateSettings(it, fadeInMs, fadeOutMs)
                            },
                            colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tayyor Studio Ritmlar (Beat Sync)",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TemplateCatalog.builtInMusicLibrary) { track ->
                    val isSelected = currentMusic?.builtInPreset == track.builtInPreset && currentMusic?.audioFilePath == null
                    Surface(
                        onClick = { onSelectBuiltIn(track) },
                        color = if (isSelected) CyanElectric.copy(alpha = 0.14f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isSelected) CyanElectric else StudioOutline.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (isSelected) CyanElectric else GoldBeat
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "${track.artist} • ${track.bpm} BPM",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = CyanElectric)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun rememberAnimPreviewBitmap(): Bitmap? {
    val context = LocalContext.current
    val bitmap = remember {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        BitmapFactory.decodeResource(context.resources, R.drawable.img_anim_preview, opts)
    }
    DisposableEffect(bitmap) {
        onDispose {
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterGradeSheet(
    clip: ClipItem,
    onDismiss: () -> Unit,
    onApplyClip: (FilterPreset, Float, Float, Float) -> Unit,
    onApplyAll: (FilterPreset) -> Unit
) {
    var selectedFilter by remember(clip.id) { mutableStateOf(clip.filter) }
    var brightness by remember(clip.id) { mutableFloatStateOf(clip.brightness) }
    var contrast by remember(clip.id) { mutableFloatStateOf(clip.contrast) }
    var saturation by remember(clip.id) { mutableFloatStateOf(clip.saturation) }

    val previewBitmap = rememberAnimPreviewBitmap()
    val staticSpec = remember { AnimationCatalog.animations.first() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtr va Ranglar (${FilterPreset.entries.size} xil)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedFilter.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanElectric
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FilterPreset.entries) { preset ->
                    val isSelected = selectedFilter == preset
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyanElectric.copy(alpha = 0.16f) else StudioSurfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CyanElectric else StudioOutline.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedFilter = preset
                                onApplyClip(preset, brightness, contrast, saturation)
                            }
                            .padding(6.dp)
                            .testTag("filter_item_${preset.name}")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioSurfaceElevated)
                        ) {
                            if (previewBitmap != null && !previewBitmap.isRecycled) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    EffectPreviewRenderer.drawSingleEffectPreview(
                                        canvas = drawContext.canvas.nativeCanvas,
                                        width = size.width,
                                        height = size.height,
                                        bitmap = previewBitmap,
                                        animSpec = staticSpec,
                                        filter = preset,
                                        progress = 0.5f
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) CyanElectric else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Yorqinlik", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onApplyClip(selectedFilter, it, contrast, saturation)
                },
                valueRange = -0.35f..0.35f,
                colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
            )

            Text("Kontrast", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
                value = contrast,
                onValueChange = {
                    contrast = it
                    onApplyClip(selectedFilter, brightness, it, saturation)
                },
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
            )

            Text("To'yinganlik", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onApplyClip(selectedFilter, brightness, contrast, it)
                },
                valueRange = 0f..1.8f,
                colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onApplyAll(selectedFilter)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Barchasiga qo'llash")
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tayyor", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotionDurationSheet(
    clip: ClipItem,
    onDismiss: () -> Unit,
    onUpdate: (AnimationSpec, Long, Float) -> Unit,
    onApplyAnimationToAll: (AnimationSpec) -> Unit
) {
    val initialSpec = remember(clip.id) {
        if (clip.animationId != "anim_001") AnimationCatalog.getById(clip.animationId)
        else if (clip.motion != MotionEffect.NONE) AnimationCatalog.fromLegacyMotion(clip.motion)
        else AnimationCatalog.animations.first()
    }
    var selectedSpec by remember(clip.id) { mutableStateOf(initialSpec) }
    var durationMs by remember(clip.id) { mutableLongStateOf(clip.durationMs) }
    var zoomScale by remember(clip.id) { mutableFloatStateOf(clip.zoomScale) }
    var selectedCategory by remember { mutableStateOf(MotionCategory.ALL) }

    val allAnimations = remember { AnimationCatalog.animations }
    val filteredAnimations = remember(selectedCategory) {
        if (selectedCategory == MotionCategory.ALL) allAnimations
        else allAnimations.filter { it.category == selectedCategory }
    }

    val previewBitmap = rememberAnimPreviewBitmap()
    var loopProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(40L)
            loopProgress = (loopProgress + 0.028f) % 1f
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Animatsiyalar (${allAnimations.size} xil)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = CoralMagenta.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "#${selectedSpec.number} ${selectedSpec.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CoralMagenta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MotionCategory.entries) { category ->
                    val active = selectedCategory == category
                    FilterChip(
                        selected = active,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanElectric.copy(alpha = 0.22f),
                            selectedLabelColor = CyanElectric,
                            containerColor = StudioSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .testTag("animations_200_grid")
            ) {
                items(filteredAnimations, key = { it.id }) { spec ->
                    val isSelected = selectedSpec.id == spec.id
                    Surface(
                        onClick = {
                            selectedSpec = spec
                            onUpdate(spec, durationMs, zoomScale)
                        },
                        color = if (isSelected) CoralMagenta.copy(alpha = 0.18f) else StudioSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CoralMagenta else StudioOutline.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("animation_item_${spec.id}")
                    ) {
                        Column(modifier = Modifier.padding(7.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.48f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceElevated)
                            ) {
                                if (previewBitmap != null && !previewBitmap.isRecycled) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        EffectPreviewRenderer.drawSingleEffectPreview(
                                            canvas = drawContext.canvas.nativeCanvas,
                                            width = size.width,
                                            height = size.height,
                                            bitmap = previewBitmap,
                                            animSpec = spec,
                                            filter = clip.filter,
                                            progress = loopProgress
                                        )
                                    }
                                }

                                Surface(
                                    color = if (isSelected) CoralMagenta else Color(0xBA0B0B13),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(5.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "#%03d", spec.number),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) Color.White else CyanElectric,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = spec.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Kadr davomiyligi: ${String.format(Locale.US, "%.1f", durationMs / 1000f)} s",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Slider(
                value = durationMs.toFloat(),
                onValueChange = {
                    durationMs = it.toLong()
                    onUpdate(selectedSpec, durationMs, zoomScale)
                },
                valueRange = 500f..7000f,
                colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
            )

            Text(
                text = "Masshtab (Zoom): ${String.format(Locale.US, "%.2f", zoomScale)}x",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Slider(
                value = zoomScale,
                onValueChange = {
                    zoomScale = it
                    onUpdate(selectedSpec, durationMs, it)
                },
                valueRange = 1.0f..2.0f,
                colors = SliderDefaults.colors(thumbColor = CoralMagenta, activeTrackColor = CoralMagenta)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onApplyAnimationToAll(selectedSpec)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Barchasiga qo'llash")
                }
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tayyor", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextOverlaySheet(
    texts: List<TextOverlayItem>,
    onDismiss: () -> Unit,
    onAddText: (String, TextAnimationStyle, Long, TextFontStyle) -> Unit,
    onSelectText: (String) -> Unit = {},
    onDeleteText: (String) -> Unit
) {
    var newText by remember { mutableStateOf("") }
    var selectedAnim by remember { mutableStateOf(TextAnimationStyle.FADE_UP) }
    var selectedFont by remember { mutableStateOf(TextFontStyle.DISPLAY) }
    var selectedColor by remember { mutableLongStateOf(0xFFFFFFFFL) }

    val colorChoices = listOf(
        0xFFFFFFFFL,
        0xFF00E5FFL,
        0xFFFF4D8DL,
        0xFFFFB84DL,
        0xFF2EE59DL
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Matn va Sarlavhalar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.OpenWith, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(14.dp))
                Text(
                    text = "Matn qo'shgach, video ustida barmoq bilan suring, kattalashtiring va burang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = newText,
                onValueChange = { newText = it },
                label = { Text("Yangi matn kiriting") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_overlay_input")
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorChoices.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(
                                width = if (selectedColor == argb) 2.5.dp else 1.dp,
                                color = if (selectedColor == argb) CyanElectric else StudioOutline,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = argb }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Shrift turi", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextFontStyle.entries.forEach { font ->
                    FilterChip(
                        selected = selectedFont == font,
                        onClick = { selectedFont = font },
                        label = { Text(font.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanElectric.copy(0.22f),
                            selectedLabelColor = CyanElectric
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Kirish animatsiyasi", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextAnimationStyle.entries.forEach { anim ->
                    FilterChip(
                        selected = selectedAnim == anim,
                        onClick = { selectedAnim = anim },
                        label = { Text(anim.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (newText.isNotBlank()) {
                        onAddText(newText, selectedAnim, selectedColor, selectedFont)
                        newText = ""
                        onDismiss()
                    }
                },
                enabled = newText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_text_confirm_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF001F26))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Matn qo'shish va joylash", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
            }

            if (texts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Mavjud matnlar (Ekranda tahrirlash uchun bosing)", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(texts, key = { it.id }) { item ->
                        Surface(
                            onClick = {
                                onSelectText(item.id)
                                onDismiss()
                            },
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.text,
                                        color = Color(item.colorArgb),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.fontStyle.label} • ${formatTimecode(item.startMs)}-${formatTimecode(item.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                IconButton(onClick = { onDeleteText(item.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportSheet(
    defaultFileName: String,
    watermark: WatermarkConfig,
    exportState: ExportState,
    onDismiss: () -> Unit,
    onUpdateWatermark: (WatermarkConfig) -> Unit,
    onExportMp4: (String, ExportResolution, ExportFps) -> Unit,
    onExportLvl: (String) -> Unit,
    onShareFile: (uriString: String, mimeType: String) -> Unit
) {
    var fileName by remember(defaultFileName) {
        mutableStateOf(
            defaultFileName.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_").ifBlank { "lavelle_montaj" }
        )
    }
    var selectedResolution by remember { mutableStateOf(ExportResolution.FHD_1080) }
    var selectedFps by remember { mutableStateOf(ExportFps.FPS_60) }

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportState.Exporting) onDismiss()
        },
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            when (exportState) {
                is ExportState.Idle -> {
                    Text(
                        text = "Eksport va Yuklab olish",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1080p Full HD / 60 FPS video yoki to'liq .lvl loyiha fayli sifatida saqlang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Fayl nomi") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_filename_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Video sifati va kadr tezligi", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportResolution.entries.forEach { res ->
                            FilterChip(
                                selected = selectedResolution == res,
                                onClick = { selectedResolution = res },
                                label = { Text(res.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanElectric.copy(0.22f),
                                    selectedLabelColor = CyanElectric
                                ),
                                modifier = Modifier.testTag("export_res_${res.name}")
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportFps.entries.forEach { fpsOpt ->
                            FilterChip(
                                selected = selectedFps == fpsOpt,
                                onClick = { selectedFps = fpsOpt },
                                label = { Text(fpsOpt.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CoralMagenta.copy(0.22f),
                                    selectedLabelColor = CoralMagenta
                                ),
                                modifier = Modifier.testTag("export_fps_${fpsOpt.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioOutline.copy(0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Shaxsiy Watermark (Imzo)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("Video burchagiga nik/logo qo'shish", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Switch(
                                    checked = watermark.enabled,
                                    onCheckedChange = { onUpdateWatermark(watermark.copy(enabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyanElectric, checkedTrackColor = CyanElectric.copy(0.35f)),
                                    modifier = Modifier.testTag("watermark_switch")
                                )
                            }
                            if (watermark.enabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = watermark.text,
                                    onValueChange = { onUpdateWatermark(watermark.copy(text = it)) },
                                    label = { Text("Nik yoki brend nomi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    WatermarkPosition.entries.forEach { pos ->
                                        FilterChip(
                                            selected = watermark.position == pos,
                                            onClick = { onUpdateWatermark(watermark.copy(position = pos)) },
                                            label = { Text(pos.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Export MP4 Video button
                    Surface(
                        onClick = { onExportMp4(fileName, selectedResolution, selectedFps) },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(CyanElectric, Color(0xFF0099CC))))
                            .testTag("export_mp4_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Movie, contentDescription = null, tint = Color(0xFF001F26), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Video (MP4) yuklab olish",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF001F26),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedResolution.label} • ${selectedFps.label} • Movies/Lavelle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF003240)
                                )
                            }
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = Color(0xFF001F26))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Export .lvl Package button
                    Surface(
                        onClick = { onExportLvl(fileName) },
                        color = StudioSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(GoldBeat, CoralMagenta)),
                                RoundedCornerShape(16.dp)
                            )
                            .testTag("export_lvl_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FolderZip, contentDescription = null, tint = GoldBeat, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${fileName.ifBlank { "video" }}.lvl eksport qilish",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Download papkasiga rasm, video, ovoz va montaji bilan saqlanadi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = GoldBeat)
                        }
                    }
                }

                is ExportState.Exporting -> {
                    Text(
                        text = "${exportState.formatLabel} tayyorlanmoqda…",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { exportState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = CyanElectric,
                        trackColor = StudioSurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = exportState.stepLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${(exportState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanElectric
                        )
                    }
                }

                is ExportState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Muvaffaqiyatli",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "${exportState.formatLabel} saqlandi!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = exportState.displayPath,
                                style = MaterialTheme.typography.labelMedium,
                                color = GoldBeat,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (exportState.shareUriString != null) {
                                OutlinedButton(
                                    onClick = { onShareFile(exportState.shareUriString, exportState.mimeType) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ulashish")
                                }
                            }
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tayyor", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                is ExportState.Failed -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Xato",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Eksport amalga oshmadi", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(exportState.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Yopish")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
