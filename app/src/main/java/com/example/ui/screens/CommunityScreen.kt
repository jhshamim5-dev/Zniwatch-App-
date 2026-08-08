package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommunityScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 12.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Community",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Connect With Us",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Join our community platforms to get updates, report issues, and chat with other members.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Telegram Button
                CommunityButton(
                    title = "Telegram",
                    subtitle = "Join our official Telegram channel",
                    icon = { TelegramLogo(modifier = Modifier.size(26.dp)) },
                    onClick = { openUrl("https://t.me/ZniWatch") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Discord Button
                CommunityButton(
                    title = "Discord",
                    subtitle = "Join our Discord server & community",
                    icon = { DiscordLogo(modifier = Modifier.size(26.dp)) },
                    onClick = { openUrl("https://discord.gg/KK7h44Hyc") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // GitHub Button
                CommunityButton(
                    title = "GitHub",
                    subtitle = "Follow developer & source updates",
                    icon = { GitHubLogo(modifier = Modifier.size(26.dp)) },
                    onClick = { openUrl("https://github.com/Jhshamim7") }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun CommunityButton(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF12121A),
        border = BorderStroke(1.dp, Color(0xFF222232)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF1C1C28), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TelegramLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.88f, h * 0.15f)
            lineTo(w * 0.12f, h * 0.44f)
            cubicTo(w * 0.05f, h * 0.47f, w * 0.05f, h * 0.52f, w * 0.11f, h * 0.54f)
            lineTo(w * 0.31f, h * 0.60f)
            lineTo(w * 0.77f, h * 0.31f)
            cubicTo(w * 0.79f, h * 0.30f, w * 0.81f, h * 0.31f, w * 0.79f, h * 0.33f)
            lineTo(w * 0.42f, h * 0.66f)
            lineTo(w * 0.42f, h * 0.82f)
            cubicTo(w * 0.42f, h * 0.88f, w * 0.47f, h * 0.90f, w * 0.51f, h * 0.86f)
            lineTo(w * 0.61f, h * 0.76f)
            lineTo(w * 0.81f, h * 0.91f)
            cubicTo(w * 0.88f, h * 0.95f, w * 0.93f, h * 0.92f, w * 0.95f, h * 0.84f)
            lineTo(w * 0.99f, h * 0.23f)
            cubicTo(w * 1.01f, h * 0.14f, w * 0.95f, h * 0.10f, w * 0.88f, h * 0.15f)
            close()
        }
        drawPath(path = path, color = Color.White, style = Fill)
    }
}

@Composable
private fun DiscordLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.81f, h * 0.22f)
            cubicTo(w * 0.75f, h * 0.19f, w * 0.68f, h * 0.17f, w * 0.61f, h * 0.16f)
            cubicTo(w * 0.60f, h * 0.18f, w * 0.58f, h * 0.21f, w * 0.57f, h * 0.24f)
            cubicTo(w * 0.49f, h * 0.23f, w * 0.41f, h * 0.23f, h * 0.33f, h * 0.24f)
            cubicTo(w * 0.32f, h * 0.21f, w * 0.30f, h * 0.18f, w * 0.29f, h * 0.16f)
            cubicTo(w * 0.22f, h * 0.17f, w * 0.15f, h * 0.19f, w * 0.09f, h * 0.22f)
            cubicTo(w * -0.04f, h * 0.41f, w * -0.01f, h * 0.60f, w * 0.01f, h * 0.78f)
            cubicTo(w * 0.09f, h * 0.84f, w * 0.18f, h * 0.88f, w * 0.27f, h * 0.91f)
            cubicTo(w * 0.29f, h * 0.88f, w * 0.31f, h * 0.85f, w * 0.33f, h * 0.81f)
            cubicTo(w * 0.30f, h * 0.80f, w * 0.27f, h * 0.79f, w * 0.24f, h * 0.77f)
            cubicTo(w * 0.25f, h * 0.76f, w * 0.25f, h * 0.75f, w * 0.26f, h * 0.74f)
            cubicTo(w * 0.42f, h * 0.81f, w * 0.59f, h * 0.81f, w * 0.74f, h * 0.74f)
            cubicTo(w * 0.75f, h * 0.75f, w * 0.75f, h * 0.76f, w * 0.76f, h * 0.77f)
            cubicTo(w * 0.73f, h * 0.79f, w * 0.70f, h * 0.80f, w * 0.67f, h * 0.81f)
            cubicTo(w * 0.69f, h * 0.85f, w * 0.71f, h * 0.88f, w * 0.73f, h * 0.91f)
            cubicTo(w * 0.82f, h * 0.88f, w * 0.91f, h * 0.84f, w * 0.99f, h * 0.78f)
            cubicTo(w * 1.02f, h * 0.57f, w * 0.95f, h * 0.38f, w * 0.81f, h * 0.22f)
            close()
        }
        drawPath(path = path, color = Color.White, style = Fill)
    }
}

@Composable
private fun GitHubLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.22f, 0f, 0f, h * 0.22f, 0f, h * 0.5f)
            cubicTo(0f, h * 0.72f, w * 0.14f, h * 0.91f, w * 0.34f, h * 0.98f)
            cubicTo(w * 0.37f, h * 0.98f, w * 0.38f, h * 0.96f, w * 0.38f, h * 0.95f)
            lineTo(w * 0.38f, h * 0.88f)
            cubicTo(w * 0.24f, h * 0.91f, w * 0.21f, h * 0.81f, w * 0.21f, h * 0.81f)
            cubicTo(w * 0.19f, h * 0.75f, w * 0.16f, h * 0.73f, w * 0.16f, h * 0.73f)
            cubicTo(w * 0.11f, h * 0.7f, w * 0.16f, h * 0.7f, w * 0.16f, h * 0.7f)
            cubicTo(w * 0.21f, h * 0.7f, w * 0.24f, h * 0.75f, w * 0.24f, h * 0.75f)
            cubicTo(w * 0.29f, h * 0.83f, w * 0.37f, h * 0.81f, w * 0.40f, h * 0.8f)
            cubicTo(w * 0.40f, h * 0.76f, w * 0.42f, h * 0.73f, w * 0.44f, h * 0.71f)
            cubicTo(w * 0.33f, h * 0.7f, w * 0.21f, h * 0.65f, w * 0.21f, h * 0.46f)
            cubicTo(w * 0.21f, h * 0.41f, w * 0.23f, h * 0.36f, w * 0.26f, h * 0.32f)
            cubicTo(w * 0.25f, h * 0.3f, w * 0.24f, h * 0.25f, w * 0.26f, h * 0.19f)
            cubicTo(w * 0.26f, h * 0.19f, w * 0.30f, h * 0.18f, w * 0.40f, h * 0.25f)
            cubicTo(w * 0.44f, h * 0.24f, w * 0.48f, h * 0.23f, w * 0.53f, h * 0.23f)
            cubicTo(w * 0.58f, h * 0.23f, w * 0.62f, h * 0.24f, w * 0.66f, h * 0.25f)
            cubicTo(w * 0.76f, h * 0.18f, w * 0.80f, h * 0.19f, w * 0.80f, h * 0.19f)
            cubicTo(w * 0.82f, h * 0.25f, w * 0.81f, h * 0.30f, w * 0.80f, h * 0.32f)
            cubicTo(w * 0.83f, h * 0.36f, h * 0.85f, h * 0.41f, w * 0.85f, h * 0.46f)
            cubicTo(w * 0.85f, h * 0.65f, w * 0.73f, h * 0.70f, w * 0.62f, h * 0.71f)
            cubicTo(w * 0.64f, h * 0.73f, w * 0.66f, h * 0.77f, w * 0.66f, h * 0.83f)
            lineTo(w * 0.66f, h * 0.95f)
            cubicTo(w * 0.66f, h * 0.96f, w * 0.67f, h * 0.98f, w * 0.70f, h * 0.98f)
            cubicTo(w * 0.90f, h * 0.91f, w * 1.04f, h * 0.72f, w * 1.04f, h * 0.5f)
            cubicTo(w * 1.04f, h * 0.22f, w * 0.78f, 0f, w * 0.5f, 0f)
            close()
        }
        drawPath(path = path, color = Color.White, style = Fill)
    }
}
