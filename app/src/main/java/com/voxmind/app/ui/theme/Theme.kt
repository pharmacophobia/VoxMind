package com.voxmind.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldSuccess,
    onSecondary = Color.White,
    secondaryContainer = Slate800,
    onSecondaryContainer = EmeraldLight,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    error = RoseError,
    onError = Color.White,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate700
)

@Composable
fun VoxMindTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = VoxMindTypography,
        content = content
    )
}
