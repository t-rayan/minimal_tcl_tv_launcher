package com.narayan.minimaltcltvlauncher.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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

/**
 * ViewModel that holds the list of discovered apps.
 * Uses StateFlow to expose a single observable list to the UI.
 * Loads apps once on init via AppRepository.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

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
        viewModelScope.launch {
            repository.getApps()
                // Collect each emission (there's only one) and update state
                .onEach { appList ->
                    _apps.value = appList
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