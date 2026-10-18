package com.sofamaven.musicrecognizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val TvPrimary = Color(0xFF6366F1) // Indigo/Violet Google TV accent
val TvOnPrimary = Color(0xFFFFFFFF)
val TvBackground = Color(0xFF0B0F17) // Deep cinematic dark blue-black
val TvSurface = Color(0xFF161F30)
val TvSurfaceVariant = Color(0xFF1E293B)
val TvAccentGold = Color(0xFFF59E0B)
val TvError = Color(0xFFEF4444)

private val TvDarkColors = darkColorScheme(
    primary = TvPrimary,
    onPrimary = TvOnPrimary,
    background = TvBackground,
    surface = TvSurface,
    surfaceVariant = TvSurfaceVariant,
    error = TvError
)

@Composable
fun AndroidTvMusicRecognizerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColors,
        content = content
    )
}
