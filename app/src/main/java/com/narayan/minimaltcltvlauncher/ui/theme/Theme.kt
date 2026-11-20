package com.narayan.minimaltcltvlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MinimalTCLTVLauncherTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // TV launchers should always use dark theme
    val colorScheme = darkColorScheme(
        primary = Primary,
        primaryContainer = PrimaryContainer,
        onPrimary = OnPrimary,
        secondary = SurfaceVariant,
        onSecondary = OnSurface,
        tertiary = FavoriteStar,
        onTertiary = Color.Black,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurface.copy(alpha = 0.7f),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}