package com.example.minimusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minimusic.data.ThemeMode
import com.example.minimusic.ui.navigation.MiniMusicNavGraph
import com.example.minimusic.ui.screens.PermissionScreen
import com.example.minimusic.ui.theme.MiniMusicTheme
import com.example.minimusic.ui.viewmodel.LibraryViewModel
import com.example.minimusic.ui.viewmodel.PlayerViewModel
import com.example.minimusic.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val audioPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val libraryViewModel: LibraryViewModel = viewModel()
            val playerViewModel: PlayerViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()
            val appSettings by settingsViewModel.settings.collectAsState()

            val darkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MiniMusicTheme(darkTheme = darkTheme, dynamicColor = appSettings.dynamicColorEnabled) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var hasPermission by remember {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(this, audioPermission) ==
                                PackageManager.PERMISSION_GRANTED
                        )
                    }

                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted -> hasPermission = granted }

                    LaunchedEffect(Unit) {
                        playerViewModel.connect()
                    }

                    LaunchedEffect(hasPermission) {
                        if (hasPermission) libraryViewModel.loadLibrary()
                    }

                    if (hasPermission) {
                        MiniMusicNavGraph(
                            libraryViewModel = libraryViewModel,
                            playerViewModel = playerViewModel,
                            settingsViewModel = settingsViewModel
                        )
                    } else {
                        PermissionScreen(onGrantClick = { permissionLauncher.launch(audioPermission) })
                    }
                }
            }
        }
    }
}
