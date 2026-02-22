package com.ftpserver.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Google Sans Flex font loaded from local TTF file
val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex, FontWeight.Normal)
)

class MainActivity : ComponentActivity() {
    
    private var ftpService: FTPServerService? = null
    private var bound = false
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // Permissions granted, check for MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    requestManageStoragePermission()
                }
            }
        } else {
            Toast.makeText(this, "Storage permissions required", Toast.LENGTH_LONG).show()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission recommended for status updates", Toast.LENGTH_LONG).show()
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
        
        requestNotificationPermission()
        requestStoragePermissions()
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            
            FTPServerTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FTPServerScreen(
                        onStartServer = { startServer() },
                        onStopServer = { stopServer() },
                        getServerState = { ftpService?.isServerRunning() ?: false },
                        getIPAddress = { ftpService?.getIPAddress() ?: "Unknown" },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
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
    
    private fun startServer() {
        val intent = Intent(this, FTPServerService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        android.os.Handler(mainLooper).postDelayed({
            ftpService?.startFTPServer()
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
                    "Please allow 'All files access' permission",
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
fun FTPServerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Animated color transitions
    val animationSpec = tween<Color>(durationMillis = 500, easing = FastOutSlowInEasing)
    
    val primary by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF7A9D54) else Color(0xFF4C662B),
        animationSpec = animationSpec,
        label = "primary"
    )
    val secondary by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFFB5C99A) else Color(0xFF7A9D54),
        animationSpec = animationSpec,
        label = "secondary"
    )
    val tertiary by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF4C662B) else Color(0xFFB5C99A),
        animationSpec = animationSpec,
        label = "tertiary"
    )
    val background by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF1C1B1F) else Color(0xFFF9FAEF),
        animationSpec = animationSpec,
        label = "background"
    )
    val surface by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFF2C2C2C) else Color(0xFFFFFFFF),
        animationSpec = animationSpec,
        label = "surface"
    )
    val onBackground by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
        animationSpec = animationSpec,
        label = "onBackground"
    )
    val onSurface by animateColorAsState(
        targetValue = if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF1C1B1F),
        animationSpec = animationSpec,
        label = "onSurface"
    )
    
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color(0xFF1C1B1F),
            onBackground = onBackground,
            onSurface = onSurface,
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            background = background,
            surface = surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = onBackground,
            onSurface = onSurface,
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun FTPServerScreen(
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    getServerState: () -> Boolean,
    getIPAddress: () -> String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Dynamic sizing based on screen size
    val titleSize = (screenWidth.value * 0.08f).coerceIn(24f, 36f).sp
    val buttonSize = (screenHeight.value * 0.18f).coerceIn(120f, 160f).dp
    val horizontalPadding = (screenWidth.value * 0.05f).coerceIn(16f, 24f).dp
    val verticalPadding = (screenHeight.value * 0.03f).coerceIn(16f, 32f).dp
    
    LaunchedEffect(Unit) {
        while (true) {
            isRunning = getServerState()
            kotlinx.coroutines.delay(500)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title and theme toggle row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "ftp server",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.align(Alignment.Center)
                )
                
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (isRunning) {
                // When server is running: button centered, details in scrollable area
                Spacer(modifier = Modifier.weight(1f))
                
                // Dynamic button - centered
                ServerButton(
                    isRunning = isRunning,
                    onStartServer = onStartServer,
                    onStopServer = onStopServer,
                    buttonSize = buttonSize
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Scrollable connection details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    CompactConnectionDetailsCard(
                        ipAddress = getIPAddress(),
                        port = FTPServerService.PORT.toString(),
                        username = "android",
                        password = "android123"
                    )
                }
            } else {
                // When server is stopped: button centered in full available space
                Spacer(modifier = Modifier.weight(1f))
                
                // Dynamic button - centered
                ServerButton(
                    isRunning = isRunning,
                    onStartServer = onStartServer,
                    onStopServer = onStopServer,
                    buttonSize = buttonSize
                )
                
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ServerButton(
    isRunning: Boolean,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    buttonSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    val ringSize = buttonSize + 20.dp
    val textSize = (buttonSize.value * 0.17f).coerceIn(18f, 26f).sp
    val subTextSize = (buttonSize.value * 0.085f).coerceIn(10f, 14f).sp
    
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        if (isRunning) {
            Canvas(modifier = Modifier.size(ringSize + 20.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = (size.minDimension / 2) * 0.9f
                val animatedRadius = baseRadius * scale
                
                // More visible ripple effect - red color matching the stop button
                drawCircle(
                    color = Color(0xFFEF5350).copy(alpha = alpha * 2.5f),
                    radius = animatedRadius,
                    center = center,
                    style = Stroke(width = 4f)
                )
            }
        }
        
        Button(
            onClick = { if (isRunning) onStopServer() else onStartServer() },
            modifier = Modifier
                .size(buttonSize)
                .shadow(
                    elevation = if (isRunning) 6.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = if (isRunning) Color(0xFFEF5350).copy(alpha = 0.4f) 
                              else Color(0xFF66BB6A).copy(alpha = 0.4f)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color(0xFFEF5350) else Color(0xFF66BB6A)
            ),
            contentPadding = PaddingValues(0.dp),
            shape = CircleShape
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isRunning) "stop" else "start",
                    fontSize = textSize,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = GoogleSansFlex,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "server",
                    fontSize = subTextSize,
                    fontWeight = FontWeight.Medium,
                    fontFamily = GoogleSansFlex,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
fun CompactStatusCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "server stopped",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "tap start to share files",
                fontSize = 12.sp,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CompactConnectionDetailsCard(
    ipAddress: String,
    port: String,
    username: String,
    password: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "connection details",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactDetailItem(
                    label = "ip",
                    value = ipAddress,
                    modifier = Modifier.weight(1f),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(ipAddress))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
                CompactDetailItem(
                    label = "port",
                    value = port,
                    modifier = Modifier.weight(0.6f),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(port))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactDetailItem(
                    label = "user",
                    value = username,
                    modifier = Modifier.weight(1f),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(username))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
                CompactDetailItem(
                    label = "password",
                    value = password,
                    modifier = Modifier.weight(1f),
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(password))
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            val fullConnection = "ftp://$username:$password@$ipAddress:$port"
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fullConnection))
                    Toast.makeText(context, "full url copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy All",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "copy full url",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun CompactDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onCopy),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ConnectionDetailsCard(
    ipAddress: String,
    port: String,
    username: String,
    password: String
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Connection Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            DetailItem(
                label = "IP Address",
                value = ipAddress,
                onCopy = {
                        clipboardManager.setText(AnnotatedString(ipAddress))
                        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                }
            )
            
            DetailItem(
                label = "Port",
                value = port,
                onCopy = {
                        clipboardManager.setText(AnnotatedString(port))
                        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                }
            )
            
            DetailItem(
                label = "Username",
                value = username,
                onCopy = {
                        clipboardManager.setText(AnnotatedString(username))
                        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                }
            )
            
            DetailItem(
                label = "Password",
                value = password,
                onCopy = {
                        clipboardManager.setText(AnnotatedString(password))
                        Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                }
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            val fullConnection = "ftp://$username:$password@$ipAddress:$port"
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(fullConnection))
                    Toast.makeText(context, "Full URL copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy All",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Copy Full URL",
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GoogleSansFlex,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, onCopy: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}