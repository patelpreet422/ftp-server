package com.ftpserver.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ftpserver.app.ui.screens.MainScreen
import com.ftpserver.app.ui.screens.OnboardingScreen
import com.ftpserver.app.ui.screens.SettingsScreen
import com.ftpserver.app.ui.screens.ThemeMode
import com.ftpserver.app.ui.theme.DefaultSeedColor
import com.ftpserver.app.ui.theme.FTPServerTheme
import kotlinx.coroutines.delay

// ============== THEME PREFERENCES ==============
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SEED_COLOR = "seed_color"
        private val DEFAULT_COLOR_ARGB = DefaultSeedColor.toArgb()
    }

    var themeMode: ThemeMode
        get() = ThemeMode.entries.getOrNull(prefs.getInt(KEY_THEME_MODE, 0)) ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value.ordinal).apply()

    var seedColor: Color
        get() = Color(prefs.getInt(KEY_SEED_COLOR, DEFAULT_COLOR_ARGB))
        set(value) = prefs.edit().putInt(KEY_SEED_COLOR, value.toArgb()).apply()
}

// ============== MAIN ACTIVITY ==============
class MainActivity : ComponentActivity() {

    private var ftpService: FTPServerService? = null
    private var bound = false

    private lateinit var prefs: SharedPreferences
    private lateinit var themePrefs: ThemePreferences

    // Callback to invoke after permission result
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    companion object {
        private const val PREFS_NAME = "ftp_server_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    // For Android < 11 (legacy storage permissions)
    private val storagePermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.all { it.value }
            onPermissionResult?.invoke(granted)
        }

    // For Android 11+ (MANAGE_EXTERNAL_STORAGE) - uses startActivityForResult pattern
    private val manageStoragePermissionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Check if permission was granted after returning from settings
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
            onPermissionResult?.invoke(granted)
        }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FTPServerService.LocalBinder
            ftpService = binder.getService()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ftpService = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        themePrefs = ThemePreferences(this)
        val onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        setContent {
            var seedColor by remember { mutableStateOf(themePrefs.seedColor) }
            var themeMode by remember { mutableStateOf(themePrefs.themeMode) }

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            FTPServerTheme(
                seedColor = seedColor,
                isDark = isDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startOnMain = onboardingCompleted,
                        onRequestStoragePermission = { callback ->
                            requestStoragePermission(callback)
                        },
                        onOnboardingComplete = { markOnboardingCompleted() },
                        onStartServer = { username, password -> startServer(username, password) },
                        onStopServer = { stopServer() },
                        getServerState = { ftpService?.isServerRunning() ?: false },
                        getIPAddress = { FTPServerService.getIPAddress() },
                        getPassword = { ftpService?.getPassword() },
                        getUsername = { ftpService?.getUsername() },
                        seedColor = seedColor,
                        themeMode = themeMode,
                        onSeedColorChange = { color ->
                            seedColor = color
                            themePrefs.seedColor = color
                        },
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            themePrefs.themeMode = mode
                        }
                    )
                }
            }
        }
    }

    private fun markOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    override fun onStart() {
        super.onStart()
        Intent(this, FTPServerService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
    }

    private fun startServer(username: String, password: String) {
        val intent = Intent(this, FTPServerService::class.java).apply {
            putExtra(FTPServerService.EXTRA_PASSWORD, password)
            putExtra(FTPServerService.EXTRA_USERNAME, username)
        }
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        android.os.Handler(mainLooper).postDelayed({
            ftpService?.startFTPServer(username, password)
        }, 100)
    }

    private fun stopServer() {
        ftpService?.stopFTPServer()
    }

    /**
     * Request storage permission. The callback will be invoked with the result.
     */
    private fun requestStoragePermission(callback: (Boolean) -> Unit) {
        onPermissionResult = callback

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE via Settings
            if (Environment.isExternalStorageManager()) {
                callback(true)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    manageStoragePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStoragePermissionLauncher.launch(intent)
                }
            }
        } else {
            // Android 10 and below
            storagePermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}

// ============== NAVIGATION ==============
private enum class ScreenState {
    Onboarding, Main, Settings
}

@Composable
private fun AppNavigation(
    startOnMain: Boolean,
    onRequestStoragePermission: (callback: (Boolean) -> Unit) -> Unit,
    onOnboardingComplete: () -> Unit,
    onStartServer: (String, String) -> Unit,
    onStopServer: () -> Unit,
    getServerState: () -> Boolean,
    getIPAddress: () -> String,
    getPassword: () -> String?,
    getUsername: () -> String?,
    seedColor: Color,
    themeMode: ThemeMode,
    onSeedColorChange: (Color) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    var currentScreen by remember {
        mutableStateOf(if (startOnMain) ScreenState.Main else ScreenState.Onboarding)
    }
    var isRunning by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showPermissionDeniedMessage by remember { mutableStateOf(false) }

    // Auto-dismiss permission denied message after 5 seconds
    LaunchedEffect(showPermissionDeniedMessage) {
        if (showPermissionDeniedMessage) {
            delay(5000)
            showPermissionDeniedMessage = false
        }
    }

    // Handle system back button for Settings screen
    BackHandler(enabled = currentScreen == ScreenState.Settings) {
        currentScreen = ScreenState.Main
    }

    LaunchedEffect(Unit) {
        while (true) {
            isRunning = getServerState()
            if (isRunning) {
                getPassword()?.let { password = it }
                getUsername()?.let { username = it }
            }
            delay(500)
        }
    }

    AnimatedVisibility(
        visible = currentScreen == ScreenState.Onboarding,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        OnboardingScreen(
            showPermissionDeniedMessage = showPermissionDeniedMessage,
            onStart = {
                showPermissionDeniedMessage = false
                onRequestStoragePermission { granted ->
                    if (granted) {
                        onOnboardingComplete()
                        currentScreen = ScreenState.Main
                    } else {
                        showPermissionDeniedMessage = true
                    }
                }
            }
        )
    }

    AnimatedVisibility(
        visible = currentScreen == ScreenState.Main,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        MainScreen(
            isRunning = isRunning,
            password = password,
            username = username,
            onStartServer = {
                val startUsername = FTPServerService.DEFAULT_USERNAME
                val startPassword = FTPServerService.generateRandomPassword()
                username = startUsername
                password = startPassword
                onStartServer(startUsername, startPassword)
            },
            onStopServer = { onStopServer() },
            getIPAddress = getIPAddress,
            onSettingsClick = { currentScreen = ScreenState.Settings }
        )
    }

    AnimatedVisibility(
        visible = currentScreen == ScreenState.Settings,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SettingsScreen(
            seedColor = seedColor,
            themeMode = themeMode,
            onSeedColorChange = onSeedColorChange,
            onThemeModeChange = onThemeModeChange,
            onBackClick = { currentScreen = ScreenState.Main }
        )
    }
}