package com.ftpserver.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    showPermissionDeniedMessage: Boolean = false,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top spacer - pushes content to center
            Spacer(modifier = Modifier.weight(1f))

            // Centered content: RingCluster + Title
            Box(
                contentAlignment = Alignment.Center
            ) {
                RingCluster()
                Text(
                    text = "ftp server",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Bottom spacer - balances with top
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "transfer files via ftp server directly from your android device",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 32.dp)
            )

            // Animated Start Button
            AnimatedStartButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Permission denied overlay - floats on top without affecting layout
        AnimatedVisibility(
            visible = showPermissionDeniedMessage,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .padding(horizontal = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "File permission is required for FTP server to work. Please grant access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

/**
 * Animated Start Button with arrow animation.
 */
@Composable
private fun AnimatedStartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrowAnim")

    // Animate from 0 to 1 and back (represents arrow moving right then left)
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    val maxArrowOffset = 8.dp // Maximum offset for arrow to the right

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
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

            // Animated arrow icon - moves right and left
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = "Start",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.offset {
                    IntOffset(
                        x = (progress * maxArrowOffset.toPx()).roundToInt(),
                        y = 0
                    )
                }
            )
        }
    }
}

/**
 * A decorative cluster of rings that uses theme colors.
 */
@Composable
private fun RingCluster(modifier: Modifier = Modifier) {
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

    val ringColor1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val ringColor2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    val ringColor3 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)

    Canvas(modifier = modifier.size(300.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 3

        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = save()

            rotate(rotation, center.x, center.y)
            drawCircle(
                color = ringColor1,
                radius = radius,
                center = center.copy(x = center.x - 40f, y = center.y - 40f),
                style = Stroke(width = 4f)
            )

            rotate(120f, center.x, center.y)
            drawCircle(
                color = ringColor2,
                radius = radius,
                center = center.copy(x = center.x + 40f, y = center.y - 20f),
                style = Stroke(width = 4f)
            )

            rotate(120f, center.x, center.y)
            drawCircle(
                color = ringColor3,
                radius = radius,
                center = center.copy(x = center.x, y = center.y + 50f),
                style = Stroke(width = 4f)
            )

            restoreToCount(checkPoint)
        }
    }
}