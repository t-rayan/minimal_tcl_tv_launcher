package com.narayan.minimaltcltvlauncher

import android.os.Bundle
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
        super.onCreate(savedInstanceState)
        setContent {
            MinimalTCLTVLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    // Use viewModel() to get/create HomeViewModel scoped to this Activity
                    val factory = remember { HomeViewModel.Factory(application) }
                    HomeScreen(viewModel = viewModel(factory = factory))
                }
            }
        }
    }
}