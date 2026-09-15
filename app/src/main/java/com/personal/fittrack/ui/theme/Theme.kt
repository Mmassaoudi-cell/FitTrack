package com.personal.fittrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = OrangeAccent,
    tertiary = BlueAccent,
    error = ErrorRed,
    background = SurfaceLight,
    surface = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = OrangeAccent,
    tertiary = BlueAccent,
    error = ErrorRed,
    background = SurfaceDark,
    surface = SurfaceDark
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
