package com.narayan.minimaltcltvlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.tv.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Centralized corner radius tokens for the design system.
 * Maps to TV Material3 Shapes for theming consistency.
 */
object DesignShapes {

    // ===== Radius Tokens (CornerSize) =====
    val ExtraSmall = RoundedCornerShape(4.dp)    // Chips, badges, small pills
    val Small = RoundedCornerShape(8.dp)         // Buttons, input fields, list items
    val Medium = RoundedCornerShape(12.dp)       // Cards, dialogs, sheets
    val Large = RoundedCornerShape(16.dp)        // Large cards, bottom sheets, drawers
    val ExtraLarge = RoundedCornerShape(24.dp)   // Full-screen modals, major containers
    val Full = RoundedCornerShape(999.dp)        // Pills, circular avatars, FABs

    // ===== Semantic Shape Assignments =====
    val Card = Medium          // Standard cards
    val CardLarge = Large      // Featured/large cards
    val Button = Full          // All buttons (pill)
    val InputField = Small     // Text fields, search bars
    val Chip = ExtraSmall      // Chips, tags, badges
    val Drawer = ExtraLarge    // Navigation drawers, side panels
    val Dialog = Large         // Dialogs, alerts, bottom sheets
    val Tooltip = ExtraSmall   // Tooltips, popovers
    val Avatar = Full          // Circular avatars, icons

    // ===== TV Material3 Shapes Mapping =====
    val Material3Shapes = Shapes(
        small = Small,
        medium = Medium,
        large = Large
    )
}