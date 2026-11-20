package com.narayan.minimaltcltvlauncher.ui.home

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.LruCache
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import com.narayan.minimaltcltvlauncher.model.AppInfo
import com.narayan.minimaltcltvlauncher.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

private const val TAG = "MinimalTvHomeScreen"
private val FavoriteStar = Color(0xFFFFD54F)
private val AlmostWhiteBackground = Color(0xFFF6F7F9)
private val LightInkPrimary = Color(0xFF141820)
private val LightInkSecondary = Color(0xFF3A4150)
private val SystemSettingsAccent = Color(0xFF1E64E8)
private val SystemSettingsAccentFocused = Color(0xFF3B7CF5)

private val iconBitmapCache = object : LruCache<String, Bitmap>(128) {}

private enum class ActiveView {
    HOME,
    ALL_APPS,
    CONNECTED_DEVICES,
    SETTINGS
}

private enum class LeftMenuItem {
    HOME,
    SETTINGS,
    ALL_APPS,
    CONNECTED_DEVICES
}

private enum class PillGlyphKind {
    VECTOR,
    APP_GRID,
    DEVICE_LINK
}

private enum class DeviceIconKind {
    HEADPHONES,
    CONTROLLER,
    DISPLAY,
    SPEAKER,
    MOBILE,
    GENERIC
}

private data class ConnectedDeviceItem(
    val name: String,
    val subtitle: String,
    val address: String? = null,
    val isConnected: Boolean = false,
    val iconKind: DeviceIconKind,
    val actionIntent: Intent? = null
)

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

    var activeView by remember { mutableStateOf(ActiveView.HOME) }
    var activeMenu by remember { mutableStateOf(LeftMenuItem.HOME) }
    var isDarkTheme by remember { mutableStateOf(false) }

    val centerRowFocusRequester = remember { FocusRequester() }
    val allAppsFirstFocusRequester = remember { FocusRequester() }
    val connectedDevicesFirstFocusRequester = remember { FocusRequester() }
    val settingsFirstFocusRequester = remember { FocusRequester() }

    val returnToHome = {
        activeView = ActiveView.HOME
        activeMenu = LeftMenuItem.HOME
    }

    var currentTime by remember { mutableStateOf(formatTime()) }
    var currentDay by remember { mutableStateOf(formatDayName()) }
    var currentDate by remember { mutableStateOf(formatDate()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formatTime()
            currentDay = formatDayName()
            currentDate = formatDate()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    LaunchedEffect(activeView) {
        when (activeView) {
            ActiveView.ALL_APPS -> {
                kotlinx.coroutines.delay(50L)
                try {
                    allAppsFirstFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            ActiveView.CONNECTED_DEVICES -> {
                kotlinx.coroutines.delay(50L)
                try {
                    connectedDevicesFirstFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            ActiveView.SETTINGS -> {
                kotlinx.coroutines.delay(50L)
                try {
                    settingsFirstFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            ActiveView.HOME -> Unit
        }
    }

    val homeSlots = remember(apps) {
        resolveSixHomeSlots(context, apps)
    }

    val canvasBackground = if (isDarkTheme) Color(0xFF0B0D11) else AlmostWhiteBackground
    val useLightSurfaceContent = !isDarkTheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 108.dp, end = 24.dp, top = 40.dp, bottom = 28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            when (activeView) {
                ActiveView.HOME -> {
                    CenteredSixAppRow(
                        slots = homeSlots,
                        viewModel = viewModel,
                        favorites = favorites,
                        context = context,
                        currentDay = currentDay,
                        currentDate = currentDate,
                        currentTime = currentTime,
                        useLightSurfaceContent = useLightSurfaceContent,
                        firstItemFocusRequester = centerRowFocusRequester,
                        enabled = true
                    )
                }

                ActiveView.ALL_APPS -> {
                    AllAppsDrawerPanel(
                        apps = apps,
                        viewModel = viewModel,
                        context = context,
                        useLightSurfaceContent = useLightSurfaceContent,
                        firstItemFocusRequester = allAppsFirstFocusRequester,
                        onClose = returnToHome,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActiveView.CONNECTED_DEVICES -> {
                    ConnectedDevicesDrawerPanel(
                        context = context,
                        useLightSurfaceContent = useLightSurfaceContent,
                        firstItemFocusRequester = connectedDevicesFirstFocusRequester,
                        onClose = returnToHome,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ActiveView.SETTINGS -> {
                    SettingsMainView(
                        isDarkTheme = isDarkTheme,
                        useLightSurfaceContent = useLightSurfaceContent,
                        firstFocusRequester = settingsFirstFocusRequester,
                        onOpenSystemSettings = { launchSystemSettings(context) },
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = returnToHome,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
        ) {
            LeftVerticalPillMenu(
                activeMenu = activeMenu,
                useLightSurfaceContent = useLightSurfaceContent,
                onMenuFocus = { focusedItem ->
                    activeMenu = focusedItem
                },
                onHomeClick = {
                    activeMenu = LeftMenuItem.HOME
                    activeView = ActiveView.HOME
                    try {
                        centerRowFocusRequester.requestFocus()
                    } catch (_: Exception) {
                    }
                },
                onSettingsClick = {
                    if (activeView == ActiveView.SETTINGS) {
                        returnToHome()
                    } else {
                        activeMenu = LeftMenuItem.SETTINGS
                        activeView = ActiveView.SETTINGS
                    }
                },
                onAllAppsClick = {
                    if (activeView == ActiveView.ALL_APPS) {
                        returnToHome()
                    } else {
                        activeMenu = LeftMenuItem.ALL_APPS
                        activeView = ActiveView.ALL_APPS
                    }
                },
                onConnectedDevicesClick = {
                    if (activeView == ActiveView.CONNECTED_DEVICES) {
                        returnToHome()
                    } else {
                        activeMenu = LeftMenuItem.CONNECTED_DEVICES
                        activeView = ActiveView.CONNECTED_DEVICES
                    }
                }
            )
        }
    }
}

@Composable
private fun TopHeaderWithCenteredTitle(
    title: String,
    useLightSurfaceContent: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = if (useLightSurfaceContent) LightInkPrimary else Color.White

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            BackIconButton(
                onClick = onBack,
                useLightSurfaceContent = useLightSurfaceContent
            )
        }
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsMainView(
    isDarkTheme: Boolean,
    useLightSurfaceContent: Boolean,
    firstFocusRequester: FocusRequester,
    onOpenSystemSettings: () -> Unit,
    onToggleTheme: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isThemeToggleFocused by remember { mutableStateOf(false) }
    var isOpenSystemFocused by remember { mutableStateOf(false) }

    val themeToggleScale by animateFloatAsState(
        targetValue = if (isThemeToggleFocused) 1.02f else 1f,
        label = "themeToggleScale"
    )
    val openSystemScale by animateFloatAsState(
        targetValue = if (isOpenSystemFocused) 1.02f else 1f,
        label = "openSystemSettingsScale"
    )

    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val secondaryTextColor = if (useLightSurfaceContent) LightInkSecondary else Color.White.copy(alpha = 0.8f)
    val cardBgColor = if (useLightSurfaceContent) Color.White else Color.White.copy(alpha = 0.08f)
    val focusedBgColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val focusedTextColor = if (useLightSurfaceContent) Color.White else LightInkPrimary

    Column(
        modifier = modifier
            .trapDrawerFocus(onBack)
            .padding(start = 12.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopHeaderWithCenteredTitle(
            title = "App setting",
            useLightSurfaceContent = useLightSurfaceContent,
            onBack = onBack
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = themeToggleScale
                    scaleY = themeToggleScale
                }
                .focusRequester(firstFocusRequester)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isThemeToggleFocused) focusedBgColor else cardBgColor)
                .border(
                    width = 1.dp,
                    color = if (isThemeToggleFocused) focusedBgColor else primaryTextColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(16.dp)
                )
                .onFocusChanged { isThemeToggleFocused = it.isFocused }
                .focusable()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleTheme
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Switch Dark / Light Theme",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isThemeToggleFocused) focusedTextColor else primaryTextColor
                )
                Text(
                    text = if (isDarkTheme) "Dark" else "Light",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isThemeToggleFocused) focusedTextColor.copy(alpha = 0.85f) else secondaryTextColor
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "System Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = openSystemScale
                        scaleY = openSystemScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isOpenSystemFocused) SystemSettingsAccentFocused else SystemSettingsAccent)
                    .border(
                        width = if (isOpenSystemFocused) 2.dp else 1.dp,
                        color = if (isOpenSystemFocused) Color.White else SystemSettingsAccentFocused.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .onFocusChanged { isOpenSystemFocused = it.isFocused }
                    .focusable()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenSystemSettings
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open System Settings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun Modifier.trapDrawerFocus(onClose: () -> Unit): Modifier {
    return this
        .focusGroup()
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                onClose()
                true
            } else {
                false
            }
        }
}

@Composable
private fun LeftVerticalPillMenu(
    activeMenu: LeftMenuItem,
    useLightSurfaceContent: Boolean,
    onMenuFocus: (LeftMenuItem) -> Unit,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onConnectedDevicesClick: () -> Unit
) {
    Box(
        modifier = Modifier.background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FrostedPillIconButton(
                icon = Icons.Filled.Home,
                glyphKind = PillGlyphKind.VECTOR,
                contentDescription = "Home",
                isSelected = activeMenu == LeftMenuItem.HOME,
                useLightSurfaceContent = useLightSurfaceContent,
                onFocus = { onMenuFocus(LeftMenuItem.HOME) },
                onClick = onHomeClick
            )
            FrostedPillIconButton(
                icon = Icons.Filled.Settings,
                glyphKind = PillGlyphKind.VECTOR,
                contentDescription = "Settings",
                isSelected = activeMenu == LeftMenuItem.SETTINGS,
                useLightSurfaceContent = useLightSurfaceContent,
                onFocus = { onMenuFocus(LeftMenuItem.SETTINGS) },
                onClick = onSettingsClick
            )
            FrostedPillIconButton(
                icon = Icons.Filled.Menu,
                glyphKind = PillGlyphKind.APP_GRID,
                contentDescription = "All Apps",
                isSelected = activeMenu == LeftMenuItem.ALL_APPS,
                useLightSurfaceContent = useLightSurfaceContent,
                onFocus = { onMenuFocus(LeftMenuItem.ALL_APPS) },
                onClick = onAllAppsClick
            )
            FrostedPillIconButton(
                icon = Icons.Filled.AccountCircle,
                glyphKind = PillGlyphKind.DEVICE_LINK,
                contentDescription = "Connected Devices",
                isSelected = activeMenu == LeftMenuItem.CONNECTED_DEVICES,
                useLightSurfaceContent = useLightSurfaceContent,
                onFocus = { onMenuFocus(LeftMenuItem.CONNECTED_DEVICES) },
                onClick = onConnectedDevicesClick
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FrostedPillIconButton(
    icon: ImageVector,
    glyphKind: PillGlyphKind,
    contentDescription: String,
    isSelected: Boolean,
    useLightSurfaceContent: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else if (isSelected) 1.06f else 1f,
        label = "pillBtnScale"
    )
    val baseInk = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val bgColor = when {
        isFocused -> baseInk.copy(alpha = 0.16f)
        isSelected -> baseInk.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val iconTint = when {
        isFocused || isSelected -> baseInk
        else -> baseInk.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgColor)
            .onFocusChanged { focusState ->
                val nowFocused = focusState.isFocused
                isFocused = nowFocused
                if (nowFocused) {
                    onFocus()
                }
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when (glyphKind) {
            PillGlyphKind.APP_GRID -> {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val gap = size.minDimension * 0.18f
                    val cell = (size.minDimension - gap) / 2f
                    val radius = CornerRadius(cell * 0.3f, cell * 0.3f)
                    for (row in 0..1) {
                        for (col in 0..1) {
                            drawRoundRect(
                                color = iconTint,
                                topLeft = Offset(
                                    x = col * (cell + gap),
                                    y = row * (cell + gap)
                                ),
                                size = Size(cell, cell),
                                cornerRadius = radius
                            )
                        }
                    }
                }
            }

            PillGlyphKind.DEVICE_LINK -> {
                Canvas(modifier = Modifier.size(21.dp)) {
                    val stroke = size.minDimension * 0.09f
                    val screenW = size.width * 0.86f
                    val screenH = size.height * 0.56f
                    val left = (size.width - screenW) / 2f
                    val top = size.height * 0.12f
                    drawRoundRect(
                        color = iconTint,
                        topLeft = Offset(left, top),
                        size = Size(screenW, screenH),
                        cornerRadius = CornerRadius(stroke * 1.5f, stroke * 1.5f),
                        style = Stroke(width = stroke)
                    )
                    drawLine(
                        color = iconTint,
                        start = Offset(size.width * 0.34f, top + screenH + stroke * 1.6f),
                        end = Offset(size.width * 0.66f, top + screenH + stroke * 1.6f),
                        strokeWidth = stroke
                    )
                    drawCircle(
                        color = iconTint,
                        radius = stroke * 1.15f,
                        center = Offset(size.width * 0.5f, top + screenH * 0.5f)
                    )
                }
            }

            PillGlyphKind.VECTOR -> {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceCategoryGlyph(
    kind: DeviceIconKind,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = size.minDimension * 0.085f
        when (kind) {
            DeviceIconKind.HEADPHONES -> {
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.14f),
                    size = Size(size.width * 0.68f, size.height * 0.62f),
                    style = Stroke(width = stroke)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.48f),
                    size = Size(size.width * 0.20f, size.height * 0.34f),
                    cornerRadius = CornerRadius(stroke * 1.4f, stroke * 1.4f)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.68f, size.height * 0.48f),
                    size = Size(size.width * 0.20f, size.height * 0.34f),
                    cornerRadius = CornerRadius(stroke * 1.4f, stroke * 1.4f)
                )
            }

            DeviceIconKind.CONTROLLER -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
                    size = Size(size.width * 0.76f, size.height * 0.44f),
                    cornerRadius = CornerRadius(size.height * 0.22f, size.height * 0.22f),
                    style = Stroke(width = stroke)
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.32f, size.height * 0.42f),
                    end = Offset(size.width * 0.32f, size.height * 0.58f),
                    strokeWidth = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.24f, size.height * 0.50f),
                    end = Offset(size.width * 0.40f, size.height * 0.50f),
                    strokeWidth = stroke
                )
                drawCircle(
                    color = tint,
                    radius = stroke * 0.85f,
                    center = Offset(size.width * 0.68f, size.height * 0.44f)
                )
                drawCircle(
                    color = tint,
                    radius = stroke * 0.85f,
                    center = Offset(size.width * 0.74f, size.height * 0.54f)
                )
            }

            DeviceIconKind.DISPLAY -> {
                val boxW = size.width * 0.80f
                val boxH = size.height * 0.52f
                val left = (size.width - boxW) / 2f
                val top = size.height * 0.16f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(left, top),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(stroke * 1.2f, stroke * 1.2f),
                    style = Stroke(width = stroke)
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.34f, top + boxH + stroke * 1.8f),
                    end = Offset(size.width * 0.66f, top + boxH + stroke * 1.8f),
                    strokeWidth = stroke
                )
            }

            DeviceIconKind.SPEAKER -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.24f, size.height * 0.12f),
                    size = Size(size.width * 0.52f, size.height * 0.76f),
                    cornerRadius = CornerRadius(stroke * 1.6f, stroke * 1.6f),
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = tint,
                    radius = size.width * 0.14f,
                    center = Offset(size.width * 0.5f, size.height * 0.60f),
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = tint,
                    radius = size.width * 0.05f,
                    center = Offset(size.width * 0.5f, size.height * 0.28f)
                )
            }

            DeviceIconKind.MOBILE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.28f, size.height * 0.12f),
                    size = Size(size.width * 0.44f, size.height * 0.76f),
                    cornerRadius = CornerRadius(stroke * 1.5f, stroke * 1.5f),
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = tint,
                    radius = stroke * 0.75f,
                    center = Offset(size.width * 0.5f, size.height * 0.78f)
                )
            }

            DeviceIconKind.GENERIC -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.34f,
                    style = Stroke(width = stroke)
                )
                drawCircle(
                    color = tint,
                    radius = stroke * 1.1f
                )
            }
        }
    }
}

private fun classifyBluetoothDevice(device: BluetoothDevice, label: String): DeviceIconKind {
    val nameLower = label.lowercase(Locale.ROOT)
    if (
        nameLower.contains("airpods") ||
        nameLower.contains("earbud") ||
        nameLower.contains("buds") ||
        nameLower.contains("headphone") ||
        nameLower.contains("headset") ||
        nameLower.contains("wh-1000") ||
        nameLower.contains("wf-1000") ||
        nameLower.contains("beats") ||
        nameLower.contains("bose")
    ) {
        return DeviceIconKind.HEADPHONES
    }
    if (
        nameLower.contains("controller") ||
        nameLower.contains("gamepad") ||
        nameLower.contains("dualshock") ||
        nameLower.contains("dualsense") ||
        nameLower.contains("xbox") ||
        nameLower.contains("remote")
    ) {
        return DeviceIconKind.CONTROLLER
    }
    if (
        nameLower.contains("tv") ||
        nameLower.contains("bravia") ||
        nameLower.contains("tcl") ||
        nameLower.contains("monitor") ||
        nameLower.contains("display")
    ) {
        return DeviceIconKind.DISPLAY
    }
    if (
        nameLower.contains("speaker") ||
        nameLower.contains("soundbar") ||
        nameLower.contains("sonos") ||
        nameLower.contains("jbl")
    ) {
        return DeviceIconKind.SPEAKER
    }
    if (
        nameLower.contains("iphone") ||
        nameLower.contains("galaxy") ||
        nameLower.contains("pixel") ||
        nameLower.contains("phone") ||
        nameLower.contains("ipad") ||
        nameLower.contains("tab")
    ) {
        return DeviceIconKind.MOBILE
    }

    val majorClass = try {
        device.bluetoothClass?.majorDeviceClass
    } catch (_: SecurityException) {
        null
    }

    return when (majorClass) {
        BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceIconKind.HEADPHONES
        BluetoothClass.Device.Major.PERIPHERAL -> DeviceIconKind.CONTROLLER
        BluetoothClass.Device.Major.PHONE -> DeviceIconKind.MOBILE
        BluetoothClass.Device.Major.COMPUTER -> DeviceIconKind.DISPLAY
        else -> DeviceIconKind.GENERIC
    }
}

@Suppress("MissingPermission")
private fun findBluetoothDevice(context: Context, address: String?): BluetoothDevice? {
    if (address.isNullOrBlank()) return null
    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter = btManager?.adapter ?: return null
    return try {
        adapter.bondedDevices?.firstOrNull { it.address.equals(address, ignoreCase = true) }
            ?: adapter.getRemoteDevice(address)
    } catch (_: Exception) {
        null
    }
}

private fun buildTvBluetoothSettingsIntent(
    context: Context,
    deviceAddress: String? = null
): Intent {
    val pm = context.packageManager
    val targetDevice: BluetoothDevice? = findBluetoothDevice(context, deviceAddress)

    val candidates = listOf(
        Intent().setComponent(
            ComponentName(
                "com.android.tv.settings",
                "com.android.tv.settings.connectivity.bluetooth.BluetoothActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.android.tv.settings",
                "com.android.tv.settings.bluetooth.BluetoothActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.android.settings",
                "com.android.settings.bluetooth.BluetoothSettings"
            )
        ),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        Intent().setComponent(
            ComponentName(
                "com.android.tv.settings",
                "com.android.tv.settings.accessories.AddAccessoryActivity"
            )
        ),
        Intent(Settings.ACTION_SETTINGS)
    )

    for (candidate in candidates) {
        if (pm.resolveActivity(candidate, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            if (targetDevice != null) {
                candidate.putExtra(BluetoothDevice.EXTRA_DEVICE, targetDevice)
            }
            return candidate
        }
    }
    return Intent(Settings.ACTION_SETTINGS)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BackIconButton(
    onClick: () -> Unit,
    useLightSurfaceContent: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        label = "backIconBtnScale"
    )
    val inkColor = if (useLightSurfaceContent) LightInkPrimary else Color.White

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isFocused) inkColor.copy(alpha = 0.20f)
                else inkColor.copy(alpha = 0.08f)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = inkColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AppDrawerIconSkeleton(
    useLightSurfaceContent: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "drawerIconSkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drawerIconSkeletonAlpha"
    )
    val baseColor = if (useLightSurfaceContent) LightInkPrimary else Color.White

    Box(
        modifier = modifier
            .size(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor.copy(alpha = alpha))
    )
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AllAppsDrawerPanel(
    apps: List<AppInfo>,
    viewModel: HomeViewModel,
    context: Context,
    useLightSurfaceContent: Boolean,
    firstItemFocusRequester: FocusRequester,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedApps = remember(apps) {
        apps.sortedBy { it.label.lowercase(Locale.ROOT) }
    }
    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val secondaryTextColor = if (useLightSurfaceContent) LightInkSecondary else Color.White.copy(alpha = 0.82f)
    val drawerIconShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .trapDrawerFocus(onClose)
            .padding(start = 12.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopHeaderWithCenteredTitle(
            title = "App",
            useLightSurfaceContent = useLightSurfaceContent,
            onBack = onClose
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(sortedApps, key = { _, app -> app.packageName }) { index, appItem ->
                val cacheKey = remember(appItem.packageName) {
                    "app_${appItem.packageName}"
                }
                var iconBitmap by remember(cacheKey) {
                    mutableStateOf(iconBitmapCache.get(cacheKey)?.asImageBitmap())
                }
                var isLoadingIcon by remember(cacheKey) {
                    mutableStateOf(iconBitmap == null)
                }
                var isFocused by remember { mutableStateOf(false) }

                LaunchedEffect(cacheKey) {
                    if (iconBitmap == null) {
                        isLoadingIcon = true
                        val bmp = loadNormalizedAppBitmap(context, viewModel, appItem, listOf(appItem.packageName))
                        if (bmp != null) {
                            iconBitmapCache.put(cacheKey, bmp)
                            iconBitmap = bmp.asImageBitmap()
                        }
                        isLoadingIcon = false
                    } else {
                        isLoadingIcon = false
                    }
                }

                val focusScale by animateFloatAsState(
                    targetValue = if (isFocused) 1.18f else 1f,
                    label = "gridIconFocusScale"
                )

                val triggerLaunch = {
                    Log.d(TAG, "Drawer item activated: label=${appItem.label}, pkg=${appItem.packageName}")
                    launchDrawerApp(context, appItem, onClose)
                }

                Column(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .then(
                            if (index == 0) Modifier.focusRequester(firstItemFocusRequester)
                            else Modifier
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp &&
                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                            ) {
                                triggerLaunch()
                                true
                            } else {
                                false
                            }
                        }
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = triggerLaunch
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap!!,
                                contentDescription = appItem.label,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(58.dp)
                                    .graphicsLayer {
                                        scaleX = focusScale
                                        scaleY = focusScale
                                    }
                                    .clip(drawerIconShape)
                            )
                        } else if (isLoadingIcon) {
                            AppDrawerIconSkeleton(
                                useLightSurfaceContent = useLightSurfaceContent,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = focusScale
                                    scaleY = focusScale
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .graphicsLayer {
                                        scaleX = focusScale
                                        scaleY = focusScale
                                    }
                                    .clip(drawerIconShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = appItem.label,
                                    tint = primaryTextColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appItem.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isFocused) primaryTextColor else secondaryTextColor,
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun launchDrawerApp(context: Context, appItem: AppInfo, onClose: () -> Unit) {
    val pkg = appItem.packageName.takeIf { it.isNotBlank() && !it.startsWith("slot.placeholder.") }
    val candidates = buildList {
        if (pkg != null) {
            context.packageManager.getLeanbackLaunchIntentForPackage(pkg)?.let { add(it) }
            context.packageManager.getLaunchIntentForPackage(pkg)?.let { add(it) }
        }
        add(Intent(appItem.launchIntent))
        if (pkg != null) {
            add(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setPackage(pkg)
            )
            add(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(pkg)
            )
        }
    }

    for (candidate in candidates) {
        try {
            val intent = Intent(candidate).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(intent)
            onClose()
            return
        } catch (e: Exception) {
            Log.w(TAG, "Launch candidate failed for pkg=$pkg action=${candidate.action}", e)
        }
    }

    Log.e(TAG, "All launch attempts failed for drawer app: label=${appItem.label}, pkg=${appItem.packageName}")
}

private suspend fun loadNormalizedAppBitmap(
    context: Context,
    viewModel: HomeViewModel,
    appItem: AppInfo,
    candidatePackages: List<String>
): Bitmap? {
    val cacheKey = "${appItem.packageName}|${candidatePackages.joinToString(",")}"
    iconBitmapCache.get(cacheKey)?.let { return it }

    val directDrawable = loadAppDrawableFromPackages(context, appItem, candidatePackages)
    val rawBitmap = if (directDrawable != null) {
        drawableToBitmap(directDrawable, 160, 160)
    } else {
        viewModel.loadAppIconBitmap(appItem.packageName, candidatePackages, 160)
    }

    val normalized = normalizeSquareBitmap(rawBitmap, 160)
    if (normalized != null) {
        iconBitmapCache.put(cacheKey, normalized)
    }
    return normalized
}

private fun normalizeSquareBitmap(source: Bitmap?, targetSize: Int = 160): Bitmap? {
    if (source == null || source.width <= 0 || source.height <= 0) return source
    return try {
        if (source.width == targetSize && source.height == targetSize && source.config == Bitmap.Config.ARGB_8888) {
            source
        } else {
            val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val scale = min(targetSize.toFloat() / source.width, targetSize.toFloat() / source.height)
            val drawW = (source.width * scale).toInt().coerceAtLeast(1)
            val drawH = (source.height * scale).toInt().coerceAtLeast(1)
            val left = (targetSize - drawW) / 2
            val top = (targetSize - drawH) / 2
            val scaled = Bitmap.createScaledBitmap(source, drawW, drawH, true)
            canvas.drawBitmap(scaled, left.toFloat(), top.toFloat(), null)
            if (scaled !== source) {
                scaled.recycle()
            }
            out
        }
    } catch (_: Exception) {
        source
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConnectedDevicesDrawerPanel(
    context: Context,
    useLightSurfaceContent: Boolean,
    firstItemFocusRequester: FocusRequester,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var refreshKey by remember { mutableStateOf(0) }
    val devices = remember(refreshKey) {
        queryConnectedDevices(context)
    }

    val openDeviceManagement = {
        val intent = buildTvBluetoothSettingsIntent(context)
        onClose()
        launchAppSafely(context, intent)
    }

    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val secondaryTextColor = if (useLightSurfaceContent) LightInkSecondary else Color.White.copy(alpha = 0.8f)
    val cardBgColor = if (useLightSurfaceContent) Color.White else Color.White.copy(alpha = 0.08f)
    val focusedBgColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val focusedTextColor = if (useLightSurfaceContent) Color.White else LightInkPrimary

    Column(
        modifier = modifier
            .trapDrawerFocus(onClose)
            .padding(start = 12.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TopHeaderWithCenteredTitle(
            title = "Connected Devices",
            useLightSurfaceContent = useLightSurfaceContent,
            onBack = onClose
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerActionChip(
                icon = Icons.Filled.Refresh,
                label = "Refresh",
                useLightSurfaceContent = useLightSurfaceContent,
                onClick = { refreshKey++ }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(devices) { index, device ->
                var isFocused by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index == 0) Modifier.focusRequester(firstItemFocusRequester)
                            else Modifier
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isFocused) primaryTextColor.copy(alpha = 0.12f)
                            else cardBgColor
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFocused) primaryTextColor.copy(alpha = 0.45f)
                            else primaryTextColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primaryTextColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        DeviceCategoryGlyph(
                            kind = device.iconKind,
                            tint = primaryTextColor
                        )
                    }

                    Text(
                        text = device.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.subtitle,
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            var isAddFocused by remember { mutableStateOf(false) }
            val addScale by animateFloatAsState(
                targetValue = if (isAddFocused) 1.06f else 1f,
                label = "addMoreBtnScale"
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = addScale
                        scaleY = addScale
                    }
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isAddFocused) focusedBgColor else cardBgColor)
                    .border(
                        width = 1.dp,
                        color = if (isAddFocused) focusedBgColor else primaryTextColor.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .onFocusChanged { isAddFocused = it.isFocused }
                    .focusable()
                    .clickable(onClick = openDeviceManagement)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add More",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAddFocused) focusedTextColor else primaryTextColor
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DrawerActionChip(
    icon: ImageVector,
    label: String,
    useLightSurfaceContent: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val chipBgColor = if (useLightSurfaceContent) Color.White else Color.White.copy(alpha = 0.08f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isFocused) primaryTextColor.copy(alpha = 0.16f)
                else chipBgColor
            )
            .border(
                width = 1.dp,
                color = primaryTextColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = primaryTextColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = primaryTextColor
        )
    }
}

@Suppress("MissingPermission")
private fun queryConnectedDevices(context: Context): List<ConnectedDeviceItem> {
    val items = mutableListOf<ConnectedDeviceItem>()

    val canReadBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
            PackageManager.PERMISSION_GRANTED
    }

    if (!canReadBluetooth) {
        return listOf(
            ConnectedDeviceItem(
                name = "Bluetooth permission required",
                subtitle = "Grant BLUETOOTH_CONNECT to detect real connected devices",
                iconKind = DeviceIconKind.GENERIC
            )
        )
    }

    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val adapter: BluetoothAdapter? = btManager?.adapter
    if (adapter == null || !adapter.isEnabled) {
        return listOf(
            ConnectedDeviceItem(
                name = "No active devices connected",
                subtitle = if (adapter == null) "Bluetooth adapter unavailable" else "Bluetooth is turned off",
                iconKind = DeviceIconKind.GENERIC
            )
        )
    }

    val connectedAddresses = mutableSetOf<String>()
    val profilesToCheck = listOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP)
    for (profile in profilesToCheck) {
        try {
            val connectedDevices: List<BluetoothDevice>? = btManager.getConnectedDevices(profile)
            connectedDevices?.forEach { dev ->
                val addr = dev.address.orEmpty()
                if (addr.isNotBlank() && connectedAddresses.add(addr)) {
                    val label = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            dev.alias ?: dev.name
                        } else {
                            dev.name
                        }?.takeIf { it.isNotBlank() } ?: addr
                    } catch (_: SecurityException) {
                        addr
                    }
                    items.add(
                        ConnectedDeviceItem(
                            name = label,
                            subtitle = "Connected • $addr",
                            address = addr,
                            isConnected = true,
                            iconKind = classifyBluetoothDevice(dev, label)
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    try {
        adapter.bondedDevices?.forEach { dev ->
            val addr = dev.address.orEmpty()
            val isConnected = try {
                dev.bondState == BluetoothDevice.BOND_BONDED && addr in connectedAddresses
            } catch (_: SecurityException) {
                false
            }
            if (!isConnected && addr.isNotBlank() && connectedAddresses.add(addr)) {
                val label = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        dev.alias ?: dev.name
                    } else {
                        dev.name
                    }?.takeIf { it.isNotBlank() } ?: addr
                } catch (_: SecurityException) {
                    addr
                }
                items.add(
                    ConnectedDeviceItem(
                        name = label,
                        subtitle = "Paired • $addr",
                        address = addr,
                        isConnected = false,
                        iconKind = classifyBluetoothDevice(dev, label)
                    )
                )
            }
        }
    } catch (_: SecurityException) {
    }

    if (items.isEmpty()) {
        items.add(
            ConnectedDeviceItem(
                name = "No connected or paired devices",
                subtitle = "No real devices are currently connected to this TV",
                iconKind = DeviceIconKind.GENERIC
            )
        )
    }

    return items
}

private fun launchSystemSettings(context: Context) {
    val tvSettings = context.packageManager.getLeanbackLaunchIntentForPackage("com.android.tv.settings")
        ?: context.packageManager.getLaunchIntentForPackage("com.android.tv.settings")
    launchAppSafely(context, tvSettings ?: Intent(Settings.ACTION_SETTINGS))
}

private fun launchAppSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
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
    context: Context,
    currentDay: String,
    currentDate: String,
    currentTime: String,
    useLightSurfaceContent: Boolean,
    firstItemFocusRequester: FocusRequester? = null,
    enabled: Boolean = true
) {
    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val secondaryTextColor = if (useLightSurfaceContent) LightInkSecondary else Color.White.copy(alpha = 0.85f)
    val dayDateFontSize = 25.sp
    val dayDateFontWeight = FontWeight.Medium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 24.dp, top = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = currentDay,
                    fontSize = dayDateFontSize,
                    fontWeight = dayDateFontWeight,
                    color = secondaryTextColor
                )
                Text(
                    text = currentDate,
                    fontSize = dayDateFontSize,
                    fontWeight = dayDateFontWeight,
                    color = secondaryTextColor
                )
            }
            Text(
                text = currentTime,
                fontSize = 68.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Quick Apps",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEachIndexed { index, slot ->
                AppCircleTile(
                    app = slot.appInfo,
                    candidatePackages = slot.candidatePackages,
                    defaultIcon = slot.defaultIcon,
                    viewModel = viewModel,
                    isFavorite = favorites.contains(slot.appInfo.packageName),
                    useLightSurfaceContent = useLightSurfaceContent,
                    context = context,
                    focusRequester = if (index == 0) firstItemFocusRequester else null,
                    enabled = enabled
                )
            }
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
    useLightSurfaceContent: Boolean = true,
    context: Context,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true
) {
    val cacheKey = remember(app.packageName, candidatePackages) {
        "${app.packageName}|${candidatePackages.joinToString(",")}"
    }
    var iconBitmap by remember(cacheKey) {
        mutableStateOf(iconBitmapCache.get(cacheKey)?.asImageBitmap())
    }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val quickIconShape = RoundedCornerShape(14.dp)

    LaunchedEffect(cacheKey) {
        if (iconBitmap == null) {
            val bmp = loadNormalizedAppBitmap(context, viewModel, app, candidatePackages)
            if (bmp != null) {
                iconBitmap = bmp.asImageBitmap()
            } else {
                Log.d(TAG, "No bitmap for label=${app.label}, pkg=${app.packageName}, candidates=$candidatePackages")
            }
        }
    }

    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.18f else 1f,
        label = "quickAppIconFocusScale"
    )
    val primaryTextColor = if (useLightSurfaceContent) LightInkPrimary else Color.White
    val secondaryTextColor = if (useLightSurfaceContent) LightInkSecondary else Color.White.copy(alpha = 0.82f)

    Column(
        modifier = Modifier
            .background(Color.Transparent)
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    launchAppSafely(context, app.launchIntent)
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
                Image(
                    bitmap = iconBitmap!!,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            scaleX = focusScale
                            scaleY = focusScale
                        }
                        .clip(quickIconShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer {
                            scaleX = focusScale
                            scaleY = focusScale
                        }
                        .clip(quickIconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = app.label,
                        tint = primaryTextColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(32.dp)
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
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFocused) primaryTextColor else secondaryTextColor,
                modifier = Modifier
                    .background(Color.Transparent)
                    .padding(horizontal = 2.dp)
            )
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

fun formatDayName(): String {
    return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
}

fun formatDate(): String {
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
}
