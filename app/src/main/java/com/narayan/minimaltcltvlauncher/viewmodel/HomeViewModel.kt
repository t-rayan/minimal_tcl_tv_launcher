package com.narayan.minimaltcltvlauncher.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.narayan.minimaltcltvlauncher.model.AppInfo
import com.narayan.minimaltcltvlauncher.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import android.util.LruCache

/**
 * ViewModel that holds the list of discovered apps.
 * Uses StateFlow to expose a single observable list to the UI.
 * Loads apps once on init via AppRepository.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    // Icon cache - stores normalized 160x160 bitmaps keyed by package name
    private val iconCache = object : LruCache<String, Bitmap>(32) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // size in KB
        }
    }

    // Loading state for initial app load
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // MutableStateFlow: thread-safe, holds the latest value, notifies collectors on change
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    // Favorites (pinned apps)
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    init {
        loadApps()
        loadFavorites()
    }

    /**
     * Starts the app discovery coroutine.
     * Runs on viewModelScope (auto-cancelled when ViewModel is cleared).
     * Repository query runs on Dispatchers.IO; result posted to _apps on main thread.
     */
    private fun loadApps() {
        val startTime = SystemClock.elapsedRealtime()
        Log.d("StartupTiming", "loadApps start: ${SystemClock.elapsedRealtime() - startTime}ms")
        viewModelScope.launch {
            repository.getApps()
                // Collect each emission (there's only one) and update state
                .onEach { appList ->
                    val loadTime = SystemClock.elapsedRealtime()
                    Log.d("StartupTiming", "Apps loaded (${appList.size} apps): ${loadTime - startTime}ms")
                    _apps.value = appList
                    _isLoading.value = false
                    // Warm icon cache for all discovered apps in background
                    preloadIcons(appList.map { it.packageName })
                }
                .collect() // terminal operator; keeps coroutine alive until flow completes
        }
    }

    /**
     * Loads favorites from SharedPreferences.
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            val favs = repository.getFavorites()
            _favorites.value = favs
        }
    }

    /**
     * Toggles favorite status for a package.
     */
    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val updated = repository.toggleFavorite(packageName)
            _favorites.value = updated
        }
    }

    /**
     * Checks if a package is favorited.
     */
    fun isFavorite(packageName: String): Boolean {
        return _favorites.value.contains(packageName)
    }

    /**
     * Returns apps split into favorites and non-favorites, preserving order.
     */
    fun getAppsWithFavoritesFirst(): Pair<List<AppInfo>, List<AppInfo>> {
        val allApps = _apps.value
        val favs = _favorites.value
        val favoritesList = allApps.filter { favs.contains(it.packageName) }
        val otherApps = allApps.filter { !favs.contains(it.packageName) }
        return Pair(favoritesList, otherApps)
    }

    /**
     * Helper to load a TV banner drawable for an app on a background thread.
     * Returns null if not available or on error.
     */
    suspend fun loadBanner(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            (getApplication() as Application).packageManager.getApplicationBanner(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null // some apps throw on banner load
        }
    }

    /**
     * Helper to load a square icon drawable for an app on a background thread.
     * Returns null if not available.
     */
    suspend fun loadIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            (getApplication() as Application).packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Loads the best available app icon as a square Bitmap on IO thread.
     * Tries multiple package names (candidate packages) for apps with aliases.
     * Handles TV banner center-cropping for wide (320x180) banners.
     * Fallback chain per package: banner (center-cropped if wide) → icon (fit) → next package.
     * Returns null if nothing is available.
     */
    suspend fun loadAppIconBitmap(packageName: String, candidatePackages: List<String> = emptyList(), targetSize: Int = 160): Bitmap? = withContext(Dispatchers.IO) {
        val pm = (getApplication() as Application).packageManager
        
        // Build list of packages to try: primary + candidates, deduplicated
        val packagesToTry = (listOf(packageName) + candidatePackages).distinct()

        for (pkg in packagesToTry) {
            // Try banner first (TV apps often have banners, not square icons)
            val bannerDrawable = try {
                pm.getApplicationBanner(pkg)
            } catch (e: Exception) {
                null
            }

            bannerDrawable?.let { drawable ->
                // Detect if banner is wide (TV banner) vs square-ish (some apps have square banners)
                val intrinsicWidth = drawable.intrinsicWidth
                val intrinsicHeight = drawable.intrinsicHeight
                val isWideBanner = intrinsicWidth > 0 && intrinsicHeight > 0 && 
                    (intrinsicWidth.toFloat() / intrinsicHeight > 1.5f) // 320x180 ≈ 1.78 ratio
                
                val bitmap = if (isWideBanner) {
                    // Wide TV banner: center-crop to square
                    drawableToBitmapCenterCrop(drawable, targetSize, targetSize)
                } else {
                    // Square-ish banner or unknown: scale to fit (no crop)
                    drawableToBitmap(drawable, targetSize, targetSize)
                }
                
                if (bitmap != null) return@withContext bitmap
            }

            // Fallback to regular app icon
            val iconDrawable = try {
                pm.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }

            iconDrawable?.let { drawable ->
                val bitmap = drawableToBitmap(drawable, targetSize, targetSize)
                if (bitmap != null) return@withContext bitmap
            }
        }

        null
    }

    /**
     * Converts a Drawable to a Bitmap with center-cropping.
     * Handles wide banners (320x180) by center-cropping to square.
     * Scales to fill the target bounds, cropping excess.
     */
    private fun drawableToBitmapCenterCrop(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val src = drawable.bitmap
                // If already close to target, just scale
                if (src.width <= targetWidth * 2 && src.height <= targetHeight * 2) {
                    return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
                }
                // Otherwise center-crop
                return centerCropBitmap(src, targetWidth, targetHeight)
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: targetWidth
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: targetHeight
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                // Scale to fill target (may crop), then center
                val scale = max(targetWidth.toFloat() / width, targetHeight.toFloat() / height)
                val drawWidth = (width * scale).toInt().coerceAtLeast(1)
                val drawHeight = (height * scale).toInt().coerceAtLeast(1)
                val left = (targetWidth - drawWidth) / 2
                val top = (targetHeight - drawHeight) / 2
                val copy = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
                copy.setBounds(left, top, left + drawWidth, top + drawHeight)
                copy.draw(canvas)
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Simple center-crop for Bitmaps.
     */
    private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height
        val scale = max(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true)
        val x = (scaledWidth - targetWidth) / 2
        val y = (scaledHeight - targetHeight) / 2
        return Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
    }

    /**
     * Converts a Drawable to a Bitmap, scaling to fit (no cropping).
     * Used for square icons.
     */
    private fun drawableToBitmap(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val src = drawable.bitmap
                if (src.width <= targetWidth && src.height <= targetHeight) {
                    src
                } else {
                    Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
                }
            } else {
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
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets a cached icon bitmap, or loads and caches it if not present.
     * Runs on IO thread to avoid blocking the main thread.
     */
    suspend fun getIconBitmap(packageName: String, candidatePackages: List<String> = emptyList(), targetSize: Int = 160): Bitmap? {
        val cacheKey = packageName
        
        // Check cache first (on main thread, but LruCache is thread-safe for reads)
        val cached = iconCache.get(cacheKey)
        if (cached != null) return cached
        
        // Load on IO thread
        val bitmap = withContext(Dispatchers.IO) {
            loadAppIconBitmap(packageName, candidatePackages, 160)
        }
        
        // Cache the result
        bitmap?.let { iconCache.put(cacheKey, it) }
        return bitmap
    }

    /**
     * Preloads icons for a list of packages in background.
     * Useful for warming the cache during idle time.
     */
    fun preloadIcons(packages: List<String>) {
        viewModelScope.launch {
            packages.forEach { pkg ->
                if (iconCache.get(pkg) == null) {
                    val bitmap = loadAppIconBitmap(pkg, emptyList(), 160)
                    bitmap?.let { iconCache.put(pkg, it) }
                }
            }
        }
    }

    /**
     * Clears the icon cache.
     */
    fun clearIconCache() {
        iconCache.evictAll()
    }

    /**
     * Factory for creating HomeViewModel with Application context.
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            return HomeViewModel(application) as T
        }
    }
}