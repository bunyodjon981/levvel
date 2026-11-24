package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LavelleDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = Color(0xFF001F26),
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanSoft,
    secondary = CoralMagenta,
    onSecondary = Color(0xFF2E0014),
    secondaryContainer = CoralContainer,
    onSecondaryContainer = Color(0xFFFFD8E4),
    tertiary = GoldBeat,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Color(0xFFFFE0B2),
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Color(0xFF07070D),
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceVariant,
    surfaceContainerHigh = StudioSurfaceElevated,
    outline = StudioOutline,
    outlineVariant = StudioOutlineSubtle,
    error = ErrorCrimson,
    onError = Color.White
)

val LavelleShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LavelleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LavelleDarkColorScheme,
        typography = LavelleTypography,
        shapes = LavelleShapes,
        content = content
    )
}
