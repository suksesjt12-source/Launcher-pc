package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StormRevColorScheme = darkColorScheme(
    primary = ChakraOrange,
    onPrimary = OnChakraOrange,
    primaryContainer = ChakraOrangeContainer,
    onPrimaryContainer = OnChakraOrangeContainer,
    secondary = RasenganCyan,
    onSecondary = OnRasenganCyan,
    secondaryContainer = RasenganCyanContainer,
    onSecondaryContainer = OnRasenganCyanContainer,
    tertiary = SharinganCrimson,
    onTertiary = OnSharingan,
    tertiaryContainer = SharinganContainer,
    onTertiaryContainer = OnSharinganContainer,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = SharinganCrimson,
    onError = OnSharingan,
    errorContainer = SharinganContainer,
    onErrorContainer = OnSharinganContainer
)

private val StormRevShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StormRevTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StormRevColorScheme,
        typography = AppTypography,
        shapes = StormRevShapes,
        content = content
    )
}
