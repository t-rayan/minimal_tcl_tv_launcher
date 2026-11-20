package com.narayan.minimaltcltvlauncher.model

import android.content.Intent

/**
 * Holds metadata for a launchable app.
 * - [packageName]: Unique identifier (e.g., "com.netflix.ninja")
 * - [label]: User-visible name (e.g., "Netflix")
 * - [launchIntent]: Ready-to-fire Intent from PackageManager
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val launchIntent: Intent
)