package com.narayan.minimaltcltvlauncher.ui.home

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.narayan.minimaltcltvlauncher.model.AppInfo
import com.narayan.minimaltcltvlauncher.ui.theme.FavoriteStar
import com.narayan.minimaltcltvlauncher.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private const val TAG = "MinimalTvHomeScreen"

private data class HomeSlotSpec(
    val displayLabel: String,
    val defaultIcon: ImageVector,
    val exactPackages: List<String> = emptyList(),
    val packageHints: List<String> = emptyList(),
    val labelHints: List<String> = emptyList(),
    val fallbackIntent: Intent? = null
)

private data class ResolvedHomeSlot(
    val appInfo: AppInfo,
    val candidatePackages: List<String>,
    val defaultIcon: ImageVector
)

private val HOME_SIX_SLOTS = listOf(
    HomeSlotSpec(
        displayLabel = "YouTube",
        defaultIcon = Icons.Filled.PlayArrow,
        exactPackages = listOf("com.google.android.youtube.tv"),
        packageHints = listOf("com.google.android.youtube.tv", "com.google.android.youtube"),
        labelHints = listOf("youtube")
    ),
    HomeSlotSpec(
        displayLabel = "Amazon Prime",
        defaultIcon = Icons.Filled.PlayArrow,
        exactPackages = listOf("com.amazon.amazonvideo.livingroom"),
        packageHints = listOf("com.amazon.amazonvideo.livingroom", "amazonvideo", "primevideo"),
        labelHints = listOf("prime video", "amazon prime", "prime")
    ),
    HomeSlotSpec(
        displayLabel = "Settings",
        defaultIcon = Icons.Filled.Settings,
        exactPackages = listOf("com.android.tv.settings", "com.android.settings"),
        packageHints = listOf("com.android.tv.settings", "com.android.settings"),
        labelHints = listOf("settings"),
        fallbackIntent = Intent(Settings.ACTION_SETTINGS)
    ),
    HomeSlotSpec(
        displayLabel = "Play Store",
        defaultIcon = Icons.Filled.Menu,
        exactPackages = listOf("com.android.vending"),
        packageHints = listOf("com.android.vending", "finsky"),
        labelHints = listOf("play store", "google play")
    ),
    HomeSlotSpec(
        displayLabel = "T-solo",
        defaultIcon = Icons.Filled.Menu,
        packageHints = listOf("t-solo", "tsolo", "t_solo"),
        labelHints = listOf("t-solo", "tsolo", "t solo")
    ),
    HomeSlotSpec(
        displayLabel = "NetMirror",
        defaultIcon = Icons.Filled.PlayArrow,
        exactPackages = listOf("app.netmirror.newtv"),
        packageHints = listOf("app.netmirror.newtv", "netmirror", "mirror"),
        labelHints = listOf("netmirror", "net mirror", "mirror")
    )
)

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

    var currentTime by remember { mutableStateOf(formatTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    val homeSlots = remember(apps) {
        resolveSixHomeSlots(context, apps)
    }

    val backgroundColor = Color(0xFF0B0D11)

    val wallpaperBitmap = remember {
        loadWallpaperBitmap(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990B0D11))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            CenteredSixAppRow(
                slots = homeSlots,
                viewModel = viewModel,
                favorites = favorites,
                context = context
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .align(Alignment.BottomCenter)
        ) {
            PanelFooter(
                currentTime = currentTime
            )
        }
    }
}

private fun buildLaunchIntent(context: Context, packageName: String, fallback: Intent?): Intent {
    val pm = context.packageManager
    return pm.getLeanbackLaunchIntentForPackage(packageName)
        ?: pm.getLaunchIntentForPackage(packageName)
        ?: fallback
        ?: Intent(Intent.ACTION_MAIN).setPackage(packageName)
}

private fun resolveSixHomeSlots(context: Context, installedApps: List<AppInfo>): List<ResolvedHomeSlot> {
    val usedPackages = mutableSetOf<String>()

    return HOME_SIX_SLOTS.mapIndexed { index, slot ->
        val exactMatch = slot.exactPackages.firstNotNullOfOrNull { exactPkg ->
            installedApps.firstOrNull { app ->
                app.packageName !in usedPackages &&
                    app.packageName.equals(exactPkg, ignoreCase = true)
            }
        }

        val hintMatch = exactMatch ?: installedApps.firstOrNull { app ->
            if (app.packageName in usedPackages) return@firstOrNull false
            val pkgLower = app.packageName.lowercase(Locale.ROOT)
            val labelLower = app.label.lowercase(Locale.ROOT)
            val pkgMatch = slot.packageHints.any { hint ->
                pkgLower.contains(hint.trim().lowercase(Locale.ROOT))
            }
            val labelMatch = slot.labelHints.any { hint ->
                labelLower.contains(hint.lowercase(Locale.ROOT))
            }
            pkgMatch || labelMatch
        }

        if (hintMatch != null) {
            usedPackages.add(hintMatch.packageName)
            val candidates = (listOf(hintMatch.packageName) + slot.exactPackages).distinct()
            ResolvedHomeSlot(
                appInfo = hintMatch,
                candidatePackages = candidates,
                defaultIcon = slot.defaultIcon
            )
        } else {
            val primaryPkg = slot.exactPackages.firstOrNull()
            val launchIntent = if (primaryPkg != null) {
                buildLaunchIntent(context, primaryPkg, slot.fallbackIntent)
            } else {
                slot.fallbackIntent ?: Intent(Intent.ACTION_MAIN)
            }
            val fallbackPkg = primaryPkg
                ?: "slot.placeholder.$index.${slot.displayLabel.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "")}"
            ResolvedHomeSlot(
                appInfo = AppInfo(
                    packageName = fallbackPkg,
                    label = slot.displayLabel,
                    launchIntent = launchIntent
                ),
                candidatePackages = slot.exactPackages,
                defaultIcon = slot.defaultIcon
            )
        }
    }
}

private fun loadAppDrawableFromPackages(
    context: Context,
    app: AppInfo,
    candidatePackages: List<String>
): Drawable? {
    val pm = context.packageManager
    val pkgsToTry = (
        candidatePackages +
            if (!app.packageName.startsWith("slot.placeholder.")) listOf(app.packageName) else emptyList()
        ).distinct()

    for (pkg in pkgsToTry) {
        try {
            val leanbackIntent = pm.getLeanbackLaunchIntentForPackage(pkg)
            if (leanbackIntent != null) {
                val component: ComponentName? = leanbackIntent.component
                if (component != null) {
                    try {
                        pm.getActivityIcon(component)?.let { return it }
                    } catch (_: Exception) {
                    }
                    try {
                        pm.getActivityBanner(component)?.let { return it }
                    } catch (_: Exception) {
                    }
                }
                val resolveInfo = pm.resolveActivity(leanbackIntent, 0)
                resolveInfo?.loadIcon(pm)?.let { return it }
            }
        } catch (_: Exception) {
        }

        try {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                val component: ComponentName? = launchIntent.component
                if (component != null) {
                    try {
                        pm.getActivityIcon(component)?.let { return it }
                    } catch (_: Exception) {
                    }
                    try {
                        pm.getActivityBanner(component)?.let { return it }
                    } catch (_: Exception) {
                    }
                }
                val resolveInfo = pm.resolveActivity(launchIntent, 0)
                resolveInfo?.loadIcon(pm)?.let { return it }
            }
        } catch (_: Exception) {
        }

        try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            if (appInfo.icon != 0) {
                try {
                    val appRes = pm.getResourcesForApplication(appInfo)
                    appRes.getDrawable(appInfo.icon, null)?.let { return it }
                } catch (_: Exception) {
                }
            }
            if (appInfo.banner != 0) {
                try {
                    val appRes = pm.getResourcesForApplication(appInfo)
                    appRes.getDrawable(appInfo.banner, null)?.let { return it }
                } catch (_: Exception) {
                }
            }
            try {
                pm.getApplicationIcon(pkg)?.let { return it }
            } catch (_: Exception) {
            }
            try {
                pm.getApplicationBanner(pkg)?.let { return it }
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }

    if (app.launchIntent.action == Settings.ACTION_SETTINGS) {
        for (pkg in listOf("com.android.tv.settings", "com.android.settings")) {
            try {
                pm.getApplicationIcon(pkg)?.let { return it }
            } catch (_: Exception) {
            }
        }
    }

    return null
}

fun loadWallpaperBitmap(context: Context): ImageBitmap? {
    return try {
        val resources = context.resources
        val candidates = listOf("wallpaper", "tv_wallpaper", "background")
        val drawableId = candidates
            .map { name -> resources.getIdentifier(name, "drawable", context.packageName) }
            .firstOrNull { id -> id != 0 } ?: 0

        if (drawableId != 0) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(resources, drawableId, options)

            val targetWidth = 640
            val targetHeight = 360
            var sampleSize = 1
            while (options.outWidth / sampleSize > targetWidth || options.outHeight / sampleSize > targetHeight) {
                sampleSize *= 2
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            BitmapFactory.decodeResource(resources, drawableId, options)?.asImageBitmap()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CenteredSixAppRow(
    slots: List<ResolvedHomeSlot>,
    viewModel: HomeViewModel,
    favorites: Set<String>,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEach { slot ->
            AppCircleTile(
                app = slot.appInfo,
                candidatePackages = slot.candidatePackages,
                defaultIcon = slot.defaultIcon,
                viewModel = viewModel,
                isFavorite = favorites.contains(slot.appInfo.packageName),
                context = context
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppCircleTile(
    app: AppInfo,
    candidatePackages: List<String>,
    defaultIcon: ImageVector,
    viewModel: HomeViewModel,
    isFavorite: Boolean,
    context: Context
) {
    var iconBitmap by remember(app.packageName, candidatePackages) { mutableStateOf<ImageBitmap?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(app.packageName, candidatePackages) {
        val directDrawable = loadAppDrawableFromPackages(context, app, candidatePackages)
        val directBitmap = directDrawable?.let { drawableToBitmap(it, 160, 160) }
        val vmBitmap = if (directBitmap == null) {
            viewModel.loadAppIconBitmap(app.packageName, candidatePackages, 160)
        } else null

        val finalBitmap = directBitmap ?: vmBitmap
        if (finalBitmap != null) {
            iconBitmap = finalBitmap.asImageBitmap()
        } else {
            Log.d(TAG, "No bitmap for label=${app.label}, pkg=${app.packageName}, candidates=$candidatePackages")
        }
    }

    val baseIconSize = 62.dp
    val focusedIconSize by animateDpAsState(
        targetValue = if (isFocused) 74.dp else baseIconSize,
        label = "iconSize"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 2.dp,
        label = "shadowElevation"
    )

    Column(
        modifier = Modifier
            .background(Color.Transparent)
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        context.startActivity(
                            app.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                },
                onLongClick = {
                    if (!app.packageName.startsWith("slot.placeholder.")) {
                        viewModel.toggleFavorite(app.packageName)
                    }
                }
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(focusedIconSize)
                        .shadow(
                            elevation = shadowElevation,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = iconBitmap!!,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(focusedIconSize)
                        .shadow(
                            elevation = shadowElevation,
                            shape = CircleShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = app.label,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        if (isFavorite) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(FavoriteStar, CircleShape)
            )
        }

        Box(
            modifier = Modifier.height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFocused) {
                Text(
                    text = app.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Transparent)
                        .padding(horizontal = 2.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PanelFooter(
    currentTime: String
) {
    val userName = "User"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentTime,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.background(Color.Transparent)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(Color.Transparent)
            ) {
                Text(
                    text = userName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.background(Color.Transparent)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
    return try {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: targetWidth
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: targetHeight
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = min(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
        val drawWidth = (width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (height * scale).toInt().coerceAtLeast(1)
        val left = (targetWidth - drawWidth) / 2
        val top = (targetHeight - drawHeight) / 2
        val copy = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
        copy.setBounds(left, top, left + drawWidth, top + drawHeight)
        copy.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}

fun formatTime(): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
}

fun formatDate(): String {
    return SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
}
