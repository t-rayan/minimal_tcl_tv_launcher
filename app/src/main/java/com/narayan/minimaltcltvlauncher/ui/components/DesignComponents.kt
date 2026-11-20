package com.narayan.minimaltcltvlauncher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narayan.minimaltcltvlauncher.ui.theme.DesignColors
import com.narayan.minimaltcltvlauncher.ui.theme.DesignShapes
import com.narayan.minimaltcltvlauncher.ui.theme.DesignTypography

// ========================================================================
// COLOR ACCESS HELPER
// ========================================================================

@Composable
internal fun rememberDesignColors(): DesignColors = DesignColors

// ========================================================================
// SURFACES
// ========================================================================

/**
 * Standard card surface with elevation and consistent styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    elevation: Int = 1,
    content: @Composable () -> Unit
) {
    val colors = rememberDesignColors()
    val shadowElevation = when (elevation) {
        0 -> 0.dp
        1 -> 2.dp
        2 -> 4.dp
        3 -> 8.dp
        4 -> 16.dp
        else -> 2.dp
    }

    Surface(
        modifier = modifier
            .border(width = 1.dp, color = colors.OutlineVariant, shape = DesignShapes.Card),
        shape = DesignShapes.Card,
        color = colors.Surface,
        shadowElevation = shadowElevation
    ) {
        content()
    }
}

/**
 * Secondary surface for newsletter/featured sections.
 * Text/content inside should use light colors (White).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsSurfaceSecondary(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = rememberDesignColors()

    Surface(
        modifier = modifier,
        shape = DesignShapes.Card,
        color = colors.SurfaceSecondary
    ) {
        content()
    }
}

/**
 * Outlined card variant for less emphasis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsCardOutlined(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = rememberDesignColors()

    Surface(
        modifier = modifier
            .border(width = 1.dp, color = colors.Outline, shape = DesignShapes.Card),
        shape = DesignShapes.Card,
        color = colors.Surface
    ) {
        content()
    }
}

// ========================================================================
// BUTTONS
// ========================================================================

/**
 * Primary filled button - main call to action.
 * Uses Accent background with White text (large text only for AA).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    val colors = rememberDesignColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DesignShapes.Button)
            .focusable()
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 24.dp)
            .background(if (enabled) colors.Accent else colors.Accent.copy(alpha = 0.4f), DesignShapes.Button)
            .shadow(1.dp, DesignShapes.Button, ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("...", style = DesignTypography.LabelLarge, color = colors.OnAccent)
                }
            } else {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = colors.OnAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = text,
                    style = DesignTypography.LabelLarge,
                    color = colors.OnAccent,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Secondary outlined button - alternative actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val colors = rememberDesignColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(DesignShapes.Button)
            .focusable()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 24.dp)
            .background(if (enabled) colors.Surface else colors.Surface.copy(alpha = 0.6f), DesignShapes.Button)
            .border(width = 1.5.dp, color = if (enabled) colors.Accent else colors.Outline, shape = DesignShapes.Button)
            .shadow(1.dp, DesignShapes.Button, ambientColor = Color.Black.copy(alpha = 0.1f), spotColor = Color.Black.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (enabled) colors.Accent else colors.Outline,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = DesignTypography.LabelLarge,
                color = if (enabled) colors.Accent else colors.Outline,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Text button - tertiary/low emphasis actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = rememberDesignColors()

    Box(
        modifier = modifier
            .clip(DesignShapes.Button)
            .focusable()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
            .background(Color.Transparent, DesignShapes.Button)
    ) {
        Text(
            text = text,
            style = DesignTypography.LabelLarge,
            color = if (enabled) colors.Accent else colors.Outline,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ========================================================================
// TEXT COMPONENTS
// ========================================================================

/**
 * Themed text component with semantic color defaults.
 */
@Composable
fun DsText(
    text: String,
    style: androidx.compose.ui.text.TextStyle = DesignTypography.BodyMedium,
    color: Color = DesignColors.OnSurface,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign
    )
}

/**
 * Headline text for section titles.
 */
@Composable
fun DsHeadline(
    text: String,
    style: androidx.compose.ui.text.TextStyle = DesignTypography.HeadlineMedium,
    color: Color = DesignColors.OnSurface,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

/**
 * Caption/metadata text.
 */
@Composable
fun DsCaption(
    text: String,
    style: androidx.compose.ui.text.TextStyle = DesignTypography.LabelSmall,
    color: Color = DesignColors.OnSurfaceVariant,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}

// ========================================================================
// DIVIDER
// ========================================================================

@Composable
fun DsDivider(
    modifier: Modifier = Modifier.fillMaxWidth(),
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    color: Color = DesignColors.OutlineVariant
) {
    Box(
        modifier = modifier
            .height(thickness)
            .background(color)
    )
}

// ========================================================================
// CHIP / TAG
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    dismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val colors = rememberDesignColors()
    val chipColor = if (selected) colors.Accent.copy(alpha = 0.12f) else colors.Surface.copy(alpha = 0.8f)
    val borderColor = if (selected) colors.Accent else colors.Outline
    val contentColor = if (selected) colors.Accent else colors.OnSurfaceVariant

    Box(
        modifier = modifier
            .clip(DesignShapes.Chip)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(
                if (onClick != null) Modifier
                    .focusable()
                    .clickable(onClick = onClick)
                else Modifier
            )
            .background(chipColor, DesignShapes.Chip)
            .border(width = 1.dp, color = borderColor, shape = DesignShapes.Chip)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = DesignTypography.LabelSmall,
                color = contentColor
            )
            if (dismissible) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = contentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onDismiss!!)
                        .padding(start = 4.dp)
                )
            }
        }
    }
}

// ========================================================================
// AVATAR / ICON CONTAINER
// ========================================================================

@Composable
fun DsAvatar(
    modifier: Modifier = Modifier.size(40.dp),
    imageVector: ImageVector? = null,
    text: String? = null,
    backgroundColor: Color = DesignColors.Accent.copy(alpha = 0.12f),
    contentColor: Color = DesignColors.Accent
) {
    val colors = rememberDesignColors()
    val size = 40.dp

    Box(
        modifier = modifier
            .clip(DesignShapes.Avatar)
            .background(backgroundColor)
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        imageVector?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(size * 0.5f)
            )
        }
        text?.let {
            Text(
                text = it,
                style = DesignTypography.TitleLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

// ========================================================================
// LIST ITEM
// ========================================================================

@Composable
fun DsListItem(
    modifier: Modifier = Modifier.fillMaxWidth(),
    leadingIcon: ImageVector? = null,
    leadingAvatar: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailingIcon: ImageVector? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true
) {
    val colors = rememberDesignColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .then(
                if (onClick != null) Modifier
                    .focusable()
                    .clickable(onClick = onClick)
                    .background(colors.Surface)
                else Modifier.background(colors.Surface)
            )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Leading
                if (leadingAvatar != null) {
                    leadingAvatar()
                } else if (leadingIcon != null) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(DesignShapes.Avatar)
                            .background(colors.Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = colors.Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = title,
                        style = DesignTypography.TitleMedium,
                        color = colors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = DesignTypography.BodySmall,
                            color = colors.OnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Trailing
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = DesignTypography.LabelMedium,
                        color = colors.OnSurfaceVariant
                    )
                }
                trailingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = colors.OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ========================================================================
// SKELETON SCREENS (Placeholder/Loading UI)
// ========================================================================

/**
 * Skeleton placeholder with shimmer animation for loading states.
 * Usage: DsSkeleton.Square(), DsSkeleton.Circle(), DsSkeleton.Rect(), DsSkeleton.AppTile()
 */
@OptIn(ExperimentalMaterial3Api::class)
object DsSkeleton {

    /**
     * Skeleton square placeholder.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Square(
        modifier: Modifier = Modifier.size(48.dp),
        cornerRadius: androidx.compose.ui.unit.Dp = 12.dp
    ) {
        val alpha = animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "skeletonPulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
        )
    }

    /**
     * Skeleton circle placeholder.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Circle(
        modifier: Modifier = Modifier.size(48.dp)
    ) {
        val alpha = animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "skeletonPulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
        )
    }

    /**
     * Skeleton rectangle placeholder.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Rect(
        modifier: Modifier = Modifier.fillMaxWidth().height(16.dp),
        cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
    ) {
        val alpha = animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "skeletonPulse"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
        )
    }

    /**
     * Skeleton for app tile (icon + label placeholder).
     * Used in app drawer grid.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppTile(
        modifier: Modifier = Modifier,
        iconSize: androidx.compose.ui.unit.Dp = 58.dp,
        iconCornerRadius: androidx.compose.ui.unit.Dp = 12.dp
    ) {
        val alpha = animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "skeletonPulse"
        )

        val baseAlpha = 0.12f + 0.08f * (1f - alpha.value)

        Column(
            modifier = modifier
                .background(Color.Transparent)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon placeholder
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
                )
            }

            // Label placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }

    /**
     * Full-width skeleton row with multiple app tiles.
     * Used for grid placeholders.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GridPlaceholder(
        modifier: Modifier = Modifier,
        columns: Int = 5,
        items: Int = 5,
        horizontalSpacing: androidx.compose.ui.unit.Dp = 12.dp,
        verticalSpacing: androidx.compose.ui.unit.Dp = 10.dp
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) {
                AppTile()
            }
        }
    }

    /**
     * List item skeleton with leading icon, title and subtitle placeholders.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ListItem(
        modifier: Modifier = Modifier.fillMaxWidth(),
        leadingIconSize: androidx.compose.ui.unit.Dp = 44.dp,
        showSubtitle: Boolean = true
    ) {
        val alpha = animateFloatAsState(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, delayMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "skeletonPulse"
        )

        val baseAlpha = 0.12f + 0.08f * (1f - alpha.value)

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Leading icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
            )

            // Content placeholders
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(DesignColors.OutlineVariant.copy(alpha = 0.12f + 0.08f * (1f - alpha.value)))
                        .clip(RoundedCornerShape(8.dp))
                )
                // Subtitle placeholder
                if (true) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .widthIn(max = 200.dp)
                            .background(DesignColors.OutlineVariant.copy(alpha = baseAlpha * 0.7f))
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}