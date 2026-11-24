package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.MontageProject
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurfaceElevated
import com.example.video.MontageBitmapCache
import com.example.video.MontageFrameRenderer
import kotlin.math.hypot

@Composable
fun MontagePreviewCanvas(
    project: MontageProject,
    playheadMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    selectedTextId: String? = null,
    selectedStickerId: String? = null,
    onSelectText: (String?) -> Unit = {},
    onSelectSticker: (String?) -> Unit = {},
    onTransformText: (id: String, xFrac: Float, yFrac: Float, scale: Float, rotationDeg: Float) -> Unit = { _, _, _, _, _ -> },
    onTransformSticker: (id: String, xFrac: Float, yFrac: Float, scale: Float, rotationDeg: Float) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val bitmapCache = remember(context) { MontageBitmapCache(context) }
    val renderer = remember(context, bitmapCache) { MontageFrameRenderer(context, bitmapCache) }
    var canvasSize by remember { mutableStateOf(IntSize(360, 640)) }

    val latestProject by rememberUpdatedState(project)
    val latestPlayhead by rememberUpdatedState(playheadMs)
    val latestSelectedTextId by rememberUpdatedState(selectedTextId)
    val latestSelectedStickerId by rememberUpdatedState(selectedStickerId)

    DisposableEffect(bitmapCache) {
        onDispose { bitmapCache.clear() }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val ratio = project.aspectRatio.ratio
        val hasActiveSelection = interactive && (selectedTextId != null || selectedStickerId != null)

        Box(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .aspectRatio(ratio, matchHeightConstraintsFirst = maxHeight * ratio <= maxWidth)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF07070D))
                .border(
                    width = if (hasActiveSelection) 1.5.dp else 1.dp,
                    color = if (hasActiveSelection) CoralMagenta else StudioOutline.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(18.dp)
                )
                .onSizeChanged { canvasSize = it }
                .pointerInput(interactive) {
                    if (!interactive) return@pointerInput
                    detectTapGestures { offset ->
                        val w = canvasSize.width.coerceAtLeast(1).toFloat()
                        val h = canvasSize.height.coerceAtLeast(1).toFloat()
                        val nx = offset.x / w
                        val ny = offset.y / h
                        val t = latestPlayhead

                        val hitSticker = latestProject.stickers
                            .lastOrNull { !it.kind.isFullFrame && t in it.startMs..it.endMs && hypot(nx - it.xFraction, ny - it.yFraction) < 0.16f }
                        if (hitSticker != null) {
                            onSelectSticker(hitSticker.id)
                            return@detectTapGestures
                        }

                        val hitText = latestProject.texts
                            .lastOrNull { t in it.startMs..it.endMs && hypot(nx - it.xFraction, ny - it.yFraction) < 0.22f }
                        if (hitText != null) {
                            onSelectText(hitText.id)
                            return@detectTapGestures
                        }

                        if (latestSelectedTextId != null || latestSelectedStickerId != null) {
                            onSelectText(null)
                            onSelectSticker(null)
                        } else {
                            onTogglePlay()
                        }
                    }
                }
                .pointerInput(interactive, selectedTextId, selectedStickerId) {
                    if (!interactive) return@pointerInput
                    if (selectedTextId == null && selectedStickerId == null) return@pointerInput
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val w = canvasSize.width.coerceAtLeast(1).toFloat()
                        val h = canvasSize.height.coerceAtLeast(1).toFloat()
                        val txtId = latestSelectedTextId
                        val stId = latestSelectedStickerId
                        if (txtId != null) {
                            val item = latestProject.texts.firstOrNull { it.id == txtId } ?: return@detectTransformGestures
                            onTransformText(
                                txtId,
                                item.xFraction + pan.x / w,
                                item.yFraction + pan.y / h,
                                item.scale * zoom,
                                item.rotationDeg + rotation
                            )
                        } else if (stId != null) {
                            val item = latestProject.stickers.firstOrNull { it.id == stId } ?: return@detectTransformGestures
                            onTransformSticker(
                                stId,
                                item.xFraction + pan.x / w,
                                item.yFraction + pan.y / h,
                                item.scale * zoom,
                                item.rotationDeg + rotation
                            )
                        }
                    }
                }
                .testTag("preview_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    renderer.drawFrame(
                        canvas = canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        project = project,
                        timeMs = playheadMs,
                        maxBitmapDim = 960
                    )
                }

                if (interactive) {
                    val selText = selectedTextId?.let { id -> project.texts.firstOrNull { it.id == id } }
                    if (selText != null && playheadMs in selText.startMs..selText.endMs) {
                        val cx = size.width * selText.xFraction
                        val cy = size.height * selText.yFraction
                        drawCircle(
                            color = CyanElectric,
                            radius = 34.dp.toPx() * selText.scale.coerceIn(0.6f, 2f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    val selSticker = selectedStickerId?.let { id -> project.stickers.firstOrNull { it.id == id } }
                    if (selSticker != null && playheadMs in selSticker.startMs..selSticker.endMs && !selSticker.kind.isFullFrame) {
                        val cx = size.width * selSticker.xFraction
                        val cy = size.height * selSticker.yFraction
                        drawCircle(
                            color = GoldBeat,
                            radius = 34.dp.toPx() * selSticker.scale.coerceIn(0.6f, 2f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            if (!isPlaying && !hasActiveSelection) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0B0B13))
                        .border(1.dp, CyanElectric.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Ijro etish",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (hasActiveSelection) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC0B0B13),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(1.dp, CoralMagenta.copy(0.7f), RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.OpenWith, contentDescription = null, tint = CoralMagenta, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Suring • Zoom • Burang",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            onSelectText(null)
                            onSelectSticker(null)
                        },
                        color = CyanElectric,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = "Tayyor", tint = Color(0xFF001F26), modifier = Modifier.size(14.dp))
                            Text(
                                text = "Tayyor",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF001F26),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = StudioSurfaceElevated.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = project.aspectRatio.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanElectric,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x880B0B13))
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = "To'xtatish",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
