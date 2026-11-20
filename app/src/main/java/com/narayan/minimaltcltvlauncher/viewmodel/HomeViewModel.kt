package com.narayan.minimaltcltvlauncher.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.narayan.minimaltcltvlauncher.model.AppInfo
import com.narayan.minimaltcltvlauncher.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val viewModelStartTime = SystemClock.elapsedRealtime()
    private val repository = AppRepository(application)

    private val iconCache = object : LruCache<String, Bitmap>(32) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites

    init {
        loadApps()
        loadFavorites()
    }

    private fun loadApps() {
        Log.d("StartupTiming", "loadApps start: ${SystemClock.elapsedRealtime() - viewModelStartTime}ms")
        viewModelScope.launch {
            repository.getApps().collect { appList ->
                val loadTime = SystemClock.elapsedRealtime()
                Log.d(
                    "StartupTiming",
                    "Apps loaded (${appList.size} apps): ${loadTime - viewModelStartTime}ms"
                )
                _apps.value = appList
                _isLoading.value = false

                // Defer bulk icon preloading slightly so initial home frame & D-pad focus settle first
                preloadIcons(appList.map { it.packageName }, delayMs = 800L)
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val favs = repository.getFavorites()
            _favorites.value = favs
        }
    }

    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            val updated = repository.toggleFavorite(packageName)
            _favorites.value = updated
        }
    }

    fun isFavorite(packageName: String): Boolean {
        return _favorites.value.contains(packageName)
    }

    fun getAppsWithFavoritesFirst(): Pair<List<AppInfo>, List<AppInfo>> {
        val allApps = _apps.value
        val favs = _favorites.value
        val favoritesList = allApps.filter { favs.contains(it.packageName) }
        val otherApps = allApps.filter { !favs.contains(it.packageName) }
        return Pair(favoritesList, otherApps)
    }

    suspend fun loadBanner(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().packageManager.getApplicationBanner(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    suspend fun loadAppIconBitmap(
        packageName: String,
        candidatePackages: List<String> = emptyList(),
        targetSize: Int = 160
    ): Bitmap? = withContext(Dispatchers.IO) {
        val pm = getApplication<Application>().packageManager
        val packagesToTry = (listOf(packageName) + candidatePackages).distinct()

        for (pkg in packagesToTry) {
            val bannerDrawable = try {
                pm.getApplicationBanner(pkg)
            } catch (_: Exception) {
                null
            }

            bannerDrawable?.let { drawable ->
                val intrinsicWidth = drawable.intrinsicWidth
                val intrinsicHeight = drawable.intrinsicHeight
                val isWideBanner = intrinsicWidth > 0 && intrinsicHeight > 0 &&
                    (intrinsicWidth.toFloat() / intrinsicHeight > 1.5f)

                val bitmap = if (isWideBanner) {
                    drawableToBitmapCenterCrop(drawable, targetSize, targetSize)
                } else {
                    drawableToBitmap(drawable, targetSize, targetSize)
                }

                if (bitmap != null) return@withContext bitmap
            }

            val iconDrawable = try {
                pm.getApplicationIcon(pkg)
            } catch (_: Exception) {
                null
            }

            iconDrawable?.let { drawable ->
                val bitmap = drawableToBitmap(drawable, targetSize, targetSize)
                if (bitmap != null) return@withContext bitmap
            }
        }

        null
    }

    private fun drawableToBitmapCenterCrop(drawable: Drawable, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val src = drawable.bitmap
                if (src.width <= targetWidth * 2 && src.height <= targetHeight * 2) {
                    return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
                }
                return centerCropBitmap(src, targetWidth, targetHeight)
            } else {
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: targetWidth
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: targetHeight
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
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
        } catch (_: Exception) {
            null
        }
    }

    private fun centerCropBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height
        val scale = max(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
        val scaledWidth = (srcWidth * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (srcHeight * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, scaledWidth, scaledHeight, true)
        val x = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val y = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(scaled, x, y, targetWidth, targetHeight)
    }

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

    suspend fun getIconBitmap(
        packageName: String,
        candidatePackages: List<String> = emptyList(),
        targetSize: Int = 160
    ): Bitmap? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached

        val bitmap = withContext(Dispatchers.IO) {
            loadAppIconBitmap(packageName, candidatePackages, targetSize)
        }

        bitmap?.let { iconCache.put(packageName, it) }
        return bitmap
    }

    fun preloadIcons(packages: List<String>, delayMs: Long = 0L) {
        viewModelScope.launch(Dispatchers.IO) {
            if (delayMs > 0L) {
                delay(delayMs)
            }
            for (pkg in packages) {
                if (iconCache.get(pkg) == null) {
                    val bitmap = loadAppIconBitmap(pkg, emptyList(), 160)
                    bitmap?.let { iconCache.put(pkg, it) }
                }
            }
        }
    }

    fun clearIconCache() {
        iconCache.evictAll()
    }

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
