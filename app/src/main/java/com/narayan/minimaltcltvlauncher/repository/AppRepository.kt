package com.narayan.minimaltcltvlauncher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.SystemClock
import android.util.Log
import com.narayan.minimaltcltvlauncher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    fun getApps(): Flow<List<AppInfo>> = flow {
        val apps = queryApps()
        emit(apps)
    }.flowOn(Dispatchers.IO)

    suspend fun getFavorites(): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.getStringSet(favoritesKey, emptySet())?.toSet() ?: emptySet()
    }

    suspend fun toggleFavorite(packageName: String): Set<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(favoritesKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet(favoritesKey, current).apply()
        current.toSet()
    }

    private fun queryApps(): List<AppInfo> {
        val startTime = SystemClock.elapsedRealtime()
        val pm = context.packageManager

        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val flags = 0

        val leanbackStart = SystemClock.elapsedRealtime()
        val leanbackApps = pm.queryIntentActivities(leanbackIntent, flags)
        Log.d(
            "StartupTiming",
            "LEANBACK query: ${SystemClock.elapsedRealtime() - leanbackStart}ms (${leanbackApps.size} results)"
        )

        val launcherStart = SystemClock.elapsedRealtime()
        val launcherApps = pm.queryIntentActivities(launcherIntent, flags)
        Log.d(
            "StartupTiming",
            "LAUNCHER query: ${SystemClock.elapsedRealtime() - launcherStart}ms (${launcherApps.size} results)"
        )

        val seenPackages = HashSet<String>(leanbackApps.size + launcherApps.size)
        val merged = ArrayList<ResolveInfo>(leanbackApps.size + launcherApps.size)

        fun addIfNew(list: List<ResolveInfo>) {
            for (info in list) {
                val pkg = info.activityInfo?.packageName ?: continue
                if (seenPackages.add(pkg)) {
                    merged.add(info)
                }
            }
        }

        addIfNew(leanbackApps)
        addIfNew(launcherApps)
        Log.d("StartupTiming", "Merged ${merged.size} unique apps")

        val convertStart = SystemClock.elapsedRealtime()
        val result = ArrayList<AppInfo>(merged.size)

        for (resolveInfo in merged) {
            val activityInfo = resolveInfo.activityInfo ?: continue
            val pkg = activityInfo.packageName
            val activityName = activityInfo.name

            val label = try {
                resolveInfo.loadLabel(pm).toString().ifBlank { pkg }
            } catch (_: Exception) {
                pkg
            }

            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(pkg, activityName)
            }

            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    launchIntent = launchIntent
                )
            )
        }

        Log.d(
            "StartupTiming",
            "AppInfo conversion: ${SystemClock.elapsedRealtime() - convertStart}ms (${result.size} apps)"
        )
        Log.d("StartupTiming", "TOTAL queryApps: ${SystemClock.elapsedRealtime() - startTime}ms")
        return result
    }
}
