package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ChronoPrimary,
    onPrimary = ChronoOnPrimary,
    primaryContainer = AccentBlue,
    background = ChronoBackground,
    onBackground = ChronoText,
    surface = ChronoSurface,
    onSurface = ChronoText,
    surfaceVariant = ChronoSurfaceElevated,
    onSurfaceVariant = ChronoTextDim,
    error = QuadrantRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
