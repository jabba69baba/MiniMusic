package com.example.minimusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minimusic.data.ThemeMode
import com.example.minimusic.ui.navigation.MiniMusicNavGraph
import com.example.minimusic.ui.screens.PermissionScreen
import com.example.minimusic.ui.theme.MiniMusicTheme
import com.example.minimusic.ui.viewmodel.LibraryViewModel
import com.example.minimusic.ui.viewmodel.PlayerViewModel
import com.example.minimusic.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.example.minimusic.extra.OPEN_PLAYER"
        const val EXTRA_WIDGET_OPEN = "com.example.minimusic.extra.WIDGET_OPEN"
    }

    private var openPlayerFromWidget by mutableStateOf(false)

    private val audioPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        var firstComposeFrameReady = false
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !firstComposeFrameReady }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.view.animate()
                .alpha(0f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(220L)
                .withEndAction { splashScreenViewProvider.remove() }
                .start()
        }

        super.onCreate(savedInstanceState)
        openPlayerFromWidget = intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)
        if (intent.getBooleanExtra(EXTRA_WIDGET_OPEN, false)) {
            overridePendingTransition(R.anim.widget_open_enter, R.anim.widget_open_exit)
        }

        setContent {
            SideEffect { firstComposeFrameReady = true }

            val libraryViewModel: LibraryViewModel = viewModel()
            val playerViewModel: PlayerViewModel = viewModel()
            val settingsViewModel: SettingsViewModel = viewModel()
            val appSettings by settingsViewModel.settings.collectAsState()

            val darkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MiniMusicTheme(
                darkTheme = darkTheme,
                dynamicColor = appSettings.dynamicColorEnabled,
                amoledBlack = appSettings.amoledBlackMode
            ) {
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
                            settingsViewModel = settingsViewModel,
                            openPlayerFromWidget = openPlayerFromWidget
                        )
                    } else {
                        PermissionScreen(onGrantClick = { permissionLauncher.launch(audioPermission) })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) {
            openPlayerFromWidget = true
        }
        if (intent.getBooleanExtra(EXTRA_WIDGET_OPEN, false)) {
            overridePendingTransition(R.anim.widget_open_enter, R.anim.widget_open_exit)
        }
    }
}
