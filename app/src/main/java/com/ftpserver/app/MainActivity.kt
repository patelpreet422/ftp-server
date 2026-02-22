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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.delay

// ============== FONTS ==============
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal)
)

// ============== THEME COLORS (Presets) ==============
val PresetColors = listOf(
    Color(0xFF99CC00), // Neon Green (Default)
    Color(0xFF2196F3), // Blue
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
    Color(0xFF795548), // Brown
)

// Legacy colors for onboarding decorations
val NeonBlue = Color(0xFF00FFFF)
val NeonYellow = Color(0xFFFFFF00)
val NeonOrange = Color(0xFFFFA500)

// ============== THEME MODE ==============
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

// ============== THEME PREFERENCES ==============
class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SEED_COLOR = "seed_color"
        private const val KEY_USE_CUSTOM_CREDENTIALS = "use_custom_credentials"
        private const val KEY_CUSTOM_USERNAME = "custom_username"
        private const val KEY_CUSTOM_PASSWORD = "custom_password"
        private const val DEFAULT_COLOR = 0xFF99CC00.toInt()
    }
    
    var themeMode: ThemeMode
        get() = ThemeMode.entries.getOrNull(prefs.getInt(KEY_THEME_MODE, 0)) ?: ThemeMode.SYSTEM
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value.ordinal).apply()
    
    var seedColor: Color
        get() = Color(prefs.getInt(KEY_SEED_COLOR, DEFAULT_COLOR))
        set(value) = prefs.edit().putInt(KEY_SEED_COLOR, value.toArgb()).apply()
    
    var useCustomCredentials: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_CREDENTIALS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_CUSTOM_CREDENTIALS, value).apply()
    
    var customUsername: String
        get() = prefs.getString(KEY_CUSTOM_USERNAME, "android") ?: "android"
        set(value) = prefs.edit().putString(KEY_CUSTOM_USERNAME, value).apply()
    
    var customPassword: String
        get() = prefs.getString(KEY_CUSTOM_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_PASSWORD, value).apply()
}

// ============== MAIN ACTIVITY ==============
class MainActivity : ComponentActivity() {

    private var ftpService: FTPServerService? = null
    private var bound = false
    
    private lateinit var prefs: SharedPreferences
    private lateinit var themePrefs: ThemePreferences
    
    companion object {
        private const val PREFS_NAME = "ftp_server_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    requestManageStoragePermission()
                }
            }
        } else {
            Toast.makeText(this, "storage permissions required", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "notification permission recommended", Toast.LENGTH_LONG).show()
        }
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

        requestNotificationPermission()
        requestStoragePermissions()

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
                        },
                        themePrefs = themePrefs
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission()
            }
        } else {
            storagePermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
                Toast.makeText(
                    this,
                    "please allow 'all files access' permission",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }
}

// ============== DYNAMIC THEME ==============
@Composable
fun FTPServerTheme(
    seedColor: Color,
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val appTypography = Typography(
        displayLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        titleLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
    
    DynamicMaterialTheme(
        seedColor = seedColor,
        useDarkTheme = isDark,
        style = PaletteStyle.TonalSpot,
        typography = appTypography,
        content = content
    )
}

// ============== NAVIGATION ==============
enum class ScreenState {
    Onboarding, Main, Settings
}

@Composable
fun AppNavigation(
    startOnMain: Boolean,
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
    onThemeModeChange: (ThemeMode) -> Unit,
    themePrefs: ThemePreferences
) {
    var currentScreen by remember { 
        mutableStateOf(if (startOnMain) ScreenState.Main else ScreenState.Onboarding) 
    }
    var isRunning by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

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
        OnboardingScreen(onStart = { 
            onOnboardingComplete()
            currentScreen = ScreenState.Main 
        })
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
            onBackClick = { currentScreen = ScreenState.Main },
            themePrefs = themePrefs
        )
    }
}

// ============== ONBOARDING SCREEN ==============
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        RingCluster(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(180.dp))

            Text(
                text = "ftp server",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "transfer files via ftp server directly from your android device",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp)
            )

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "start",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RingCluster(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(300.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 3

        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = save()
            
            rotate(rotation, center.x, center.y)
            drawCircle(
                color = NeonBlue.copy(alpha = 0.6f),
                radius = radius,
                center = center.copy(x = center.x - 40f, y = center.y - 40f),
                style = Stroke(width = 4f)
            )
            
            rotate(120f, center.x, center.y)
            drawCircle(
                color = NeonYellow.copy(alpha = 0.6f),
                radius = radius,
                center = center.copy(x = center.x + 40f, y = center.y - 20f),
                style = Stroke(width = 4f)
            )

            rotate(120f, center.x, center.y)
            drawCircle(
                color = NeonOrange.copy(alpha = 0.6f),
                radius = radius,
                center = center.copy(x = center.x, y = center.y + 50f),
                style = Stroke(width = 4f)
            )
            
            restoreToCount(checkPoint)
        }
    }
}

// ============== MAIN SCREEN ==============
@Composable
fun MainScreen(
    isRunning: Boolean,
    password: String,
    username: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    getIPAddress: () -> String,
    onSettingsClick: () -> Unit
) {
    val transition = updateTransition(targetState = isRunning, label = "ServerState")
    
    val iconColor by transition.animateColor(
        label = "IconColor",
        transitionSpec = { tween(durationMillis = 400) }
    ) { state ->
        if (state) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val glowScale by transition.animateFloat(
        label = "GlowScale",
        transitionSpec = { tween(durationMillis = 800, easing = FastOutSlowInEasing) }
    ) { state ->
        if (state) 1f else 0f
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val glowPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowPulseAlpha"
    )
    
    val bgPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgPulseAlpha"
    )
    
    val glowAlpha = glowScale * glowPulseAlpha
    val bgGlowAlpha = glowScale * bgPulseAlpha
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar with Settings
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding() + 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(60.dp))
            
            // Wifi Icon with Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = primaryColor.copy(alpha = glowAlpha * 0.6f),
                        radius = (size.minDimension / 2) * glowScale
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = "Wi-Fi",
                    tint = if (isRunning) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Center Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryContainerColor.copy(alpha = bgGlowAlpha),
                                primaryContainerColor.copy(alpha = bgGlowAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = 1000f
                        )
                    )
                }
                
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        color = primaryColor.copy(alpha = glowAlpha),
                        radius = (size.minDimension / 2) * glowScale
                    )
                }

                FilledIconButton(
                    onClick = { if (isRunning) onStopServer() else onStartServer() },
                    modifier = Modifier
                        .size(180.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.DarkGray.copy(alpha = 0.3f)
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = (size.minDimension / 2) * glowScale
                            )
                        },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = iconColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = isRunning,
                transitionSpec = {
                    fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                },
                label = "StatusText"
            ) { running ->
                Text(
                    text = if (running) "active" else "disabled",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (running) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Bottom Content
        AnimatedContent(
            targetState = isRunning,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(500)
                )) togetherWith (fadeOut(tween(400)) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(400)
                ))
            },
            contentAlignment = Alignment.Center,
            label = "BottomContent"
        ) { running ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (running) {
                    val ip = getIPAddress()
                    val port = FTPServerService.PORT
                    val user = username.ifEmpty { FTPServerService.DEFAULT_USERNAME }
                    val fullUrl = "ftp://$user:$password@$ip:$port"

                    Text(
                        text = fullUrl,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(fullUrl))
                                Toast
                                    .makeText(context, "copied to clipboard", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(label = "IP", value = ip)
                            DetailItem(label = "Port", value = "$port")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(label = "User", value = user)
                            DetailItem(label = "Pass", value = password)
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "network",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getIPAddress(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============== SETTINGS SCREEN ==============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    seedColor: Color,
    themeMode: ThemeMode,
    onSeedColorChange: (Color) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBackClick: () -> Unit,
    themePrefs: ThemePreferences
) {
    var isAppearanceExpanded by remember { mutableStateOf(true) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(statusBarPadding.calculateTopPadding() + 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(60.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Appearance Section Header
        item {
            SettingsSectionHeader(
                icon = Icons.Rounded.Palette, 
                title = "Appearance",
                isExpanded = isAppearanceExpanded,
                onToggle = { isAppearanceExpanded = !isAppearanceExpanded }
            )
        }
        
        // Appearance Section Content
        item {
            AnimatedVisibility(
                visible = isAppearanceExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column {
                    // Theme Mode
                    ListItem(
                        headlineContent = { 
                            Text("Theme", color = MaterialTheme.colorScheme.onBackground) 
                        },
                        supportingContent = {
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = themeMode == mode,
                                        onClick = { onThemeModeChange(mode) },
                                        label = { 
                                            Text(
                                                when (mode) {
                                                    ThemeMode.SYSTEM -> "System"
                                                    ThemeMode.LIGHT -> "Light"
                                                    ThemeMode.DARK -> "Dark"
                                                }
                                            ) 
                                        }
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    // Accent Color
                    ListItem(
                        headlineContent = { 
                            Text("Accent Color", color = MaterialTheme.colorScheme.onBackground) 
                        },
                        supportingContent = {
                            LazyRow(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(PresetColors) { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (seedColor == color) {
                                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { onSeedColorChange(color) }
                                    )
                                }
                                // Random button
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                val randomColor = Color(
                                                    red = (0..255).random() / 255f,
                                                    green = (0..255).random() / 255f,
                                                    blue = (0..255).random() / 255f
                                                )
                                                onSeedColorChange(randomColor)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Random",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(48.dp)) }
    }
}

@Composable
fun SettingsSectionHeader(
    icon: ImageVector, 
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f,
        animationSpec = tween(300),
        label = "chevronRotation"
    )
    
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotationAngle)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 0.5.dp
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(title, color = MaterialTheme.colorScheme.onBackground) 
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}