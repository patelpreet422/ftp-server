package com.ftpserver.app.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ftpserver.app.FTPServerService
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates a QR code Bitmap from the given content string.
 * Uses transparent background and black foreground (to be tinted later).
 * This function is designed to be called from a background thread.
 */
private fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.TRANSPARENT
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // QR Code overlay state
    var showQrCode by remember { mutableStateOf(false) }

    // Build FTP URL for QR code
    val ip = getIPAddress()
    val port = FTPServerService.PORT
    val user = username.ifEmpty { FTPServerService.DEFAULT_USERNAME }
    val fullUrl = "ftp://$user:$password@$ip:$port"

    // Generate QR Code Bitmap in background (avoids main thread lag)
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(fullUrl) {
        qrBitmap = withContext(Dispatchers.IO) {
            generateQrCodeBitmap(fullUrl, 512)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar with Settings only
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarPadding.calculateTopPadding() + 8.dp,
                        start = 12.dp,
                        end = 12.dp
                    )
            ) {
                // Wifi Icon centered
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
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

                // Settings Button (Right)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
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
                                drawCircle(color = surfaceColor.copy(alpha = 0.3f))
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
                        // QR Code Icon - only visible once bitmap is generated
                        AnimatedVisibility(
                            visible = qrBitmap != null,
                            enter = fadeIn(tween(300)) + scaleIn(tween(300)),
                            exit = fadeOut(tween(200)) + scaleOut(tween(200))
                        ) {
                            IconButton(
                                onClick = { showQrCode = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.QrCode2,
                                    contentDescription = "QR Code",
                                    tint = primaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 2
                        ) {
                            DetailItem(label = "ip", value = ip)
                            DetailItem(label = "port", value = "$port")
                            DetailItem(label = "user", value = user)
                            DetailItem(label = "pass", value = password)
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
                                text = ip,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // QR Code Overlay with solid theme background
        AnimatedVisibility(
            visible = showQrCode,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            // Click anywhere to close
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { showQrCode = false },
                contentAlignment = Alignment.Center
            ) {
                // QR Code with pop animation (originates from bottom-center)
                // No container background - dots float directly on frosted glass
                AnimatedVisibility(
                    visible = showQrCode,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        transformOrigin = TransformOrigin(0.5f, 0.85f)
                    ) + fadeIn(tween(300)),
                    exit = scaleOut(
                        animationSpec = tween(250),
                        transformOrigin = TransformOrigin(0.5f, 0.85f)
                    ) + fadeOut(tween(200))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(24.dp)
                            // Block clicks on the QR code from closing the overlay
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* consume click */ }
                    ) {
                        // QR Code Image - tinted with primary color, transparent background
                        qrBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "FTP Server QR Code",
                                modifier = Modifier.size(280.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(primaryColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Close button (Circle X)
                        FilledIconButton(
                            onClick = { showQrCode = false },
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
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