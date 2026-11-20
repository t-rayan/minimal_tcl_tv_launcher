package com.narayan.minimaltcltvlauncher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * Main theme entry point for the Minimal TCL TV Launcher.
 * Forces light theme per new design system specification.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MinimalTCLTVLauncherTheme(
    // Force light theme - new design system is light-only
    isInDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = lightColorScheme(
        // Primary brand colors
        primary = DesignColors.Accent,
        onPrimary = DesignColors.OnAccent,
        primaryContainer = DesignColors.Accent.copy(alpha = 0.12f),
        onPrimaryContainer = DesignColors.Accent,

        // Secondary brand colors
        secondary = DesignColors.SurfaceSecondary,
        onSecondary = DesignColors.OnSurfaceSecondary,
        secondaryContainer = DesignColors.SurfaceSecondary.copy(alpha = 0.12f),
        onSecondaryContainer = DesignColors.SurfaceSecondary,

        // Tertiary
        tertiary = DesignColors.Accent.copy(alpha = 0.8f),
        onTertiary = DesignColors.OnAccent,
        tertiaryContainer = DesignColors.Accent.copy(alpha = 0.16f),
        onTertiaryContainer = DesignColors.Accent,

        // Background & Surface
        background = DesignColors.Background,
        onBackground = DesignColors.OnBackground,
        surface = DesignColors.Surface,
        onSurface = DesignColors.OnSurface,

        // Error
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),

        // Surface variants
        surfaceVariant = DesignColors.Surface.copy(alpha = 0.8f),
        onSurfaceVariant = DesignColors.OnSurfaceVariant,

        // Scrim
        scrim = DesignColors.Scrim,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesignTypography.Material3Typography,
        shapes = DesignShapes.Material3Shapes,
        content = content
    )
}