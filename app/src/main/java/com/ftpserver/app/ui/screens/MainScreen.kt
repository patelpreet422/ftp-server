package com.ftpserver.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ftpserver.app.FTPServerService

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
                .padding(
                    top = statusBarPadding.calculateTopPadding() + 8.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(48.dp))

            // Wifi Icon with Glow
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
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
                modifier = Modifier.size(48.dp)
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
                            // Use surfaceVariant for the inactive state background
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