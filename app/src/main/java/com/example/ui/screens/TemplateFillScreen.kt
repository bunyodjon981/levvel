package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.io.ProjectRepository
import com.example.model.ClipMediaType
import com.example.model.TemplateSpec
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@Composable
fun TemplateFillScreen(
    template: TemplateSpec,
    slotMedia: List<ProjectRepository.ImportedMediaItem?>,
    onBack: () -> Unit,
    onPreviewTemplate: () -> Unit,
    onPickMultiplePhotos: () -> Unit,
    onPickSingleSlotPhoto: (Int) -> Unit,
    onLaunchEditor: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val filledCount = slotMedia.count { it != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Orqaga",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.slotCount} ta kadr • ${template.bpm} BPM • ${template.aspectRatio.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            OutlinedButton(
                onClick = onPreviewTemplate,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("template_fill_demo_button")
            ) {
                Icon(Icons.Rounded.PlayCircleOutline, contentDescription = "Jonli ko'rish", tint = GoldBeat, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Jonli demo", color = GoldBeat, style = MaterialTheme.typography.labelMedium)
            }
        }

        // Template Info Banner + Batch Pick CTA
        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, StudioOutline.copy(0.6f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = GoldBeat)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Musiqa: ${template.music.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = template.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onPickMultiplePhotos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_pick_photos_button")
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = CyanElectric)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Galereyadan ${template.slotCount} ta rasm/video tanlash",
                        color = CyanElectric,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Shablon katakchalari ($filledCount/${template.slotCount} tanlangan)",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(template.clips.size) { idx ->
                val clip = template.clips[idx]
                val pickedMedia = slotMedia.getOrNull(idx)
                val pickedPath = pickedMedia?.imagePath
                val isFilled = pickedMedia != null && !pickedPath.isNullOrBlank()
                val isVideo = pickedMedia?.mediaType == ClipMediaType.VIDEO

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioSurfaceElevated)
                        .border(
                            width = if (isFilled) 2.dp else 1.dp,
                            color = if (isFilled) EmeraldSuccess else StudioOutline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onPickSingleSlotPhoto(idx) }
                        .testTag("template_slot_$idx")
                ) {
                    if (isFilled && pickedPath != null) {
                        AsyncImage(
                            model = File(pickedPath),
                            contentDescription = clip.slotLabel,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val resId = remember(clip.sampleDrawableName) {
                            clip.sampleDrawableName?.let {
                                context.resources.getIdentifier(it, "drawable", context.packageName)
                            } ?: 0
                        }
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = clip.slotLabel,
                                contentScale = ContentScale.Crop,
                                alpha = 0.42f,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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

                    if (isFilled) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isVideo) {
                                Surface(
                                    color = CoralMagenta,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Rounded.Videocam, contentDescription = "Video", tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text("VIDEO", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Tanlangan",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = "Rasm yoki video tanlash",
                            tint = CyanElectric,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(34.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clip.slotLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color(0x990B0B13),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fs", clip.durationMs / 1000f),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldBeat,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Launch Editor Bottom Bar
        Surface(
            color = StudioSurface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onLaunchEditor,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filledCount > 0) CyanElectric else CoralMagenta
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_template_editor_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (filledCount > 0) Color(0xFF001F26) else Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (filledCount > 0) "Montajga o'tish ($filledCount/${template.slotCount})" else "Namuna bilan Montajga o'tish",
                        color = if (filledCount > 0) Color(0xFF001F26) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
