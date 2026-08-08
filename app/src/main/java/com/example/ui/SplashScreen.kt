package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PremiumTitleFont
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 60f
        ),
        label = "progress"
    )

    val sweepAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1800, easing = FastOutSlowInEasing),
        label = "sweep"
    )

    val fillAlphaAnim by animateFloatAsState(
        targetValue = if (sweepAnim > 0.4f) 1f else 0f,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "fillAlpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
        delay(2400)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)), // Dark theme solid background
        contentAlignment = Alignment.Center
    ) {
        val sweepWidth = 800f
        val currentX = sweepAnim * (sweepWidth * 2) - (sweepWidth / 2f)

        val strokeBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.Transparent,
                Color.White.copy(alpha = 0.8f),
                Color.White,
                Color.White.copy(alpha = 0.8f),
                Color.Transparent,
                Color.Transparent
            ),
            start = androidx.compose.ui.geometry.Offset(currentX - sweepWidth / 2f, 0f),
            end = androidx.compose.ui.geometry.Offset(currentX + sweepWidth / 2f, 0f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .graphicsLayer {
                    val scale = 0.8f + (0.2f * progress)
                    scaleX = scale
                    scaleY = scale
                    translationY = 50f * (1f - progress)
                    alpha = progress.coerceIn(0f, 1f)
                }
        ) {
            // Circular App Logo with optimized proportions
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Zniwatch Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Title under logo
            Text(
                text = "Zniwatch",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                fontFamily = PremiumTitleFont,
                color = Color.White.copy(alpha = fillAlphaAnim)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Anime Streaming & Tracking",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = PremiumTitleFont,
                color = Color.White.copy(alpha = fillAlphaAnim * 0.7f)
            )
        }
    }
}
