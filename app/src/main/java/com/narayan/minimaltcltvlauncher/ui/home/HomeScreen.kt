package com.narayan.minimaltcltvlauncher.ui.home

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.narayan.minimaltcltvlauncher.model.AppInfo
import com.narayan.minimaltcltvlauncher.ui.theme.FavoriteStar
import com.narayan.minimaltcltvlauncher.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    // Clock state - updates once per minute
    var currentTime by remember { mutableStateOf(formatTime()) }
    var currentDate by remember { mutableStateOf(formatDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime()
            currentDate = formatDate()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    // Split apps into favorites and remaining apps
    val (favoriteApps, otherApps) = remember(apps, favorites) {
        Pair(
            apps.filter { favorites.contains(it.packageName) },
            apps.filter { !favorites.contains(it.packageName) }
        )
    }

    // View state: true = Pinned, false = All Apps
    var showPinned by remember { mutableStateOf(favoriteApps.isNotEmpty()) }

    // Current app list based on view mode
    val currentApps = if (showPinned) favoriteApps else otherApps

    // Deep dark background colors
    val backgroundColor = Color(0xFF0B0D11)
    val dividerColor = Color.White.copy(alpha = 0.08f)
    val footerBgColor = Color(0xFF14171D)
    val focusBgColor = Color.White.copy(alpha = 0.12f)
    val focusBorderColor = Color.White.copy(alpha = 0.25f)

    // Load low-res wallpaper bitmap (downsampled to ~640x360 for memory efficiency)
    val wallpaperBitmap by remember {
        mutableStateOf<ImageBitmap?>(loadWallpaperBitmap(context))
    }

    // Main layout: full-screen background with wallpaper + left nav rail + centered transparent container
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen wallpaper layer with soft ambient look
        if (wallpaperBitmap != null) {
            // Use dark atmospheric scrim overlay for soft "blurred" look on all API levels
            // This avoids memory-heavy blur operations and works reliably on Android 11+
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    bitmap = wallpaperBitmap!!,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark scrim overlay for soft "blurred" atmospheric look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x990B0D11)) // Dark scrim overlay (60% opacity)
                )
            }
        } else {
            // Fallback: Deep dark atmospheric gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )
        }

        // Left floating pill navigation rail
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 24.dp, top = 24.dp, bottom = 24.dp)
                .align(Alignment.CenterStart)
        ) {
            Surface(
                color = Color(0xCC161920),
                border = androidx.compose.ui.graphics.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                NavigationRail(
                    showPinned = showPinned,
                    onHomeClick = { showPinned = true },
                    onAllAppsClick = { showPinned = false },
                    onConnectedDevicesClick = {
                        try {
                            context.startActivity(
                                Intent("android.settings.INPUT_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_CAST_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                    // Ignore all failures
                                }
                            }
                        }
                    },
                    onSettingsClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            // Ignore
                        }
                    }
                )
            }
        }

        // Centered transparent container (no background, no border)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.64f)
                .fillMaxHeight(0.86f)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Main grid area (no top header or divider)
                AppGrid(
                    apps = currentApps,
                    viewModel = viewModel,
                    context = context,
                    isPinnedView = showPinned,
                    focusBgColor = focusBgColor,
                    focusBorderColor = focusBorderColor
                )

                // Divider before footer
                Divider(
                    color = dividerColor,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Bottom footer: clock/date + settings
                PanelFooter(
                    currentTime = currentTime,
                    currentDate = currentDate,
                    onSettingsClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            // Ignore if settings intent cannot be resolved
                        }
                    },
                    footerBgColor = footerBgColor,
                    focusBgColor = focusBgColor,
                    focusBorderColor = focusBorderColor
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NavigationRail(
    showPinned: Boolean,
    onHomeClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onConnectedDevicesClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val railFocusedIndex by remember { mutableStateOf(0) }
    val railItems = listOf(
        NavigationRailItem(
            icon = Icons.Filled.Home,
            contentDescription = "Home",
            action = onHomeClick
        ),
        NavigationRailItem(
            icon = Icons.Outlined.ViewModule,
            contentDescription = "All Apps",
            action = onAllAppsClick
        ),
        NavigationRailItem(
            icon = Icons.Outlined.Cast,
            contentDescription = "Connected Devices",
            action = onConnectedDevicesClick
        ),
        NavigationRailItem(
            icon = Icons.Filled.Settings,
            contentDescription = "Settings",
            action = onSettingsClick
        )
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        railItems.forEachIndexed { index, item ->
            var isFocused by remember { mutableStateOf(false) }
            val focusScale by animateFloatAsState(
                targetValue = if (isFocused) 1.08f else 1f,
                animationSpec = tween(120)
            )
            val focusAlpha by animateFloatAsState(
                targetValue = if (isFocused) 1f else 0.7f,
                animationSpec = tween(120)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .graphicsLayer { scaleX = focusScale; scaleY = focusScale }
                    .combinedClickable(
                        onClick = item.action,
                        onLongClick = {}
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.contentDescription,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White.copy(alpha = focusAlpha)
                )
            }
        }
    }
}

private data class NavigationRailItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val action: () -> Unit
)

// Helper function to load and downsample wallpaper bitmap for low memory usage
private fun loadWallpaperBitmap(context: Context): ImageBitmap? {
    return try {
        // Try to load a local wallpaper drawable (res/drawable/wallpaper.jpg or similar)
        // If none exists, this will return null and we'll use the fallback gradient
        val resources = context.resources
        // Check for common wallpaper resource names
        val drawableId = resources.getIdentifier("wallpaper", "drawable", context.packageName)
            ?: resources.getIdentifier("tv_wallpaper", "drawable", context.packageName)
            ?: resources.getIdentifier("background", "drawable", context.packageName)
            ?: 0

        if (drawableId != 0) {
            // Decode with downsampling to max 640x360 (or 960x540) to keep memory low
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(resources, drawableId, options)

            // Calculate sample size for ~640x360 max
            val targetWidth = 640
            val targetHeight = 360
            var sampleSize = 1
            while (options.outWidth / sampleSize > targetWidth || options.outHeight / sampleSize > targetHeight) {
                sampleSize *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val bitmap = BitmapFactory.decodeResource(resources, drawableId, options)
            bitmap?.asImageBitmap()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    viewModel: HomeViewModel,
    context: Context,
    isPinnedView: Boolean,
    focusBgColor: Color,
    focusBorderColor: Color
) {
    if (apps.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp), // More padding
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp) // Larger
                )
                Text(
                    text = if (isPinnedView) "No pinned apps yet" else "No apps found",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                if (isPinnedView) {
                    Text(
                        text = "Long-press any app in All Apps to pin it",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp), // Adjusted for narrower panel
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppTile(
                app = app,
                viewModel = viewModel,
                isFavorite = viewModel.isFavorite(app.packageName),
                context = context,
                focusBgColor = focusBgColor,
                focusBorderColor = focusBorderColor
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppInfo,
    viewModel: HomeViewModel,
    isFavorite: Boolean,
    context: Context,
    focusBgColor: Color,
    focusBorderColor: Color
) {
    var iconBitmap by remember(app.packageName) { mutableStateOf<ImageBitmap?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(app.packageName) {
        val iconDrawable = viewModel.loadIcon(app.packageName)
        if (iconDrawable != null) {
            // Downsample to 96x96 max for icon tile - keeps Native Heap low
            iconBitmap = drawableToBitmap(iconDrawable, 96, 96)?.asImageBitmap()
        }
    }

    val tileShape = CircleShape // Changed to CircleShape for circular icons
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = tween(150)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(if (isFocused) focusBgColor else Color.Transparent, tileShape)
            .border(
                width = if (isFocused) 1.dp else 0.dp,
                color = if (isFocused) focusBorderColor else Color.Transparent,
                shape = tileShape
            )
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                onClick = {
                    try {
                        context.startActivity(app.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                        // Ignore launch failures
                    }
                },
                onLongClick = { viewModel.toggleFavorite(app.packageName) }
            )
            .graphicsLayer { scaleX = focusScale; scaleY = focusScale }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp), // More padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp) // More spacing
        ) {
            // App icon - 52dp, now CIRCULAR with subtle dark circular backing
            if (iconBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape), // Subtle dark circular backing
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Slightly smaller to show backing ring
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = iconBitmap!!,
                            contentDescription = app.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            } else {
                // Placeholder with same circular backing
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Favorite indicator dot
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(FavoriteStar, CircleShape)
                )
            }

            // App label
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PanelFooter(
    currentTime: String,
    currentDate: String,
    onSettingsClick: () -> Unit,
    footerBgColor: Color,
    focusBgColor: Color,
    focusBorderColor: Color
) {
    var settingsFocused by remember { mutableStateOf(false) }
    val settingsScale by animateFloatAsState(
        targetValue = if (settingsFocused) 1.06f else 1f,
        animationSpec = tween(150)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(footerBgColor, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp), // More padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Clock and date
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "$currentTime  •  $currentDate",
                    fontSize = 15.sp, // Slightly larger
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Right: Settings button
            Box(
                modifier = Modifier
                    .size(48.dp) // Slightly larger
                    .onFocusChanged { settingsFocused = it.isFocused }
                    .graphicsLayer { scaleX = settingsScale; scaleY = settingsScale }
                    .background(if (settingsFocused) focusBgColor else Color.Transparent, RoundedCornerShape(12.dp))
                    .border(
                        width = if (settingsFocused) 1.dp else 0.dp,
                        color = if (settingsFocused) focusBorderColor else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(
                        onClick = onSettingsClick,
                        onLongClick = {}
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(26.dp), // Slightly larger
                        tint = if (settingsFocused) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
    return when (drawable) {
        is BitmapDrawable -> {
            val src = drawable.bitmap ?: return null
            if (src.width <= targetWidth && src.height <= targetHeight) {
                src
            } else {
                Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
            }
        }
        else -> {
            val width = drawable.intrinsicWidth
            val height = drawable.intrinsicHeight
            if (width <= 0 || height <= 0) return null
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = min(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
            val drawWidth = (width * scale).toInt()
            val drawHeight = (height * scale).toInt()
            val left = (targetWidth - drawWidth) / 2
            val top = (targetHeight - drawHeight) / 2
            drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
            drawable.draw(canvas)
            bitmap
        }
    }
}

private fun formatTime(): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
}

private fun formatDate(): String {
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
}