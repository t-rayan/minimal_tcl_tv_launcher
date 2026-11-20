package com.narayan.minimaltcltvlauncher.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.narayan.minimaltcltvlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Queries PackageManager for launchable apps on a background thread.
 * Emits a single list via Flow when collected.
 */
class AppRepository(private val context: Context) {

    private val prefsName = "favorites"
    private val favoritesKey = "pinned_packages"

    /**
     * Returns a Flow that emits the list of discovered apps once.
     * Collection happens on the caller's coroutine context;
     * the actual PackageManager query runs on Dispatchers.IO.
     */
    fun getApps(): kotlinx.coroutines.flow.Flow<List<AppInfo>> = flow {
        // Switch to IO thread for blocking PackageManager calls
        val apps = withContext(Dispatchers.IO) { queryApps() }
        emit(apps)
    }.flowOn(Dispatchers.IO) // ensure upstream also runs on IO

    /**
     * Returns the set of favorite (pinned) package names.
     */
    suspend fun getFavorites(): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()
    }

    /**
     * Toggles a package's favorite status.
     */
    suspend fun toggleFavorite(packageName: String): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(favoritesKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(favoritesKey, current).apply()
        current
    }

    /**
     * Builds the app list:
     * 1. Query LEANBACK_LAUNCHER (TV apps with banners)
     * 2. Query LAUNCHER (regular phone/tablet apps)
     * 3. Merge + deduplicate by packageName
     * 4. Convert each ResolveInfo to AppInfo with launch intent
     */
    private fun queryApps(): List<AppInfo> {
        val pm = context.packageManager

        // Intent for TV apps (CATEGORY_LEANBACK_LAUNCHER)
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        // Intent for regular launchable apps (CATEGORY_LAUNCHER)
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // Query both categories
        val leanbackApps = pm.queryIntentActivities(leanbackIntent, PackageManager.MATCH_ALL)
        val launcherApps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        // Merge and deduplicate by packageName (some apps declare both)
        val seenPackages = mutableSetOf<String>()
        val merged = mutableListOf<ResolveInfo>()

        fun addIfNew(list: List<ResolveInfo>) {
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (pkg !in seenPackages) {
                    seenPackages.add(pkg)
                    merged.add(info)
                }
            }
        }

        addIfNew(leanbackApps)
        addIfNew(launcherApps)

        // Convert to AppInfo list
        return merged.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()

            // Get a ready-to-use launch intent for this package
            val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null

            AppInfo(
                packageName = pkg,
                label = label,
                launchIntent = launchIntent
            )
        }
    }
}