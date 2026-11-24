package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LvlLoadingState
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated

@Composable
fun LvlLoadingOverlay(
    state: LvlLoadingState,
    onDismissError: () -> Unit
) {
    AnimatedVisibility(
        visible = state !is LvlLoadingState.Idle,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(220))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF171830),
                            StudioObsidian
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is LvlLoadingState.Loading -> {
                    val infinite = rememberInfiniteTransition(label = "lvl_loader")
                    val outerAngle by infinite.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "outer_angle"
                    )
                    val innerAngle by infinite.animateFloat(
                        initialValue = 360f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "inner_angle"
                    )
                    val pulse by infinite.animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Column(
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .testTag("lvl_loading_overlay"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(144.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(CyanElectric, CoralMagenta, Color.Transparent, CyanElectric)
                                    ),
                                    startAngle = outerAngle,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(GoldBeat, CoralMagenta, Color.Transparent, GoldBeat)
                                    ),
                                    startAngle = innerAngle,
                                    sweepAngle = 200f,
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(16.dp.toPx(), 16.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(
                                        size.width - 32.dp.toPx(),
                                        size.height - 32.dp.toPx()
                                    ),
                                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .scale(pulse)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF142A3A), Color(0xFF2D1631))
                                        )
                                    )
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(listOf(CyanElectric, CoralMagenta)),
                                        RoundedCornerShape(22.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.MovieCreation,
                                        contentDescription = null,
                                        tint = CyanElectric,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = ".LVL",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GoldBeat,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "Yuklanmoqda…",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = StudioSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(
                                1.dp,
                                CyanElectric.copy(alpha = 0.35f),
                                RoundedCornerShape(50)
                            )
                        ) {
                            Text(
                                text = state.fileName,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanElectric,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = CyanElectric,
                            trackColor = StudioSurfaceElevated
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = state.stepLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is LvlLoadingState.Error -> {
                    Surface(
                        color = StudioSurface,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Xato",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Faylni ochib bo'lmadi",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onDismissError,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Yopish", color = Color(0xFF001F26))
                            }
                        }
                    }
                }

                LvlLoadingState.Idle -> Unit
            }
        }
    }
}
