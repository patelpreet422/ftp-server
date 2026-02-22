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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Fonts
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal)
)

// Colors
val NeonGreen = Color(0xFF99CC00)
val DeepCharcoal = Color(0xFF1E1E1E)
val SurfaceBlack = Color(0xFF121212)
val MutedGrey = Color(0xFF808080)
val NeonBlue = Color(0xFF00FFFF)
val NeonYellow = Color(0xFFFFFF00)
val NeonOrange = Color(0xFFFFA500)

class MainActivity : ComponentActivity() {

    private var ftpService: FTPServerService? = null
    private var bound = false
    
    private lateinit var prefs: SharedPreferences
    
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
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        requestNotificationPermission()
        requestStoragePermissions()

        setContent {
            FTPServerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startOnMain = onboardingCompleted,
                        onOnboardingComplete = { markOnboardingCompleted() },
                        onStartServer = { password -> startServer(password) },
                        onStopServer = { stopServer() },
                        getServerState = { ftpService?.isServerRunning() ?: false },
                        getIPAddress = { FTPServerService.getIPAddress() },
                        getPassword = { ftpService?.getPassword() }
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

    private fun startServer(password: String) {
        val intent = Intent(this, FTPServerService::class.java).apply {
            putExtra(FTPServerService.EXTRA_PASSWORD, password)
        }
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        android.os.Handler(mainLooper).postDelayed({
            ftpService?.startFTPServer(password)
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

@Composable
fun FTPServerTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = NeonGreen,
        secondary = MutedGrey,
        background = SurfaceBlack,
        surface = DeepCharcoal,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = TextStyle(
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp
            ),
            headlineMedium = TextStyle(
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            )
        ),
        content = content
    )
}

enum class ScreenState {
    Onboarding, Main
}

@Composable
fun AppNavigation(
    startOnMain: Boolean,
    onOnboardingComplete: () -> Unit,
    onStartServer: (String) -> Unit,
    onStopServer: () -> Unit,
    getServerState: () -> Boolean,
    getIPAddress: () -> String,
    getPassword: () -> String?
) {
    var currentScreen by remember { 
        mutableStateOf(if (startOnMain) ScreenState.Main else ScreenState.Onboarding) 
    }
    var isRunning by remember { mutableStateOf(false) }
    
    // Password state: Initially empty, filled from service or on start
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            isRunning = getServerState()
            // If server is running, get password from service
            if (isRunning) {
                getPassword()?.let { password = it }
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
            onStartServer = {
                // Generate new password when starting fresh
                val newPassword = FTPServerService.generateRandomPassword()
                password = newPassword
                onStartServer(newPassword)
            },
            onStopServer = { onStopServer() },
            getIPAddress = getIPAddress
        )
    }
}

@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative Rings
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
            Spacer(modifier = Modifier.height(180.dp)) // Space for rings

            Text(
                text = "ftp server",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "transfer files via ftp server directly from your android device",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 48.dp)
            )

            // Start Button
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1B5E20), // Dark green
                                NeonGreen.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "start",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "Start",
                        tint = Color.White
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
            
            // Ring 1 - Neon Blue - Rotated
            rotate(rotation, center.x, center.y)
            drawCircle(
                color = NeonBlue.copy(alpha = 0.6f),
                radius = radius,
                center = center.copy(x = center.x - 40f, y = center.y - 40f),
                style = Stroke(width = 4f)
            )
            
            // Ring 2 - Neon Yellow
            rotate(120f, center.x, center.y)
            drawCircle(
                color = NeonYellow.copy(alpha = 0.6f),
                radius = radius,
                center = center.copy(x = center.x + 40f, y = center.y - 20f),
                style = Stroke(width = 4f)
            )

            // Ring 3 - Neon Orange
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

@Composable
fun MainScreen(
    isRunning: Boolean,
    password: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    getIPAddress: () -> String
) {
    val transition = updateTransition(targetState = isRunning, label = "ServerState")
    
    val buttonColor by transition.animateColor(
        label = "ButtonColor",
        transitionSpec = { tween(durationMillis = 600) }
    ) { state ->
        if (state) NeonGreen else Color.DarkGray.copy(alpha = 0.3f)
    }
    
    val iconColor by transition.animateColor(
        label = "IconColor",
        transitionSpec = { tween(durationMillis = 400) }
    ) { state ->
        if (state) Color.White else MutedGrey
    }
    
    // Glow scale transition (0 to 1 when active)
    val glowScale by transition.animateFloat(
        label = "GlowScale",
        transitionSpec = { tween(durationMillis = 800, easing = FastOutSlowInEasing) }
    ) { state ->
        if (state) 1f else 0f
    }

    // Glow Animation - Pulse (only visible when active)
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
    
    // Background pulse (slower, milder)
    val bgPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgPulseAlpha"
    )
    
    // Combined glow alpha (scale controls visibility, pulse adds effect)
    val glowAlpha = glowScale * glowPulseAlpha
    val bgGlowAlpha = glowScale * bgPulseAlpha
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar - Wifi Icon with Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            // Wifi Glow Effect
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(
                    color = NeonGreen.copy(alpha = glowAlpha * 0.6f),
                    radius = (size.minDimension / 2) * glowScale
                )
            }
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = "Wi-Fi",
                tint = if (isRunning) NeonGreen else MutedGrey,
                modifier = Modifier.size(28.dp)
            )
        }

        // Center Content - Stable Position
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Power Button with Background Glow
            Box(contentAlignment = Alignment.Center) {
                // Background Glow - radiates from button center with pulse
                Canvas(
                    modifier = Modifier.size(220.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1B5E20).copy(alpha = bgGlowAlpha),
                                Color(0xFF1B5E20).copy(alpha = bgGlowAlpha * 0.3f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = 1000f
                        )
                    )
                }
                
                    // Glow Effect - Always rendered but scale/alpha animated
                    Canvas(modifier = Modifier.size(220.dp)) {
                        drawCircle(
                            color = NeonGreen.copy(alpha = glowAlpha),
                            radius = (size.minDimension / 2) * glowScale
                        )
                    }

                FilledIconButton(
                    onClick = { if (isRunning) onStopServer() else onStartServer() },
                    modifier = Modifier
                        .size(180.dp)
                        .drawBehind {
                            // Base (Disabled state)
                            drawCircle(
                                color = Color.DarkGray.copy(alpha = 0.3f)
                            )
                            // Active fill (expands from center)
                            drawCircle(
                                color = NeonGreen,
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

            // Status Text with fade
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
                    color = if (running) Color.White else MutedGrey,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Bottom Content - Scale + Fade Transition for depth effect
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
                    val user = FTPServerService.USERNAME
                    val fullUrl = "ftp://$user:$password@$ip:$port"

                    // Full FTP URL (Large, Green, Copyable)
                    Text(
                        text = fullUrl,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .clickable {
                                clipboardManager.setText(AnnotatedString(fullUrl))
                                Toast.makeText(context, "copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Details Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                    ) {
                        // Row 1: IP | Port
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailItem(label = "IP", value = ip)
                            DetailItem(label = "Port", value = "$port")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Row 2: User | Pass
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
                            color = MutedGrey
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getIPAddress(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
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
            color = MutedGrey
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
