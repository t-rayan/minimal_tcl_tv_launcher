package com.narayan.minimaltcltvlauncher

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.narayan.minimaltcltvlauncher.ui.home.HomeScreen
import com.narayan.minimaltcltvlauncher.ui.theme.MinimalTCLTVLauncherTheme
import com.narayan.minimaltcltvlauncher.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = SystemClock.elapsedRealtime()
        Log.d("StartupTiming", "MainActivity.onCreate entry: ${SystemClock.elapsedRealtime() - startTime}ms")
        super.onCreate(savedInstanceState)
        Log.d("StartupTiming", "After super.onCreate: ${SystemClock.elapsedRealtime() - startTime}ms")
        setContent {
            Log.d("StartupTiming", "setContent start: ${SystemClock.elapsedRealtime() - startTime}ms")
            MinimalTCLTVLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Log.d("StartupTiming", "HomeScreen composition start: ${SystemClock.elapsedRealtime() - startTime}ms")
                    HomeScreen()
                }
            }
            Log.d("StartupTiming", "setContent end: ${SystemClock.elapsedRealtime() - startTime}ms")
        }
    }
}