package com.sofamaven.musicrecognizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import coil.Coil
import com.sofamaven.musicrecognizer.ui.screens.MainTvScreen
import com.sofamaven.musicrecognizer.ui.screens.SettingsTvScreen
import com.sofamaven.musicrecognizer.ui.theme.AndroidTvMusicRecognizerTheme
import com.sofamaven.musicrecognizer.ui.viewmodel.TvViewModel

enum class TvScreen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: TvViewModel by viewModels()

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecognition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Keep TV screen on constantly for 10-foot media experience
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Clear all file caches and Coil image memory/disk cache on each start as requested
        clearAppCaches()

        setContent {
            AndroidTvMusicRecognizerTheme {
                val uiState by viewModel.uiState.collectAsState()
                val settings by viewModel.settings.collectAsState()
                var currentScreen by remember { mutableStateOf(TvScreen.MAIN) }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TvScreenTransition"
                ) { screen ->
                    when (screen) {
                        TvScreen.MAIN -> {
                            MainTvScreen(
                                uiState = uiState,
                                recognitionMode = settings.recognitionMode,
                                onRecognizeClick = { checkAndRequestMic() },
                                onNavigateToSettings = { currentScreen = TvScreen.SETTINGS }
                            )
                        }
                        TvScreen.SETTINGS -> {
                            SettingsTvScreen(
                                currentSettings = settings,
                                onSaveAndExit = { updatedSettings ->
                                    viewModel.updateSettings(updatedSettings)
                                    currentScreen = TvScreen.MAIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestMic() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startRecognition()
        } else {
            audioPermissionLauncher.launch(permission)
        }
    }

    /**
     * Requirement: Complete automated cache wiping at every app launch.
     * Deletes context.cacheDir and flushes Coil image memory and disk caches.
     */
    private fun clearAppCaches() {
        try {
            // Delete all temporary audio recordings and files in cacheDir
            cacheDir?.deleteRecursively()
        } catch (_: Exception) {}

        try {
            // Clear Coil image caches
            val imageLoader = Coil.imageLoader(this)
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (_: Exception) {}
    }
}
