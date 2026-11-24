package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipBackgroundStyle
import com.example.model.ClipFitMode
import com.example.model.ClipItem
import com.example.model.ClipMediaType
import com.example.model.StickerKind
import com.example.model.StickerOverlayItem
import com.example.model.VoiceoverItem
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerSheet(
    stickers: List<StickerOverlayItem>,
    onDismiss: () -> Unit,
    onAddSticker: (StickerKind) -> Unit,
    onSelectSticker: (String) -> Unit,
    onDeleteSticker: (String) -> Unit
) {
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
                text = "Stiker va Kino Ramkalar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ramka tanlang yoki stiker qo'shib barmoq bilan video ustida suring/burang.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .testTag("stickers_grid")
            ) {
                items(StickerKind.entries) { kind ->
                    val glyph = when (kind) {
                        StickerKind.POLAROID -> "📷"
                        StickerKind.VHS_RETRO -> "📼"
                        StickerKind.CINEMA_REC -> "🎬"
                        StickerKind.NEON_FRAME -> "🟪"
                        StickerKind.HEART -> "❤️"
                        StickerKind.FIRE -> "🔥"
                        StickerKind.LIGHTNING -> "⚡"
                        StickerKind.CROWN -> "👑"
                        StickerKind.STAR -> "⭐"
                        StickerKind.MUSIC_VIBE -> "🎵"
                        StickerKind.SPARKLES -> "✨"
                        StickerKind.SUNGLASSES -> "😎"
                    }
                    Surface(
                        onClick = {
                            onAddSticker(kind)
                            if (!kind.isFullFrame) onDismiss()
                        },
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (kind.isFullFrame) CyanElectric.copy(0.45f) else StudioOutline.copy(0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("sticker_item_${kind.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = glyph, fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = kind.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (kind.isFullFrame) CyanElectric else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (stickers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Faol stiker va ramkalar", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(stickers, key = { it.id }) { st ->
                        Surface(
                            onClick = {
                                if (!st.kind.isFullFrame) {
                                    onSelectSticker(st.id)
                                    onDismiss()
                                }
                            },
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = st.kind.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onDeleteSticker(st.id) }) {
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
fun ClipTransformCropSheet(
    clip: ClipItem,
    onDismiss: () -> Unit,
    onUpdateClip: ((ClipItem) -> ClipItem) -> Unit
) {
    val isVideo = clip.mediaType == ClipMediaType.VIDEO
    val maxVidDur = clip.videoDurationMs.coerceAtLeast(1000L).toFloat()
    var trimRange by remember(clip.id) {
        val start = clip.trimStartMs.toFloat().coerceIn(0f, maxVidDur - 400f)
        val end = (clip.trimEndMs.takeIf { it > clip.trimStartMs } ?: clip.videoDurationMs)
            .toFloat()
            .coerceIn(start + 400f, maxVidDur)
        mutableStateOf(start..end)
    }

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
                text = if (isVideo) "Video Qirqish, Tezlik va Fon" else "Kadrni Kesish, Burish va Fon",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isVideo) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = StudioSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Videoni qirqish (Trim): ${formatTimecode(trimRange.start.toLong())} - ${formatTimecode(trimRange.endInclusive.toLong())}",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyanElectric
                        )
                        RangeSlider(
                            value = trimRange,
                            onValueChange = { next ->
                                val clampedEnd = next.endInclusive.coerceAtLeast(next.start + 400f)
                                trimRange = next.start..clampedEnd
                                val newStart = next.start.toLong()
                                val newEnd = clampedEnd.toLong()
                                val newDur = ((newEnd - newStart) / clip.playbackSpeed.coerceAtLeast(0.25f))
                                    .toLong()
                                    .coerceIn(500L, 60_000L)
                                onUpdateClip {
                                    it.copy(
                                        trimStartMs = newStart,
                                        trimEndMs = newEnd,
                                        durationMs = newDur
                                    )
                                }
                            },
                            valueRange = 0f..maxVidDur
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tezlik (Speed)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                                val selected = kotlin.math.abs(clip.playbackSpeed - spd) < 0.05f
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        val span = (clip.trimEndMs - clip.trimStartMs).takeIf { it > 0L } ?: clip.videoDurationMs
                                        val newDur = (span / spd).toLong().coerceIn(500L, 60_000L)
                                        onUpdateClip { it.copy(playbackSpeed = spd, durationMs = newDur) }
                                    },
                                    label = { Text("${spd}x") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CoralMagenta.copy(0.22f),
                                        selectedLabelColor = CoralMagenta
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video asl ovozi: ${(clip.clipVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Slider(
                            value = clip.clipVolume,
                            onValueChange = { vol -> onUpdateClip { it.copy(clipVolume = vol) } },
                            colors = SliderDefaults.colors(thumbColor = GoldBeat, activeTrackColor = GoldBeat)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Burish va Akslantirish", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onUpdateClip { it.copy(rotationQuarterTurns = (it.rotationQuarterTurns + 1) % 4) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${clip.rotationQuarterTurns * 90}°")
                }
                OutlinedButton(
                    onClick = { onUpdateClip { it.copy(flipHorizontal = !it.flipHorizontal) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (clip.flipHorizontal) "Goriz ✓" else "Goriz")
                }
                OutlinedButton(
                    onClick = { onUpdateClip { it.copy(flipVertical = !it.flipVertical) } },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (clip.flipVertical) "Vert ✓" else "Vertikal")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Kadr sig'imi (Crop / Fit)", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClipFitMode.entries.forEach { mode ->
                    FilterChip(
                        selected = clip.fitMode == mode,
                        onClick = { onUpdateClip { it.copy(fitMode = mode) } },
                        leadingIcon = { Icon(Icons.Rounded.CropFree, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanElectric.copy(0.22f),
                            selectedLabelColor = CyanElectric
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Orqa fon (Chet bo'sh qolganda)", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClipBackgroundStyle.entries.forEach { bg ->
                    FilterChip(
                        selected = clip.backgroundStyle == bg,
                        onClick = {
                            onUpdateClip {
                                it.copy(
                                    fitMode = ClipFitMode.CONTAIN,
                                    backgroundStyle = bg
                                )
                            }
                        },
                        label = { Text(bg.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldBeat.copy(0.22f),
                            selectedLabelColor = GoldBeat
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tayyor", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceoverSheet(
    voiceovers: List<VoiceoverItem>,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    onDismiss: () -> Unit,
    onToggleRecord: () -> Unit,
    onUpdateVolume: (String, Float) -> Unit,
    onDeleteVoiceover: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        containerColor = StudioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ovoz berish (Voiceover)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Hozirgi playhead joyidan mikrofonga sharh yoki ovoz yozing.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0xFFE53935) else CoralMagenta)
                    .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable(onClick = onToggleRecord)
                    .testTag("voiceover_record_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isRecording) "To'xtatish" else "Yozishni boshlash",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isRecording) "Yozilmoqda… ${String.format(Locale.US, "%.1f", recordingElapsedMs / 1000f)} s"
                else "Yozish uchun bosing",
                style = MaterialTheme.typography.titleSmall,
                color = if (isRecording) Color(0xFFFF6B6B) else CyanElectric
            )

            if (voiceovers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(voiceovers, key = { it.id }) { vo ->
                        var vol by remember(vo.id) { mutableFloatStateOf(vo.volume) }
                        Surface(
                            color = StudioSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(vo.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        Text(
                                            text = "${formatTimecode(vo.startMs)} dan • ${String.format(Locale.US, "%.1f", vo.durationMs / 1000f)} s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(onClick = { onDeleteVoiceover(vo.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "O'chirish", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Slider(
                                    value = vol,
                                    valueRange = 0f..1.5f,
                                    onValueChange = {
                                        vol = it
                                        onUpdateVolume(vo.id, it)
                                    },
                                    colors = SliderDefaults.colors(thumbColor = CyanElectric, activeTrackColor = CyanElectric)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (isRecording) onToggleRecord()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tayyor", color = Color(0xFF001F26), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
