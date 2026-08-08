package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AniListRepository
import com.example.data.AniListUserProfile
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont
import kotlinx.coroutines.launch

@Composable
fun AniListLogoBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF02A9FF),
    iconColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://anilist.co/img/icons/android-chrome-192x192.png",
            contentDescription = "AniList Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun ProfileScreen(
    onAccountClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onClearCacheClick: () -> Unit = {},
    onAppVersionClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE) }

    var isConnected by remember { mutableStateOf(prefs.getBoolean("anilist_connected", false)) }
    var username by remember { mutableStateOf(prefs.getString("anilist_username", "") ?: "") }
    var avatarUrl by remember { mutableStateOf(prefs.getString("anilist_avatar_url", "") ?: "") }
    var bannerUrl by remember { mutableStateOf(prefs.getString("anilist_banner_url", "") ?: "") }
    var animeCount by remember { mutableStateOf(prefs.getInt("anilist_anime_count", 0)) }
    var episodesWatched by remember { mutableStateOf(prefs.getInt("anilist_episodes_watched", 0)) }

    var isRefreshing by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }
    var inputUsername by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectError by remember { mutableStateOf<String?>(null) }

    var autoSyncEnabled by remember { mutableStateOf(prefs.getBoolean("auto_sync", true)) }

    // Sync or refresh user data from AniList API
    val refreshUserData: () -> Unit = {
        if (isConnected && username.isNotEmpty()) {
            isRefreshing = true
            coroutineScope.launch {
                try {
                    val profile = AniListRepository.getUserProfile(username)
                    if (profile != null) {
                        avatarUrl = profile.avatarUrl
                        bannerUrl = profile.bannerUrl
                        animeCount = profile.animeCount
                        episodesWatched = profile.episodesWatched

                        prefs.edit()
                            .putString("anilist_avatar_url", profile.avatarUrl)
                            .putString("anilist_banner_url", profile.bannerUrl)
                            .putInt("anilist_anime_count", profile.animeCount)
                            .putInt("anilist_episodes_watched", profile.episodesWatched)
                            .putInt("anilist_user_id", profile.id)
                            .putLong("anilist_minutes_watched", profile.minutesWatched)
                            .apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    LaunchedEffect(isConnected, username) {
        if (isConnected && username.isNotEmpty()) {
            refreshUserData()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090D))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header Banner & Profile Section
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Banner Background
                    if (isConnected && bannerUrl.isNotEmpty()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = "User Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1A1A28),
                                            Color(0xFF09090D)
                                        )
                                    )
                                )
                        )
                    }

                    // Scrim overlay for smooth transition
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF09090D)
                                    )
                                )
                            )
                    )

                    // Profile Details Card Container
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 70.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF12121A),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Profile Image (72dp with AniList glow)
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E1E2C))
                                            .border(
                                                width = 2.dp,
                                                color = if (isConnected) Color(0xFF02A9FF) else Color(0xFF333348),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isConnected && avatarUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = avatarUrl,
                                                contentDescription = "AniList Avatar",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = "Default Avatar",
                                                tint = Color(0xFF777788),
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // User info / AniList status
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isConnected && username.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = username,
                                                    color = Color.White,
                                                    fontSize = 19.sp,
                                                    fontFamily = PremiumTitleFont,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                Spacer(modifier = Modifier.width(6.dp))

                                                AniListLogoBadge(
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Connected",
                                                    tint = Color(0xFF02A9FF),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "AniList Synced",
                                                    color = Color(0xFF02A9FF),
                                                    fontSize = 12.sp,
                                                    fontFamily = PremiumBodyFont,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Guest User",
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.height(3.dp))

                                            Text(
                                                text = "Connect AniList client to sync watch history",
                                                color = Color(0xFF888899),
                                                fontSize = 12.sp,
                                                fontFamily = PremiumBodyFont
                                            )
                                        }
                                    }

                                    if (isConnected) {
                                        IconButton(
                                            onClick = refreshUserData,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            if (isRefreshing) {
                                                CircularProgressIndicator(
                                                    color = Color(0xFF02A9FF),
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = "Refresh",
                                                    tint = Color(0xFF888899),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isConnected) {
                                    // User Stats Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF181824))
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StatItem(
                                            label = "Total Anime",
                                            value = if (animeCount > 0) animeCount.toString() else "-"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(Color(0xFF2A2A3D))
                                        )
                                        StatItem(
                                            label = "Episodes",
                                            value = if (episodesWatched > 0) episodesWatched.toString() else "-"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(24.dp)
                                                .background(Color(0xFF2A2A3D))
                                        )
                                        StatItem(
                                            label = "Client Status",
                                            value = "Active"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Disconnect Button
                                    OutlinedButton(
                                        onClick = {
                                            showDisconnectConfirmDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF333348)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(0xFFFF5555)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                    ) {
                                        Text(
                                            text = "Disconnect AniList",
                                            fontSize = 12.sp,
                                            fontFamily = PremiumBodyFont,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    // Connect AniList Button (White & Black Theme)
                                    Button(
                                        onClick = {
                                            try {
                                                val clientId = com.example.BuildConfig.ANILIST_CLIENT_ID.ifEmpty { "47902" }
                                                val authUrl = "https://anilist.co/api/v2/oauth/authorize?client_id=$clientId&redirect_uri=jhshamim.zniwatch://anilist-auth&response_type=code"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            AniListLogoBadge(
                                                modifier = Modifier.size(22.dp),
                                                backgroundColor = Color(0xFF02A9FF),
                                                iconColor = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Connect with AniList",
                                                color = Color.Black,
                                                fontSize = 13.sp,
                                                fontFamily = PremiumTitleFont,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Account
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader("Account")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF12121A),
                    border = BorderStroke(1.dp, Color(0xFF222232)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column {
                        SettingActionRow(
                            icon = Icons.Filled.Person,
                            title = "Account Settings",
                            subtitle = if (isConnected) "Connected as $username" else "Connect your AniList account",
                            onClick = onAccountClick
                        )
                    }
                }
            }

            // Section: Features
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Features")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF12121A),
                    border = BorderStroke(1.dp, Color(0xFF222232)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column {
                        SettingActionRow(
                            icon = Icons.Filled.Download,
                            title = "Download",
                            subtitle = "Manage offline downloads",
                            onClick = onDownloadClick
                        )

                        SettingActionRow(
                            icon = Icons.Filled.People,
                            title = "Community",
                            subtitle = "Join discussions & forums",
                            onClick = onCommunityClick
                        )
                    }
                }
            }

            // Section: Playback Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Playback Settings")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF12121A),
                    border = BorderStroke(1.dp, Color(0xFF222232)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column {
                        SettingActionRow(
                            icon = Icons.Filled.Subtitles,
                            title = "Subtitle Settings",
                            subtitle = "Font size, background, language & styling",
                            onClick = onSubtitleClick
                        )
                    }
                }
            }

            // Section: App Storage
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader("Storage & About")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF12121A),
                    border = BorderStroke(1.dp, Color(0xFF222232)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column {
                        SettingActionRow(
                            icon = Icons.Filled.DeleteSweep,
                            title = "Clear App Cache",
                            subtitle = "Free up local memory",
                            onClick = onClearCacheClick
                        )

                        SettingActionRow(
                            icon = Icons.Filled.Info,
                            title = "App Version",
                            subtitle = "v2.4.0 (Build 2026)",
                            onClick = onAppVersionClick
                        )
                    }
                }
            }
        }
    }

    if (showDisconnectConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            containerColor = Color(0xFF14141E),
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Disconnect AniList?",
                    color = Color.White,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to disconnect your AniList account? You can reconnect anytime.",
                    color = Color(0xFFCCCCCC),
                    fontFamily = PremiumBodyFont,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isConnected = false
                        username = ""
                        avatarUrl = ""
                        bannerUrl = ""
                        animeCount = 0
                        episodesWatched = 0
                        prefs.edit().clear().apply()
                        showDisconnectConfirmDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Disconnect",
                        fontFamily = PremiumBodyFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDisconnectConfirmDialog = false }
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF888899),
                        fontFamily = PremiumBodyFont
                    )
                }
            }
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFF888899),
            fontSize = 11.sp,
            fontFamily = PremiumBodyFont
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF8888AA),
        fontSize = 13.sp,
        fontFamily = PremiumTitleFont,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Change
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E2C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF02A9FF),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF888899),
                fontSize = 11.sp,
                fontFamily = PremiumBodyFont
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF02A9FF),
                uncheckedThumbColor = Color(0xFF888899),
                uncheckedTrackColor = Color(0xFF1E1E2C)
            )
        )
    }
}

private typealias Change = Unit

@Composable
private fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E2C)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8888AA),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = PremiumTitleFont,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF888899),
                fontSize = 11.sp,
                fontFamily = PremiumBodyFont
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFF555566),
            modifier = Modifier.size(16.dp)
        )
    }
}
