package com.personal.fittrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = OrangeAccent,
    tertiary = BlueAccent,
    error = ErrorRed,
    background = SurfaceLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EDDB),
    onPrimaryContainer = Color(0xFF173D24),
    secondaryContainer = Color(0xFFFCE6CF),
    onSecondaryContainer = Color(0xFF492A0D),
    surfaceContainer = Color(0xFFEDF2EB),
    surfaceContainerLow = Color(0xFFF3F6F0),
    surfaceContainerHighest = Color(0xFFE3EBE0),
    onSurface = Color(0xFF1A231B),
    onSurfaceVariant = Color(0xFF465347),
    outline = Color(0xFF71806F),
    outlineVariant = Color(0xFFCED8CA)
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = OrangeAccent,
    tertiary = BlueAccent,
    error = ErrorRed,
    background = SurfaceDark,
    surface = SurfaceDark,
    onPrimary = Color(0xFF12371D),
    primaryContainer = Color(0xFF244C2C),
    onPrimaryContainer = Color(0xFFD9EDDB),
    secondaryContainer = Color(0xFF4A3521),
    onSecondaryContainer = Color(0xFFFCE6CF),
    surfaceContainer = Color(0xFF20281F),
    surfaceContainerLow = Color(0xFF191F18),
    surfaceContainerHighest = Color(0xFF303B2E),
    onSurface = Color(0xFFE4ECDF),
    onSurfaceVariant = Color(0xFFBECAB8),
    outline = Color(0xFF889480),
    outlineVariant = Color(0xFF404D3B)
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FitTrackTypography,
        content = content
    )
}
