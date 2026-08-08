package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PremiumBodyFont
import com.example.data.PremiumTitleFont

// Custom avatars
data class AvatarPreset(val name: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE) }

    var isConnected by remember { mutableStateOf(prefs.getBoolean("anilist_connected", false)) }
    var username by remember { mutableStateOf(prefs.getString("anilist_username", "") ?: "") }
    var avatarUrl by remember { mutableStateOf(prefs.getString("anilist_avatar_url", "") ?: "") }
    var animeCount by remember { mutableStateOf(prefs.getInt("anilist_anime_count", 0)) }
    var episodesWatched by remember { mutableStateOf(prefs.getInt("anilist_episodes_watched", 0)) }
    var userId by remember { mutableStateOf(prefs.getInt("anilist_user_id", 0)) }
    var minutesWatched by remember { mutableStateOf(prefs.getLong("anilist_minutes_watched", 0L)) }

    // Custom app overrides
    var customDisplayName by remember { mutableStateOf(prefs.getString("custom_display_name", "") ?: "") }
    var customAvatarUrlState by remember { mutableStateOf(prefs.getString("custom_avatar_url", "") ?: "") }
    var useCustomAvatar by remember { mutableStateOf(prefs.getBoolean("use_custom_avatar", false)) }

    // Bottom sheet dialog states
    var showDisplayNameSheet by remember { mutableStateOf(false) }
    var showAvatarSheet by remember { mutableStateOf(false) }

    // Presets
    val avatarPresets = remember {
        listOf(
            AvatarPreset("Rimuru", "https://s4.anilist.co/file/anilistcdn/character/large/b127222-S0E5V6R89B6r.png"),
            AvatarPreset("Gojo", "https://s4.anilist.co/file/anilistcdn/character/large/b127521-Gvj69K7X86o9.png"),
            AvatarPreset("Luffy", "https://s4.anilist.co/file/anilistcdn/character/large/b13701-Oq6Xf43NIDN7.jpg"),
            AvatarPreset("L", "https://s4.anilist.co/file/anilistcdn/character/large/b40-H4Z1Tq7X6S8W.png"),
            AvatarPreset("Mikasa", "https://s4.anilist.co/file/anilistcdn/character/large/b24475-DInFm3N8zYxK.png"),
            AvatarPreset("Naruto", "https://s4.anilist.co/file/anilistcdn/character/large/b130102-1LgY7A9G9XW8.png")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Account Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!isConnected) {
                // Not Connected Screen (ONLY shows a beautifully themed "Connect with AniList" button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x0D02A9FF),
                            modifier = Modifier.size(96.dp),
                            border = BorderStroke(1.dp, Color(0x3302A9FF))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = "https://anilist.co/img/icons/android-chrome-192x192.png",
                                    contentDescription = "AniList",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "AniList Synchronization",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Connect your AniList profile to synchronize your watch history, customize your display profile, and track your anime achievements.",
                            color = Color(0xFF888899),
                            fontSize = 13.sp,
                            fontFamily = PremiumBodyFont,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

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
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF02A9FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = "https://anilist.co/img/icons/android-chrome-192x192.png",
                                        contentDescription = "AniList Logo",
                                        modifier = Modifier.padding(2.dp)
                                    )
                                }
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
            } else {
                // Logged In Screen (Completely customized different design)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Modern Premium Hero Card for Account (different style than profile page)
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12121A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Dynamic profile picture with customized border
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E2C))
                                        .border(
                                            width = 3.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(Color(0xFF02A9FF), Color(0xFF8B5CF6))
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val activeAvatar = if (useCustomAvatar) customAvatarUrlState else avatarUrl
                                    if (activeAvatar.isNotEmpty()) {
                                        AsyncImage(
                                            model = activeAvatar,
                                            contentDescription = "Avatar",
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
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (customDisplayName.isNotEmpty()) customDisplayName else username,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF02A9FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = "https://anilist.co/img/icons/android-chrome-192x192.png",
                                            contentDescription = "AniList",
                                            modifier = Modifier.padding(1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "AniList ID: $userId",
                                    color = Color(0xFF666680),
                                    fontSize = 11.sp,
                                    fontFamily = PremiumBodyFont,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Section: Customize Profile
                    item {
                        Text(
                            text = "PROFILE CUSTOMIZATION",
                            color = Color(0xFF888899),
                            fontSize = 11.sp,
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF12121A),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                // Display Name Setting Button
                                ProfileCustomizationButton(
                                    icon = Icons.Default.Edit,
                                    title = "Display Name",
                                    subtitle = if (customDisplayName.isNotBlank()) customDisplayName else (username.ifEmpty { "Default User" }),
                                    onClick = { showDisplayNameSheet = true }
                                )

                                HorizontalDivider(color = Color(0xFF1C1C28), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                                // Avatar Settings Button
                                ProfileCustomizationButton(
                                    icon = Icons.Default.PhotoCamera,
                                    title = "Avatar Settings",
                                    subtitle = if (useCustomAvatar) "Custom Avatar Active" else "AniList Avatar Active",
                                    trailingAvatarUrl = if (useCustomAvatar && customAvatarUrlState.isNotBlank()) customAvatarUrlState else avatarUrl,
                                    onClick = { showAvatarSheet = true }
                                )
                            }
                        }
                    }

                    // Section: AniList Account Insights
                    item {
                        Text(
                            text = "ANILIST ACCOUNT INSIGHTS",
                            color = Color(0xFF888899),
                            fontSize = 11.sp,
                            fontFamily = PremiumTitleFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF12121A),
                            border = BorderStroke(1.dp, Color(0xFF222232)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFF02A9FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Official AniList Profile Data",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF222232), thickness = 1.dp)

                                Spacer(modifier = Modifier.height(14.dp))

                                // Dynamic stats items from AniList API!
                                AccountStatRow(label = "User Status", value = "Synchronized")
                                AccountStatRow(label = "Username", value = username)
                                AccountStatRow(label = "Total Anime", value = "$animeCount shows")
                                AccountStatRow(label = "Episodes Watched", value = "$episodesWatched episodes")

                                val days = minutesWatched / (24 * 60)
                                val hours = (minutesWatched % (24 * 60)) / 60
                                val timeWatchedStr = if (days > 0) {
                                    "$days days, $hours hours"
                                } else {
                                    "$hours hours"
                                }
                                AccountStatRow(label = "Total Time Watched", value = timeWatchedStr)
                            }
                        }
                    }

                    // Section: Disconnect Option
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1A1212),
                            border = BorderStroke(1.dp, Color(0xFF322222)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        prefs.edit()
                                            .putBoolean("anilist_connected", false)
                                            .putString("anilist_access_token", null)
                                            .putString("anilist_username", "")
                                            .putString("anilist_avatar_url", "")
                                            .putString("anilist_banner_url", "")
                                            .putInt("anilist_anime_count", 0)
                                            .putInt("anilist_episodes_watched", 0)
                                            .putInt("anilist_user_id", 0)
                                            .putLong("anilist_minutes_watched", 0L)
                                            // Reset custom fields too so they disconnect cleanly
                                            .putString("custom_display_name", "")
                                            .putString("custom_avatar_url", "")
                                            .putBoolean("use_custom_avatar", false)
                                            .apply()

                                        isConnected = false
                                        username = ""
                                        avatarUrl = ""
                                        animeCount = 0
                                        episodesWatched = 0
                                        userId = 0
                                        minutesWatched = 0L
                                        customDisplayName = ""
                                        customAvatarUrlState = ""
                                        useCustomAvatar = false

                                        Toast.makeText(context, "Disconnected AniList client", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Disconnect Account",
                                        color = Color(0xFFFF5555),
                                        fontSize = 14.sp,
                                        fontFamily = PremiumTitleFont,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Unlink this application from AniList cloud",
                                        color = Color(0xFF887777),
                                        fontSize = 11.sp,
                                        fontFamily = PremiumBodyFont
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Disconnect",
                                    tint = Color(0xFFFF5555),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Display Name Bottom Sheet
        if (showDisplayNameSheet) {
            var tempDisplayName by remember { mutableStateOf(customDisplayName) }
            ModalBottomSheet(
                onDismissRequest = { showDisplayNameSheet = false },
                containerColor = Color(0xFF12121A),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Change Display Name",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set a custom display name across the application.",
                        color = Color(0xFF888899),
                        fontSize = 12.sp,
                        fontFamily = PremiumBodyFont
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = tempDisplayName,
                        onValueChange = { tempDisplayName = it },
                        placeholder = {
                            Text(
                                text = username.ifEmpty { "Enter custom display name" },
                                color = Color(0xFF555566),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = Color(0xFF02A9FF),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF02A9FF),
                            unfocusedBorderColor = Color(0xFF222232),
                            focusedContainerColor = Color(0xFF0C0C12),
                            unfocusedContainerColor = Color(0xFF0C0C12),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (tempDisplayName.isNotBlank()) {
                            Button(
                                onClick = {
                                    tempDisplayName = ""
                                    customDisplayName = ""
                                    prefs.edit().remove("custom_display_name").apply()
                                    Toast.makeText(context, "Reset to AniList username", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222232)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset", color = Color.White)
                            }
                        }

                        Button(
                            onClick = {
                                customDisplayName = tempDisplayName
                                prefs.edit().putString("custom_display_name", tempDisplayName).apply()
                                Toast.makeText(context, "Display name updated!", Toast.LENGTH_SHORT).show()
                                showDisplayNameSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02A9FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Name", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Avatar Settings Bottom Sheet
        if (showAvatarSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarSheet = false },
                containerColor = Color(0xFF12121A),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Avatar Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontFamily = PremiumTitleFont,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose a custom app avatar or sync with your AniList profile avatar.",
                        color = Color(0xFF888899),
                        fontSize = 12.sp,
                        fontFamily = PremiumBodyFont
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Switch row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Custom App Avatar",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Override default AniList profile picture",
                                color = Color(0xFF777788),
                                fontSize = 11.sp,
                                fontFamily = PremiumBodyFont
                            )
                        }

                        Switch(
                            checked = useCustomAvatar,
                            onCheckedChange = { checked ->
                                useCustomAvatar = checked
                                prefs.edit().putBoolean("use_custom_avatar", checked).apply()
                                Toast.makeText(context, if (checked) "Custom Avatar Enabled" else "AniList Avatar Restored", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF02A9FF),
                                uncheckedThumbColor = Color(0xFF777788),
                                uncheckedTrackColor = Color(0xFF222232)
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = useCustomAvatar,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = "Select Preset Character",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(avatarPresets) { preset ->
                                    val isSelected = customAvatarUrlState == preset.url
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E1E2C))
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) Color(0xFF02A9FF) else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                customAvatarUrlState = preset.url
                                                prefs.edit().putString("custom_avatar_url", preset.url).apply()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = preset.url,
                                            contentDescription = preset.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0x66000000)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Or Paste Custom Image URL",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = PremiumTitleFont,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = customAvatarUrlState,
                                onValueChange = { newValue ->
                                    customAvatarUrlState = newValue
                                    prefs.edit().putString("custom_avatar_url", newValue).apply()
                                },
                                placeholder = {
                                    Text(
                                        text = "https://example.com/avatar.png",
                                        color = Color(0xFF555566),
                                        fontSize = 13.sp,
                                        fontFamily = PremiumBodyFont
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "URL",
                                        tint = Color(0xFF888899),
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF02A9FF),
                                    unfocusedBorderColor = Color(0xFF222232),
                                    focusedContainerColor = Color(0xFF0C0C12),
                                    unfocusedContainerColor = Color(0xFF0C0C12),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showAvatarSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF02A9FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileCustomizationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailingAvatarUrl: String? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1C1C28), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF02A9FF),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = PremiumTitleFont,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF888899),
                    fontSize = 12.sp,
                    fontFamily = PremiumBodyFont
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!trailingAvatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = trailingAvatarUrl,
                    contentDescription = "Avatar Preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF02A9FF), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Open",
                tint = Color(0xFF666677),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AccountStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF888899),
            fontSize = 13.sp,
            fontFamily = PremiumBodyFont
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontFamily = PremiumTitleFont,
            fontWeight = FontWeight.SemiBold
        )
    }
}
