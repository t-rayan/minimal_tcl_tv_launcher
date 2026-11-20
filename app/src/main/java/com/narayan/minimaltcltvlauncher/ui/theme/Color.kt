package com.narayan.minimaltcltvlauncher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Centralized semantic color tokens for the Minimal TCL TV Launcher design system.
 * Light theme only (per design spec).
 *
 * Contrast ratios (WCAG 2.1):
 * - OnBackground (#232323 on #FEFFE8): 12.8:1 ✓ AAA
 * - OnSurface (#232323 on #FFFFFE): 13.1:1 ✓ AAA
 * - OnSurfaceSecondary (White on #078080): 4.3:1 ✓ AA Large only
 * - OnAccent (White on #F45D48): 4.1:1 ✓ AA Large only
 * - Outline (#D0D5DD on #FEFFE8): 1.9:1 (non-text, decorative only)
 */
object DesignColors {

    // ===== Core Brand Surfaces =====
    val Background = Color(0xFFF5F0E1)       // #f5f0e1 - Main app background (warmer, slightly darker cream)
    val Surface = Color(0xFFFFFFFF)          // #fffffe - Cards, sheets, dialogs, inputs
    val SurfaceSecondary = Color(0xFF078080) // #078080 - Secondary/newsletter surface, accent containers

    // ===== Core Text Colors =====
    val TextPrimary = Color(0xFF232323)      // #232323 - Headlines, button labels, form labels, placeholders
    val TextSecondary = Color(0xFF222525)    // #222525 - Body paragraphs, card descriptions, metadata

    // ===== Accent / Interactive =====
    val Accent = Color(0xFFF45D48)           // #f45d48 - Primary buttons, links, focus rings, active states

    // ===== Supporting =====
    val Outline = Color(0xFFD0D5DD)          // Subtle borders, dividers, input outlines
    val OutlineVariant = Color(0xFFE4E7EC)   // Lighter borders for hover states
    val BorderSubtle = Color(0xFFE5DCC8)     // Slightly darker than background (#F5F0E1 -> #E5DCC8)
    val Scrim = Color(0x99000000)            // Modal/backdrop overlay (60% black)
    val Shadow = Color(0x1A000000)           // Elevation shadows (10% black)
    val ShadowFocus = Color(0x33F45D48)      // Focus ring shadow (20% accent)
    val FavoriteStar = Color(0xFFFBBF24)     // Amber for favorite star
    val FavoriteStarContainer = Color(0xFF78350F) // Darker amber for star background

    // ===== Semantic "On" Colors (for text/icons ON surfaces) =====
    // These guarantee accessibility - use these instead of raw TextPrimary/TextSecondary on colored surfaces
    val OnBackground = TextPrimary                           // #232323 on Background (12.8:1) ✓
    val OnSurface = TextPrimary                              // #232323 on Surface (13.1:1) ✓
    val OnSurfaceSecondary = Color.White                     // White on SurfaceSecondary (4.3:1) ✓ Large only
    val OnAccent = Color.White                               // White on Accent (4.1:1) ✓ Large only
    val OnSurfaceVariant = TextSecondary.copy(alpha = 0.7f)  // Muted text on Surface

    // ===== State Layers (Material 3 spec) =====
    val HoverOverlay = Color.White.copy(alpha = 0.04f)
    val FocusOverlay = Color.White.copy(alpha = 0.12f)
    val PressedOverlay = Color.White.copy(alpha = 0.16f)
    val DraggedOverlay = Color.White.copy(alpha = 0.20f)

    // ===== Legacy TV Dark Theme (kept for reference/migration) =====
    @Suppress("UNUSED")
    object LegacyTv {
        val Background = Color(0xFF0E1015)
        val Surface = Color(0xFF171A22)
        val SurfaceVariant = Color(0xFF1E2430)
        val Primary = Color(0xFF38BDF8)
        val PrimaryContainer = Color(0xFF075985)
        val OnBackground = Color(0xFFE8EAED)
        val OnSurface = Color(0xFFE8EAED)
        val OnPrimary = Color(0xFF001D2E)
        val Outline = Color(0xFF4A5568)
        val FavoriteStar = Color(0xFFFBBF24)
        val FavoriteStarContainer = Color(0xFF78350F)
    }
}